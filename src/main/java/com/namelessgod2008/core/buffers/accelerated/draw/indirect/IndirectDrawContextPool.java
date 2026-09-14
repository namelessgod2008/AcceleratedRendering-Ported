package com.namelessgod2008.core.buffers.accelerated.draw.indirect;

import com.namelessgod2008.core.backends.buffers.IServerBuffer;
import com.namelessgod2008.core.backends.buffers.MappedBuffer;
import com.namelessgod2008.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import com.namelessgod2008.core.buffers.accelerated.draw.pools.IDrawContextPool;
import com.namelessgod2008.core.buffers.accelerated.draw.pools.IElementPool.IElementSegment;
import com.namelessgod2008.core.buffers.accelerated.draw.indirect.IndirectElementBufferPool.ElementSegment;
import com.namelessgod2008.core.buffers.memory.IMemoryInterface;
import com.namelessgod2008.core.buffers.memory.SimpleMemoryInterface;
import com.namelessgod2008.core.utils.RenderTypeUtils;
import com.namelessgod2008.core.utils.SimpleResetPool;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.renderer.rendertype.RenderType;

import static org.lwjgl.opengl.GL46.*;

public class IndirectDrawContextPool extends SimpleResetPool<IndirectDrawContextPool.DrawContext, MappedBuffer> implements IDrawContextPool {

	public IndirectDrawContextPool(int size) {
		super(size, new MappedBuffer(20L * size));
	}

	@Override
	public void setup() {
		context.bind(GL_DRAW_INDIRECT_BUFFER);
	}

	@Override
	protected DrawContext create(MappedBuffer buffer, int i) {
		return new DrawContext(i);
	}

	@Override
	protected void reset(DrawContext drawContext) {

	}

	@Override
	protected void delete(DrawContext drawContext) {

	}

	@Override
	public void delete() {
		getContext().delete();
	}

	@Override
	public DrawContext fail() {
		expand();
		return get();
	}

	public class DrawContext implements IDrawContext {

		public static	final	int					ELEMENT_COUNT_INDEX		= 0;
		public static	final	int					ELEMENT_BUFFER_INDEX	= 6;
		public static	final	IMemoryInterface	INDIRECT_INDEX_COUNT	= new SimpleMemoryInterface(0L * 4L, 4);
		public static	final	IMemoryInterface	INDIRECT_INSTANCE_COUNT	= new SimpleMemoryInterface(1L * 4L, 4);
		public static	final	IMemoryInterface	INDIRECT_FIRST_INDEX	= new SimpleMemoryInterface(2L * 4L, 4);
		public static	final	IMemoryInterface	INDIRECT_BASE_INDEX		= new SimpleMemoryInterface(3L * 4L, 4);
		public static	final	IMemoryInterface	INDIRECT_BASE_INSTANCE	= new SimpleMemoryInterface(4L * 4L, 4);

		private			final	long				commandOffset;
		private					RenderType			renderType;

		public DrawContext(int index) {
			this.commandOffset	= index * 20L;
			this.renderType		= null;

			var address = context.reserve(20L);

			INDIRECT_INDEX_COUNT	.putInt(address, 0);
			INDIRECT_INSTANCE_COUNT	.putInt(address, 1);
			INDIRECT_FIRST_INDEX	.putInt(address, 0);
			INDIRECT_BASE_INDEX		.putInt(address, 0);
			INDIRECT_BASE_INSTANCE	.putInt(address, 0);
		}

