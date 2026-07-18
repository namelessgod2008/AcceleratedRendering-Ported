package com.namelessgod2008.compat.iris.mixins.vanilla;

import com.namelessgod2008.compat.iris.IrisCompatBuffers;
import com.namelessgod2008.core.CoreBuffers;
import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.core.CoreStates;
import com.namelessgod2008.core.buffers.accelerated.layers.LayerDrawType;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.resource.ResourceHandle;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.4: All rendering injections moved from renderLevel to method_62214 (frame graph lambda).
 * Injection points verified against method_62214 bytecode:
 *   endBatch()V:        offsets 604 (ordinal 0), 623 (ordinal 1)
 *   endLastBatch()V:    offsets 336 (ord 0), 370 (ord 1), 518 (ord 2)
 *   endOutlineBatch()V: offset 466
 *   "translucent":      offset 686 (ordinal 1)
 */
@Mixin(value = LevelRenderer.class, priority = 999)
public class LevelRendererMixin {

	@Inject(method = "method_62214",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V", ordinal = 0))
	public void drawIrisAllCoreBuffers(
		FogParameters fogParameters, DeltaTracker deltaTracker, Camera camera, ProfilerFiller profiler,
		Matrix4f frustumMatrix, Matrix4f projectionMatrix,
		ResourceHandle<?> rh1, ResourceHandle<?> rh2, ResourceHandle<?> rh3, ResourceHandle<?> rh4,
		boolean renderBlockOutline, Frustum frustum, ResourceHandle<?> rh5,
		CallbackInfo ci
	) {
		if (!CoreFeature.isLoaded()) return;
		CoreStates.recordBuffers();
		CoreBuffers.ENTITY.prepareBuffers(); CoreBuffers.BLOCK.prepareBuffers();
		CoreBuffers.POS.prepareBuffers(); CoreBuffers.POS_COLOR.prepareBuffers();
		CoreBuffers.POS_TEX.prepareBuffers(); CoreBuffers.POS_TEX_COLOR.prepareBuffers();
		CoreBuffers.POS_COLOR_TEX_LIGHT.prepareBuffers();
		CoreStates.restoreBuffers();
		CoreBuffers.ENTITY.drawBuffers(LayerDrawType.ALL); CoreBuffers.BLOCK.drawBuffers(LayerDrawType.ALL);
		CoreBuffers.POS.drawBuffers(LayerDrawType.ALL); CoreBuffers.POS_COLOR.drawBuffers(LayerDrawType.ALL);
		CoreBuffers.POS_TEX.drawBuffers(LayerDrawType.ALL); CoreBuffers.POS_TEX_COLOR.drawBuffers(LayerDrawType.ALL);
		CoreBuffers.POS_COLOR_TEX_LIGHT.drawBuffers(LayerDrawType.ALL);
		CoreBuffers.ENTITY.clearBuffers(); CoreBuffers.BLOCK.clearBuffers();
		CoreBuffers.POS.clearBuffers(); CoreBuffers.POS_COLOR.clearBuffers();
		CoreBuffers.POS_TEX.clearBuffers(); CoreBuffers.POS_TEX_COLOR.clearBuffers();
		CoreBuffers.POS_COLOR_TEX_LIGHT.clearBuffers();
	}

	@Inject(method = "method_62214",
		at = @At(value = "CONSTANT", args = "stringValue=translucent", ordinal = 0))
	public void drawIrisOpaqueCoreBuffers(
		FogParameters fogParameters, DeltaTracker deltaTracker, Camera camera, ProfilerFiller profiler,
		Matrix4f frustumMatrix, Matrix4f projectionMatrix,
		ResourceHandle<?> rh1, ResourceHandle<?> rh2, ResourceHandle<?> rh3, ResourceHandle<?> rh4,
		boolean renderBlockOutline, Frustum frustum, ResourceHandle<?> rh5,
		CallbackInfo ci
	) {
		if (!CoreFeature.isLoaded()) return;
		CoreStates.recordBuffers();
		CoreBuffers.ENTITY.prepareBuffers(); CoreBuffers.BLOCK.prepareBuffers();
		CoreBuffers.POS.prepareBuffers(); CoreBuffers.POS_COLOR.prepareBuffers();
		CoreBuffers.POS_TEX.prepareBuffers(); CoreBuffers.POS_TEX_COLOR.prepareBuffers();
		CoreBuffers.POS_COLOR_TEX_LIGHT.prepareBuffers();
		CoreStates.restoreBuffers();
		CoreBuffers.ENTITY.drawBuffers(LayerDrawType.OPAQUE); CoreBuffers.BLOCK.drawBuffers(LayerDrawType.OPAQUE);
		CoreBuffers.POS.drawBuffers(LayerDrawType.OPAQUE); CoreBuffers.POS_COLOR.drawBuffers(LayerDrawType.OPAQUE);
		CoreBuffers.POS_TEX.drawBuffers(LayerDrawType.OPAQUE); CoreBuffers.POS_TEX_COLOR.drawBuffers(LayerDrawType.OPAQUE);
		CoreBuffers.POS_COLOR_TEX_LIGHT.drawBuffers(LayerDrawType.OPAQUE);
	}

	@Inject(method = "method_62214",
		at = @At(value = "CONSTANT", args = "stringValue=translucent", ordinal = 0, shift = At.Shift.AFTER))
	public void drawIrisTranslucentCoreBuffers(
		FogParameters fogParameters, DeltaTracker deltaTracker, Camera camera, ProfilerFiller profiler,
		Matrix4f frustumMatrix, Matrix4f projectionMatrix,
		ResourceHandle<?> rh1, ResourceHandle<?> rh2, ResourceHandle<?> rh3, ResourceHandle<?> rh4,
		boolean renderBlockOutline, Frustum frustum, ResourceHandle<?> rh5,
		CallbackInfo ci
	) {
		if (!CoreFeature.isLoaded()) return;
		CoreBuffers.ENTITY.drawBuffers(LayerDrawType.TRANSLUCENT); CoreBuffers.BLOCK.drawBuffers(LayerDrawType.TRANSLUCENT);
		CoreBuffers.POS.drawBuffers(LayerDrawType.TRANSLUCENT); CoreBuffers.POS_COLOR.drawBuffers(LayerDrawType.TRANSLUCENT);
		CoreBuffers.POS_TEX.drawBuffers(LayerDrawType.TRANSLUCENT); CoreBuffers.POS_TEX_COLOR.drawBuffers(LayerDrawType.TRANSLUCENT);
		CoreBuffers.POS_COLOR_TEX_LIGHT.drawBuffers(LayerDrawType.TRANSLUCENT);
		CoreBuffers.ENTITY.clearBuffers(); CoreBuffers.BLOCK.clearBuffers();
		CoreBuffers.POS.clearBuffers(); CoreBuffers.POS_COLOR.clearBuffers();
		CoreBuffers.POS_TEX.clearBuffers(); CoreBuffers.POS_TEX_COLOR.clearBuffers();
		CoreBuffers.POS_COLOR_TEX_LIGHT.clearBuffers();
	}

	@WrapOperation(method = "method_62214",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endLastBatch()V"))
	public void preventDrawVanillaCoreBuffers(MultiBufferSource.BufferSource instance, Operation<Void> original) {
		instance.endLastBatch();
	}

	@Inject(method = "close", at = @At("TAIL"))
	public void deleteIrisBuffers(CallbackInfo ci) {
		if (!CoreFeature.isLoaded()) return;
		IrisCompatBuffers.BLOCK_SHADOW.delete(); IrisCompatBuffers.ENTITY_SHADOW.delete();
		IrisCompatBuffers.GLYPH_SHADOW.delete(); IrisCompatBuffers.POS_TEX_SHADOW.delete();
		IrisCompatBuffers.POS_TEX_COLOR_SHADOW.delete();
		IrisCompatBuffers.ENTITY_HAND.delete(); IrisCompatBuffers.BLOCK_HAND.delete();
		IrisCompatBuffers.POS_HAND.delete(); IrisCompatBuffers.POS_COLOR_HAND.delete();
		IrisCompatBuffers.POS_TEX_HAND.delete(); IrisCompatBuffers.POS_TEX_COLOR_HAND.delete();
		IrisCompatBuffers.POS_COLOR_TEX_LIGHT_HAND.delete();
	}
}
