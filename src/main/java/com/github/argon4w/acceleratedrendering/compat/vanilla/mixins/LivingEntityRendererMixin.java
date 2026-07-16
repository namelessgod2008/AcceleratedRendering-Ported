package com.github.argon4w.acceleratedrendering.compat.vanilla.mixins;

import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.features.mods.ModsFeature;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.4 update — render() now takes LivingEntityRenderState instead of LivingEntity.
 * RenderLayer.render() now takes (PoseStack, MultiBufferSource, int, EntityRenderState, float, float).
 */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

	@Inject(
		method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
		at = @At("HEAD")
	)
	public void initLayer(
		LivingEntityRenderState		state,
		PoseStack					poseStack,
		MultiBufferSource			buffer,
		int							packedLight,
		CallbackInfo				ci,
		@Share("layer") LocalIntRef	layer
	) {
		layer.set(CoreFeature.getDefaultLayer() + 1);
	}

	@SuppressWarnings("rawtypes")
	@WrapOperation(
		method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/EntityRenderState;FF)V"
		)
	)
	public void wrapRenderLayer(
		RenderLayer					instance,
		PoseStack					poseStack,
		MultiBufferSource			bufferSource,
		int							packedLight,
		EntityRenderState			entityRenderState,
		float						yRot,
		float						xRot,
		Operation<Void>				original,
		@Share("layer") LocalIntRef	layer
	) {
		if (		!CoreFeature.isLoaded			()
				||	!ModsFeature.isEnabled			()
				||	!ModsFeature.shouldFixVanilla	()
		) {
			original.call(
				instance,
				poseStack,
				bufferSource,
				packedLight,
				entityRenderState,
				yRot,
				xRot
			);
			return;
		}

		CoreFeature.forceSetDefaultLayer(layer.get());

		original.call(
			instance,
			poseStack,
			bufferSource,
			packedLight,
			entityRenderState,
			yRot,
			xRot
		);

		CoreFeature.resetDefaultLayer();

		layer.set(layer.get() + 1);
	}
}