		@Override
		public void setupContext(
				AcceleratedBufferBuilder	builder,
				IElementSegment				elementSegment,
				IServerBuffer				elementBuffer,
				RenderType					renderType
		) {
			if (!(elementSegment instanceof ElementSegment indirect)) {
				throw new IllegalStateException("Incorrect draw method.");
			}

			this.renderType = renderType;

			var commandAddress	= context	.addressAt	(commandOffset);
			var elementOffset	= indirect	.getOffset	();
			var elementSize		= indirect	.getSize	();

			INDIRECT_INDEX_COUNT.putInt(commandAddress, 0);
			INDIRECT_FIRST_INDEX.putInt(commandAddress, elementOffset / 4);

			elementBuffer	.bindRange(GL_SHADER_STORAGE_BUFFER, ELEMENT_BUFFER_INDEX,	elementOffset,	elementSize);
			context			.bindRange(GL_ATOMIC_COUNTER_BUFFER, ELEMENT_COUNT_INDEX,	commandOffset,	4);

			// 记录本次绘制所需的缓冲，供 drawElements(pass, mode) 设置到 RenderPass 上
			this.elementBufferForDraw	= elementBuffer;
			this.elementBufferSize		= elementBuffer instanceof com.namelessgod2008.core.utils.MutableSize sized
					? sized.getSize()
					: Integer.MAX_VALUE;

			var vertexStorage = builder.getBuffer().getVertexBufferStorage();

			this.vertexBufferHandle	= vertexStorage.getBufferHandle();
			this.vertexBufferSize	= vertexStorage instanceof com.namelessgod2008.core.utils.MutableSize sized
					? sized.getSize()
					: Integer.MAX_VALUE;
		}

		@Override
		public void drawElements(Mode mode) {
			glDrawElementsIndirect(
					RenderTypeUtils.toGLMode(mode),
					GL_UNSIGNED_INT,
					commandOffset
			);
		}

		/**
		 * 26.1: 绘制前准备——必须在任何 RenderPass 打开之前调用。
		 *
		 * <p>与 BASEVERTEX 路径一致：解析纹理（可能触发懒加载上传）与写入 DynamicTransforms
		 * （mapBuffer）都会编码命令，在打开的 pass 内执行会抛
		 * "Close the existing render pass before performing additional commands"。
		 *
		 * <p><b>DynamicTransforms 不可省略</b>：它是顶点着色器用的 MVP 矩阵（含 modelView 与
		 * layering 变换）。缺了它，顶点会被变换到无效位置，实体将完全不可见。
		 */
		@Override
		public void prepareDraw() {
			if (vertexBufferHandle <= 0 || elementBufferForDraw == null) {
				return;
			}

			this.preparedTextures = RenderTypeUtils.getTextures(renderType);

			// 与原版 RenderType.draw 一致：先施加 RenderType 的 layeringTransform 再写 uniform
			// （VIEW_OFFSET_Z_LAYERING 等，防止与共面几何 z-fighting）
			this.preparedTransforms = com.mojang.blaze3d.systems.RenderSystem
					.getDynamicUniforms()
					.writeTransform(
							RenderTypeUtils.applyLayeringTransform(
									renderType,
									com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix()
							),
							new org.joml.Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
							new org.joml.Vector3f(),
							RenderTypeUtils.getTextureMatrix(renderType)
					);
		}

