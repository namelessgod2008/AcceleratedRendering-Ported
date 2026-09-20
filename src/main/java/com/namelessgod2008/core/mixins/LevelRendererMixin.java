package com.namelessgod2008.core.mixins;

import com.namelessgod2008.core.AccelStats;
import com.namelessgod2008.core.CoreBuffers;
import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.core.CoreStates;
import com.namelessgod2008.core.buffers.accelerated.layers.LayerDrawType;
import com.namelessgod2008.core.buffers.accelerated.layers.storage.LayerStorageType;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.1 LevelRendererMixin — 适配 FrameGraph 渲染体系。
 *
 * 1.21.4 的绘制时机基于帧图 lambda method_62214 内的 endLastBatch/endOutlineBatch；
 * 26.1 中渲染全部在 LevelRenderer.renderLevel() 构建的 FrameGraph 的 pass.executes() 内完成，
 * endBatch()/endOutlineBatch() 位于各 pass lambda 深处（无法直接注入）。
 *
 * 因此绘制核心缓冲区的时机改为注入 renderLevel() 中 frame.execute(...)（INVOKE）调用之后：
 * - 此时所有 frame pass 已同步执行完，实体/方块实体顶点已提交到加速缓冲
 * - modelViewStack 仍持有世界矩阵（popMatrix 在 execute 之后）— RenderSystem 矩阵可用
 * - outline 缓冲（endOutlineBatch）也已处理，与 drawCoreBuffers 合并到此处绘制
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

	@Inject(method = "renderLevel", at = @At("HEAD"))
	public void startRenderLevel(
		GraphicsResourceAllocator allocator,
		DeltaTracker deltaTracker,
		boolean renderOutline,
		CameraRenderState cameraState,
		Matrix4fc modelViewMatrix,
		GpuBufferSlice terrainFog,
		Vector4f fogColor,
		boolean shouldRenderSky,
		ChunkSectionsToRender chunkSectionsToRender,
		CallbackInfo ci
	) {
		// 26.1: 捕获世界渲染的投影矩阵，供计算着色器 uniform 上传（RenderSystem.getProjectionMatrix 已移除）
		CoreFeature.setProjectionMatrix(cameraState.projectionMatrix);
		// Defense against stuck GUI batching state after screen/world transitions
		// (e.g. CSL may trigger GL state changes during resource reload)
		if (CoreFeature.isGuiBatching()) {
			CoreFeature.resetGuiBatching();
		}
		CoreFeature.setRenderingLevel();
		// [临时探针] 记录 renderLevel 起点，用于统计整帧世界渲染耗时
		AccelStats.LEVEL_START = System.nanoTime();
		if (AccelStats.FIRST_FRAME == 0L) {
			AccelStats.FIRST_FRAME = AccelStats.LEVEL_START;
		}
	}

	@Inject(method = "renderLevel", at = @At("RETURN"))
	public void stopRenderLevel(
		GraphicsResourceAllocator allocator,
		DeltaTracker deltaTracker,
		boolean renderOutline,
		CameraRenderState cameraState,
		Matrix4fc modelViewMatrix,
		GpuBufferSlice terrainFog,
		Vector4f fogColor,
		boolean shouldRenderSky,
		ChunkSectionsToRender chunkSectionsToRender,
		CallbackInfo ci
	) {
		CoreFeature.resetRenderingLevel();
		// [临时探针] 累加整帧世界渲染耗时（含实体提交/地形/GUI 之外的全部世界渲染）
		AccelStats.LEVEL_NANOS += System.nanoTime() - AccelStats.LEVEL_START;
	}

	/**
	 * 26.1 + Iris: 加速绘制按 OPAQUE / TRANSLUCENT **拆成两个时机**。
	 *
	 * <p><b>为什么必须拆</b>（2026-09-20 定位，Iris 1.11.4 字节码实证）：
	 * Iris 把 {@code beginTranslucents()} 挂钩在 {@code lambda$addMainPass$0} 内
	 * <b>对 {@code FeatureRenderDispatcher.renderTranslucentFeatures()} 的 INVOKE 上</b>
	 * （{@code MixinLevelRenderer.iris$beginTranslucents}，无 shift ⇒ 在该调用**之前**执行）。
	 * 它会把 {@code isBeforeTranslucent} 置为 {@code false}，而该标志决定
	 * {@code bindDefault()} / {@code ExtendedShader.iris$setupState()} 绑哪个 FBO：
	 * <pre>
	 *   true  → 写 gbuffer 主纹理（deferred 会消费）
	 *   false → 写 alt 纹理（deferred 已跑完，不再被采样）
	 * </pre>
	 *
	 * <p>{@code lambda$addMainPass$0} 的字节码顺序：
	 * <pre>
	 *   273: renderSolidFeatures()        ← isBeforeTranslucent == true  ★ OPAQUE 锚点取这之后
	 *   278: endBatch()
	 *   390: renderTranslucentFeatures()  ← Iris 在此之前把标志置 false   ★ TRANSLUCENT 锚点取这之后
	 *   416: endOutlineBatch()
	 * </pre>
	 *
	 * <p>若不拆，两批都会落在 {@code false} 之后：Photon 的 {@code solid.glsl}
	 * 是 {@code DRAWBUFFERS:1}（**只写 gbuffer albedo**，deferred 之后不再采样）
	 * → 不透明实体不可见；而 {@code translucent.glsl} 是 {@code DRAWBUFFERS:01}
	 * （同时写 scene color，final composite 读得到）→ 半透明可见。
	 * 这精确对应实测症状「史莱姆有身体无眼睛、羊/箱子消失」。
	 *
	 * <p>拆分要求 {@code LayerStorageType.SEPARATED}（{@code SeparatedLayerStorage.get(type)}
	 * 才能按 drawType 取到独立桶）；配置正是 SEPARATED。
	 */
	@Inject(
			method	= "lambda$addMainPass$0",
			at		= @At(
					value	= "INVOKE",
					target	= "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderSolidFeatures()V",
					shift	= At.Shift.AFTER
			)
	)
	private void drawAcceleratedOpaque(CallbackInfo ci) {
		if (!CoreFeature.isLoaded()) {
			return;
		}

		// 此处 isBeforeTranslucent 仍为 true → 几何写进 deferred 会读的 gbuffer 主纹理
		drawAccelerated(LayerDrawType.OPAQUE);
	}

	@Inject(
			method	= "lambda$addMainPass$0",
			at		= @At(
					value	= "INVOKE",
					target	= "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V",
					shift	= At.Shift.AFTER
			)
	)
	private void drawAcceleratedTranslucent(CallbackInfo ci) {
		if (!CoreFeature.isLoaded()) {
			return;
		}

		// 此处 isBeforeTranslucent 已为 false → 与半透明语义一致。
		// 非 SEPARATED 时上半透明时机已用 ALL 画完，这里不能再画一遍。
		if (CoreFeature.getLayerStorageType() == LayerStorageType.SEPARATED) {
			drawAccelerated(LayerDrawType.TRANSLUCENT);
		}

		// outline 缓冲有独立的 OUTLINE_TARGET，与主几何相位无关，放在最后即可
		CoreStates                        .recordBuffers ();
		CoreBuffers.POS_TEX_COLOR_OUTLINE .prepareBuffers();
		CoreStates                        .restoreBuffers();
		CoreBuffers.POS_TEX_COLOR_OUTLINE .drawBuffers(LayerDrawType.ALL);

		reportFrame();
	}

	/**
	 * 绘制指定 drawType 的加速缓冲。
	 *
	 * <p>{@code prepareBuffers()} 内部有幂等守卫（{@code prepared} 标志），
	 * 故两个时机各调一次不会重复上传/变换 —— 第一次负责上传并投桶，第二次直接返回。
	 * {@code clearBuffers()} 同理只在后一次真正生效。
	 */
	private void drawAccelerated(LayerDrawType drawType) {
		// 只有 SEPARATED 存储才能按 drawType 取到独立桶；其余（如 SORTED）对任何
		// LayerDrawType 都返回同一集合，拆分会把同一批 drawContext 画两遍。
		// 非 SEPARATED 时退化为一次全量绘制（放在不透明时机，与拆分前的行为一致）。
		if (CoreFeature.getLayerStorageType() != LayerStorageType.SEPARATED) {
			drawType = LayerDrawType.ALL;
		}

		if (drawType == LayerDrawType.OPAQUE || drawType == LayerDrawType.ALL) {
			// 帧计时起点：从第一次绘制开始，到半透明 + outline 收尾为止
			frameStartNanos = System.nanoTime();
		}

		CoreStates                      .recordBuffers ();
		CoreBuffers.ENTITY              .prepareBuffers();
		CoreBuffers.BLOCK               .prepareBuffers();
		CoreBuffers.POS                 .prepareBuffers();
		CoreBuffers.POS_COLOR           .prepareBuffers();
		CoreBuffers.POS_TEX             .prepareBuffers();
		CoreBuffers.POS_TEX_COLOR       .prepareBuffers();
		CoreBuffers.POS_COLOR_TEX_LIGHT .prepareBuffers();
		CoreStates                      .restoreBuffers();

		CoreBuffers.ENTITY              .drawBuffers(drawType);
		CoreBuffers.BLOCK               .drawBuffers(drawType);
		CoreBuffers.POS                 .drawBuffers(drawType);
		CoreBuffers.POS_COLOR           .drawBuffers(drawType);
		CoreBuffers.POS_TEX             .drawBuffers(drawType);
		CoreBuffers.POS_TEX_COLOR       .drawBuffers(drawType);
		CoreBuffers.POS_COLOR_TEX_LIGHT .drawBuffers(drawType);
	}

	/** 帧计时起点，供 {@link #reportFrame()} 统计整帧加速开销 */
	private long frameStartNanos;

	/** 收尾：清空缓冲并上报统计（只在最后一个绘制时机调用一次） */
	private void reportFrame() {
		CoreBuffers.ENTITY              .clearBuffers();
		CoreBuffers.BLOCK               .clearBuffers();
		CoreBuffers.POS                 .clearBuffers();
		CoreBuffers.POS_COLOR           .clearBuffers();
		CoreBuffers.POS_TEX             .clearBuffers();
		CoreBuffers.POS_TEX_COLOR       .clearBuffers();
		CoreBuffers.POS_COLOR_TEX_LIGHT .clearBuffers();
		CoreBuffers.POS_TEX_COLOR_OUTLINE.clearBuffers();

		AccelStats.FRAMES		++;
		AccelStats.TOTAL_NANOS	+= System.nanoTime() - frameStartNanos;
		AccelStats.report();
	}
}