package com.namelessgod2008.core.mixins;

import com.namelessgod2008.core.CoreBuffers;
import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.core.CoreStates;
import com.namelessgod2008.core.buffers.accelerated.layers.LayerDrawType;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "renderLevel", at = @At("HEAD"))
    public void startRenderLevel(
        GraphicsResourceAllocator allocator,
        DeltaTracker deltaTracker,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        Matrix4f frustumMatrix,
        Matrix4f projectionMatrix,
        CallbackInfo ci
    ) {
        CoreFeature.setRenderingLevel();
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    public void stopRenderLevel(
        GraphicsResourceAllocator allocator,
        DeltaTracker deltaTracker,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        Matrix4f frustumMatrix,
        Matrix4f projectionMatrix,
        CallbackInfo ci
    ) {
        CoreFeature.resetRenderingLevel();
    }

    @Inject(
        method = "method_62214",
        require = 0,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V"
        )
    )
    public void endOutlineBatches(
        FogParameters fogParameters,
        DeltaTracker deltaTracker,
        Camera camera,
        ProfilerFiller profiler,
        Matrix4f frustumMatrix,
        Matrix4f projectionMatrix,
        ResourceHandle<RenderTarget> resourcehandle2,
        ResourceHandle<RenderTarget> resourcehandle,
        ResourceHandle<RenderTarget> resourcehandle3,
        ResourceHandle<RenderTarget> resourcehandle4,
		boolean renderBlockOutline,
        Frustum frustum,

        ResourceHandle<RenderTarget> resourcehandle1,
        CallbackInfo ci
    ) {
        if (!CoreFeature.isLoaded()) {
            return;
        }
        CoreStates                        .recordBuffers();
        CoreBuffers.POS_TEX_COLOR_OUTLINE .prepareBuffers();
        CoreStates                        .restoreBuffers();
        CoreBuffers.POS_TEX_COLOR_OUTLINE .drawBuffers(LayerDrawType.ALL);
        CoreBuffers.POS_TEX_COLOR_OUTLINE .clearBuffers();
    }

    @Inject(
        method = "method_62214",
        require = 0,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endLastBatch()V",
            // ordinal 0 (offset 336) 位于 renderEntities 之后、renderBlockEntities(offset 365) 之前 —
            // 告示牌等方块实体阶段写入的加速数据（含文字）在 ordinal 0 绘制点之后才产生，当帧永远画不出来，
            // 随后还会被 GUI flushBatching 以正交矩阵错误消费。必须用 ordinal 1 (offset 370)：
            // renderBlockEntities 之后的 endLastBatch，与 1.21.1 上游的绘制时机语义一致。
            ordinal = 1
        )
    )
    public void drawCoreBuffers(
        FogParameters fogParameters,
        DeltaTracker deltaTracker,
        Camera camera,
        ProfilerFiller profiler,
        Matrix4f frustumMatrix,
        Matrix4f projectionMatrix,
        ResourceHandle<RenderTarget> resourcehandle2,
        ResourceHandle<RenderTarget> resourcehandle,
        ResourceHandle<RenderTarget> resourcehandle3,
        ResourceHandle<RenderTarget> resourcehandle4,
		boolean renderBlockOutline,
        Frustum frustum,

        ResourceHandle<RenderTarget> resourcehandle1,
        CallbackInfo ci
    ) {
        if (!CoreFeature.isLoaded()) {
            return;
        }
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
    }
}
