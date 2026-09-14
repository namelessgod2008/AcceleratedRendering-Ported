package com.namelessgod2008.core.mixins;

import com.namelessgod2008.core.AccelStats;
import com.namelessgod2008.core.CoreBuffers;
import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.core.CoreStates;
import com.namelessgod2008.core.buffers.accelerated.layers.LayerDrawType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
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
	 * 26.1: 绘制必须在 frame.execute() 【期间】执行，而不是之后。
	 *
	 * 原因：26.1 的纹理是懒加载的（TextureManager.getTexture → 未注册则同步上传）。
	 * 在 execute() 之后帧图已提交，GPU 无法再完成纹理上传，会抛
	 * ReportedException("Uploading texture") → 采样器绑定失败 → 绘制异常/画面缺失。
	 * 原版实体渲染同样发生在 frame pass 内（pass.executes），此处与之对齐：
	 * 注册一个 FramePass，把加速缓冲的绘制放到该 pass 中。
	 *
	 * 注入 addMainPass 的 TAIL：它的第一个参数即 FrameGraphBuilder（作为普通 handler
	 * 参数即可取得，无需 MixinExtras sugar），此时 main pass 已注册，我们追加的 pass
	 * 会在其之后执行——正是实体顶点已写入加速缓冲之后。
	 */
	@Inject(method = "addMainPass", at = @At("TAIL"), require = 0)
	public void drawCoreBuffers(
		FrameGraphBuilder					frame,
		net.minecraft.client.renderer.culling.Frustum			frustum,
		Matrix4fc							modelViewMatrix,
		GpuBufferSlice						terrainFog,
		boolean								renderOutline,
		net.minecraft.client.renderer.state.level.LevelRenderState	levelRenderState,
		DeltaTracker						deltaTracker,
		net.minecraft.util.profiling.ProfilerFiller				profiler,
		ChunkSectionsToRender				chunkSectionsToRender,
		CallbackInfo						ci
	) {
		if (!CoreFeature.isLoaded()) {
			return;
		}

		var pass = frame.addPass("accelerated_rendering");

		// 注意：readsAndWrites 会把原句柄的内容移入新句柄（writeAndAlias），
		// 必须写回字段，否则后续 pass（clouds/weather 等）引用的旧句柄会失效并抛
		// "Handle main#N is no longer valid"。LevelRenderer 自身的 pass 同样是这种写法。
		this.targets.main = pass.readsAndWrites(this.targets.main);
		pass.executes(this::renderAcceleratedBuffers);
	}

	@org.spongepowered.asm.mixin.Shadow
	private net.minecraft.client.renderer.LevelTargetBundle targets;

	/** 在 frame pass 内绘制加速缓冲（此时 GPU 活跃，纹理懒加载/绘制均可用） */
	private void renderAcceleratedBuffers() {
		long statStart = System.nanoTime();

		CoreStates                      .recordBuffers();
		CoreBuffers.ENTITY              .prepareBuffers();
		CoreBuffers.BLOCK               .prepareBuffers();
		CoreBuffers.POS                 .prepareBuffers();
		CoreBuffers.POS_COLOR           .prepareBuffers();
		CoreBuffers.POS_TEX             .prepareBuffers();
		CoreBuffers.POS_TEX_COLOR       .prepareBuffers();
		CoreBuffers.POS_COLOR_TEX_LIGHT .prepareBuffers();
		CoreStates                      .restoreBuffers();

		CoreBuffers.ENTITY              .drawBuffers(LayerDrawType.ALL);
		CoreBuffers.BLOCK               .drawBuffers(LayerDrawType.ALL);
		CoreBuffers.POS                 .drawBuffers(LayerDrawType.ALL);
		CoreBuffers.POS_COLOR           .drawBuffers(LayerDrawType.ALL);
		CoreBuffers.POS_TEX             .drawBuffers(LayerDrawType.ALL);
		CoreBuffers.POS_TEX_COLOR       .drawBuffers(LayerDrawType.ALL);
		CoreBuffers.POS_COLOR_TEX_LIGHT .drawBuffers(LayerDrawType.ALL);

		CoreBuffers.ENTITY              .clearBuffers();
		CoreBuffers.BLOCK               .clearBuffers();
		CoreBuffers.POS                 .clearBuffers();
		CoreBuffers.POS_COLOR           .clearBuffers();
		CoreBuffers.POS_TEX             .clearBuffers();
		CoreBuffers.POS_TEX_COLOR       .clearBuffers();
		CoreBuffers.POS_COLOR_TEX_LIGHT .clearBuffers();

		// 26.1: outline 缓冲同样在此绘制
		CoreStates                        .recordBuffers();
		CoreBuffers.POS_TEX_COLOR_OUTLINE .prepareBuffers();
		CoreStates                        .restoreBuffers();
		CoreBuffers.POS_TEX_COLOR_OUTLINE .drawBuffers(LayerDrawType.ALL);
		CoreBuffers.POS_TEX_COLOR_OUTLINE .clearBuffers();

		AccelStats.FRAMES		++;
		AccelStats.TOTAL_NANOS	+= System.nanoTime() - statStart;
		AccelStats.report();
	}
}