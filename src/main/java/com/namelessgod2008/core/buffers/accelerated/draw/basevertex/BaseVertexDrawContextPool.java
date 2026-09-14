package com.namelessgod2008.core.buffers.accelerated.draw.basevertex;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.namelessgod2008.core.AccelStats;
import com.namelessgod2008.core.backends.WrappedGlBuffer;
import com.namelessgod2008.core.backends.buffers.IServerBuffer;
import com.namelessgod2008.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import com.namelessgod2008.core.buffers.accelerated.draw.pools.IDrawContextPool;
import com.namelessgod2008.core.buffers.accelerated.draw.pools.IElementPool.IElementSegment;
import com.namelessgod2008.core.utils.RenderTypeUtils;
import com.namelessgod2008.core.utils.SimpleResetPool;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * 26.1: BASEVERTEX 绘制改走 26.1 的 RenderPass 体系。
 *
 * 26.1 移除了 ShaderInstance（裸 GL 绘制无 shader 可用），故复用 RenderType 自身的
 * RenderPipeline（含正确的顶点/片段着色器与 uniform 约定），用 mod 自己的顶点/索引缓冲绘制，
 * 等价于 1.21.4 的 "renderType.setupRenderState() + shader.setDefaultUniforms + glDrawElementsBaseVertex"。
 *
 * mod 的缓冲仍以裸 GL 实现（与 1.21.4 一致），绘制时经 WrappedGlBuffer（继承 GlBuffer、覆盖 close）
 * 包装为 26.1 GpuBuffer。
 *
 * ⚠️ 26.1 的 CommandEncoder 规定：存在打开的 render pass 时不得执行其它命令。而解析纹理
 * （可能触发懒加载上传 writeToTexture）与写入 DynamicTransforms（mapBuffer）都属于此类命令，
 * 必须在【开 pass 之前】完成（原版 RenderType.draw 亦如此）。故拆为 prepareDraw() / drawElements()。
 */
public class BaseVertexDrawContextPool extends SimpleResetPool<BaseVertexDrawContextPool.DrawContext, Void> implements IDrawContextPool {

	public BaseVertexDrawContextPool(int size) {
		super(size, null);
	}

	@Override
	public void setup() {

	}

	@Override
	protected DrawContext create(Void buffer, int i) {
		return new DrawContext();
	}

	@Override
	protected void reset(DrawContext drawContext) {

	}

	@Override
	protected void delete(DrawContext drawContext) {

	}

	@Override
	public void delete() {

	}

	@Override
	public DrawContext fail() {
		expand();
		return get();
	}

	public static class DrawContext implements IDrawContext {

		private RenderType	renderType;
		private int			vertexHandle;
		private int			indexHandle;
		private long		vertexSize;
		private long		indexSize;
		private int			baseVertex;
		private int			count;

		// 准备阶段结果（须在 RenderPass 打开前算好）
		private GpuBufferSlice												preparedTransforms;
		private java.util.Map<String, net.minecraft.client.renderer.rendertype.RenderSetup.TextureAndSampler>	preparedTextures	= java.util.Collections.emptyMap();

		public DrawContext() {
			this.baseVertex	= -1;
			this.count		= -1;
		}

		// 26.1: 经 WrappedGlBuffer.of 按 handle 全局复用包装对象（VAO 缓存依赖对象同一性）
		private GpuBuffer vertexGpuBuffer() {
			return WrappedGlBuffer.of(vertexHandle, GpuBuffer.USAGE_VERTEX,	vertexSize);
		}

		private GpuBuffer indexGpuBuffer() {
			return WrappedGlBuffer.of(indexHandle, GpuBuffer.USAGE_INDEX,	indexSize);
		}

		@Override
		public void setupContext(
				AcceleratedBufferBuilder	builder,
				IElementSegment				elementSegment,
				IServerBuffer				elementBuffer,
				RenderType					renderType
		) {
			this.renderType		= renderType;
			this.vertexHandle	= builder.getBuffer().getVertexBufferStorage().getBufferHandle	();
			this.vertexSize		= builder.getBuffer().getVertexBufferStorage()
					instanceof com.namelessgod2008.core.utils.MutableSize sized
					? sized.getSize()
					: Integer.MAX_VALUE;
			this.indexHandle	= elementBuffer	.getBufferHandle										();
			this.indexSize		= elementBuffer	instanceof com.namelessgod2008.core.utils.MutableSize sized
					? sized.getSize()
					: Integer.MAX_VALUE;
			this.baseVertex		= (int) builder			.getVertexCountOffset	();
			this.count			= (int) elementSegment	.getCount				();
		}