		/**
		 * 26.1: 在外层共享的 RenderPass 内执行 indirect 绘制。
		 *
		 * <p>绘制前的状态绑定与 BASEVERTEX 路径逐项对齐（pipeline / scissor /
		 * DynamicTransforms / 顶点缓冲 / 纹理），差别仅在最后一步：BASEVERTEX 用
		 * {@code pass.drawIndexed}，此处用 {@code glDrawElementsIndirect}
		 * （索引与 draw count 由 compute shader 写入命令缓冲）。
		 *
		 * <p>indirect 命令缓冲是 mod 自有的裸 GL 缓冲，须在此绑定到
		 * {@code GL_DRAW_INDIRECT_BUFFER}；多个 pool 共用同一 target，故每次绘制前重新绑定。
		 */
		@Override
		public void drawElements(com.mojang.blaze3d.systems.RenderPass pass, Mode mode) {
			// RenderPass.backend 经 access widener 开放，实际类型即 GlRenderPass
			if (!(pass.backend instanceof com.mojang.blaze3d.opengl.GlRenderPass glPass)) {
				return;
			}

			var elementBuffer = elementBufferForDraw;

			if (elementBuffer == null || vertexBufferHandle <= 0) {
				return;
			}

			// 复用 RenderType 自身的 RenderPipeline（含正确的顶点/片段着色器与 uniform 约定）
			pass.setPipeline(renderType.pipeline());

			// 多个 draw 共用同一个 RenderPass，scissor 会延续到下一个 draw，故先重置再按需启用
			pass.disableScissor();

			var scissorState = com.mojang.blaze3d.systems.RenderSystem.getScissorStateForRenderTypeDraws();

			if (scissorState.enabled()) {
				pass.enableScissor(
						scissorState.x		(),
						scissorState.y		(),
						scissorState.width	(),
						scissorState.height	()
				);
			}

			// MVP 矩阵：缺了它实体不可见（见 prepareDraw 注释）
			if (preparedTransforms != null) {
				pass.setUniform("DynamicTransforms", preparedTransforms);
			}

			pass.setVertexBuffer(0, com.namelessgod2008.core.backends.WrappedGlBuffer.of(
					vertexBufferHandle,
					com.mojang.blaze3d.buffers.GpuBuffer.USAGE_VERTEX,
					vertexBufferSize
			));

			// bindTexture 在 sampler 为 null 时会移除该绑定，编码器随后静默跳过；
			// 若一个采样器都没绑上，编码器校验会抛 "Missing sampler"。故对 sampler 兜底。
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

			pass.setIndexBuffer(
					com.namelessgod2008.core.backends.WrappedGlBuffer.of(
							elementBuffer.getBufferHandle(),
							com.mojang.blaze3d.buffers.GpuBuffer.USAGE_INDEX,
							elementBufferSize
					),
					com.mojang.blaze3d.vertex.VertexFormat.IndexType.INT
			);

			// indirect 命令缓冲需绑定到 GL_DRAW_INDIRECT_BUFFER，再由 encoder 完成绘制
			context.bind(GL_DRAW_INDIRECT_BUFFER);

			// 执行器由 GlCommandEncoderMixin 在构造时登记到静态桥接表（见 IndirectDrawBridge 注释）
			var executor = com.namelessgod2008.core.backends.IndirectDrawBridge.get();

			if (executor != null) {
				executor.drawElementsIndirect(glPass, commandOffset);
			}
		}

		/** 准备阶段结果（须在 RenderPass 打开前算好），与 BASEVERTEX 路径同构。 */
		private com.mojang.blaze3d.buffers.GpuBufferSlice	preparedTransforms;
		private java.util.Map<String, net.minecraft.client.renderer.rendertype.RenderSetup.TextureAndSampler>
				preparedTextures = java.util.Collections.emptyMap();

		/** sampler 为 null 时的兜底采样器，仅首次需要时查询缓存（与 BASEVERTEX 路径一致）。 */
		private static com.mojang.blaze3d.textures.GpuSampler FALLBACK_SAMPLER;

		private static com.mojang.blaze3d.textures.GpuSampler fallbackSampler() {
			if (FALLBACK_SAMPLER == null) {
				FALLBACK_SAMPLER = com.mojang.blaze3d.systems.RenderSystem
						.getSamplerCache()
						.getClampToEdge(com.mojang.blaze3d.textures.FilterMode.NEAREST);
			}

			return FALLBACK_SAMPLER;
		}

		/** 本次绘制所用的顶点缓冲句柄与大小（由 {@code setupContext} 记录）。 */
		private int		vertexBufferHandle	= -1;
		private long	vertexBufferSize	= Integer.MAX_VALUE;
		/** 本次绘制所用的索引缓冲（由 {@code setupContext} 记录）。 */
		private IServerBuffer	elementBufferForDraw;
		private long			elementBufferSize	= Integer.MAX_VALUE;

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
