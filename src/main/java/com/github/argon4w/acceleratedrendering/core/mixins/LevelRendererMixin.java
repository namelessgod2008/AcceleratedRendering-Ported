package com.github.argon4w.acceleratedrendering.core.mixins;

import com.github.argon4w.acceleratedrendering.core.CoreBuffers;
import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.CoreStates;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.layers.LayerDrawType;
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
            ordinal = 0
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