		/**
		 * 26.1: 绘制前准备——必须在任何 RenderPass 打开之前调用。
		 * 解析纹理（可能触发懒加载上传）与写入 DynamicTransforms（mapBuffer）都会编码命令。
		 */
		@Override
		public void prepareDraw() {
			if (vertexHandle <= 0 || indexHandle <= 0 || count <= 0) {
				return;
			}

			long statStart = System.nanoTime();

			this.preparedTextures = RenderTypeUtils.getTextures(renderType);

			// 与原版 RenderType.draw 一致：DynamicTransforms 在 pass 外写入。
			// 原版在写之前会把 RenderType 的 layeringTransform 施加到 modelView 栈上
			// （VIEW_OFFSET_Z_LAYERING 等，用于避免与共面几何 z-fighting），此处必须补上，
			// 否则 entity_shadow 等 RenderType 会闪烁。
			this.preparedTransforms = RenderSystem.getDynamicUniforms().writeTransform(
					RenderTypeUtils.applyLayeringTransform(renderType, RenderSystem.getModelViewMatrix()),
					new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
					new Vector3f(),
					RenderTypeUtils.getTextureMatrix(renderType)
			);

			AccelStats.PREPARE_NANOS += System.nanoTime() - statStart;
		}

		@Override
		public void drawElements(Mode mode) {
			// 26.1: 独立绘制（无外层共享 pass 时的回退路径）
			if (vertexHandle <= 0 || indexHandle <= 0 || count <= 0) {
				return;
			}

			var renderTarget = getRenderTarget();

			if (renderTarget == null) {
				return;
			}

			var colorTexture = RenderSystem.outputColorTextureOverride != null
					? RenderSystem.outputColorTextureOverride
					: renderTarget.getColorTextureView();
			var depthTexture = renderTarget.useDepth
					? (RenderSystem.outputDepthTextureOverride != null
						? RenderSystem.outputDepthTextureOverride
						: renderTarget.getDepthTextureView())
					: null;

			prepareDraw();

			try (var pass = RenderSystem.getDevice()
					.createCommandEncoder()
					.createRenderPass(
							() -> "acceleratedrendering_basevertex",
							colorTexture,
							OptionalInt.empty(),
							depthTexture,
							OptionalDouble.empty()
					)
			) {
				RenderSystem.bindDefaultUniforms(pass);
				drawElements(pass, mode);
			}
		}

		@Override
		public void drawElements(com.mojang.blaze3d.systems.RenderPass pass, Mode mode) {
			if (vertexHandle <= 0 || indexHandle <= 0 || count <= 0) {
				return;
			}

			long statStart = System.nanoTime();

			// 26.1: mod 缓冲以裸 GL 实现，绘制时包装为 26.1 GpuBuffer（结果按 handle 复用）
			var vertexBuffer = vertexGpuBuffer();
			var indexBuffer  = indexGpuBuffer();

			if (vertexBuffer == null || indexBuffer == null) {
				return;
			}

			// 复用 RenderType 自身的 RenderPipeline（含正确的顶点/片段着色器与 uniform 约定）
			pass.setPipeline(renderType.pipeline());

			// 26.1: 与原版 RenderType.draw 对齐——应用 scissor 状态。
			// 多个 draw 共用同一个 RenderPass，scissor 会延续到下一个 draw，故先重置再按需启用。
			pass.disableScissor();

			var scissorState = RenderSystem.getScissorStateForRenderTypeDraws();

			if (scissorState.enabled()) {
				pass.enableScissor(
						scissorState.x		(),
						scissorState.y		(),
						scissorState.width	(),
						scissorState.height	()
				);
			}

			if (preparedTransforms != null) {
				pass.setUniform("DynamicTransforms", preparedTransforms);
			}

			pass.setVertexBuffer(0, vertexBuffer);

			// 26.1: bindTexture 在 sampler 为 null 时会从 samplers 移除该名称，编码器随后静默跳过
			// 绑定（纹理单元残留上一次绘制的图集）；且若一个采样器都没绑上，编码器校验会抛
			// "Missing sampler" 异常。故对 sampler 兜底；无纹理时不绘制（避免每 draw 抛异常）。
			if (preparedTextures.isEmpty()) {
				return;
			}

			for (var entry : preparedTextures.entrySet()) {
				var textureView	= entry.getValue().textureView();
				var sampler		= entry.getValue().sampler();

				if (textureView == null) {
					continue;
				}

				if (sampler == null) {
					sampler = fallbackSampler();
				}

				pass.bindTexture(
						entry.getKey(),
						textureView,
						sampler
				);
			}

			pass.setIndexBuffer(indexBuffer, VertexFormat.IndexType.INT);
			pass.drawIndexed(
					baseVertex,
					0,
					count,
					1
			);

			AccelStats.DRAWS		++;
			AccelStats.VERTICES	+= count;
			AccelStats.DRAW_NANOS+= System.nanoTime() - statStart;
		}

		/** 26.1: sampler 为 null 时的兜底采样器，仅首次需要时查询缓存。 */
		private static com.mojang.blaze3d.textures.GpuSampler FALLBACK_SAMPLER;

		private static com.mojang.blaze3d.textures.GpuSampler fallbackSampler() {
			if (FALLBACK_SAMPLER == null) {
				FALLBACK_SAMPLER = RenderSystem.getSamplerCache().getClampToEdge(com.mojang.blaze3d.textures.FilterMode.NEAREST);
			}

			return FALLBACK_SAMPLER;
		}

		@Override
		public int compareTo(IDrawContext that) {
			return Boolean.compare(
					this.getRenderType().sortOnUpload(),
					that.getRenderType().sortOnUpload()
			);
		}

		@Override
		public RenderType getRenderType() {
			return renderType;
		}
	}
}
