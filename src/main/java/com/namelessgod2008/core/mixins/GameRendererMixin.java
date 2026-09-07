package com.namelessgod2008.core.mixins;

import com.namelessgod2008.core.CoreBuffers;
import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.core.CoreStates;
import com.namelessgod2008.core.buffers.accelerated.layers.LayerDrawType;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

	@Inject(
		method = "renderItemInHand",
		require = 0,
		at = @At(
			value	= "INVOKE",
			target	= "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/player/LocalPlayer;I)V",
			shift	= At.Shift.BEFORE
		)
	)
	public void startRenderItemInHandsFast(
		Camera			camera,
		float			partialTick,
		Matrix4f		projectionMatrix,
		CallbackInfo	ci
	) {
		CoreFeature.setRenderingHand();
	}

	@Inject(
		require = 0,
		method = "renderItemInHand",
		at = @At(
			value	= "INVOKE",
			target	= "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/player/LocalPlayer;I)V",
			shift	= At.Shift.AFTER
		)
	)
	public void stopRenderItemInHandsFast(
		Camera			camera,
		float			partialTick,
		Matrix4f		projectionMatrix,
		CallbackInfo	ci
	) {
		CoreFeature.resetRenderingHand();
	}
}
