package com.github.argon4w.acceleratedrendering.features.filter.mixins;

import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.features.entities.AcceleratedEntityRenderingFeature;
import com.github.argon4w.acceleratedrendering.features.filter.FilterFeature;
import com.github.argon4w.acceleratedrendering.features.items.AcceleratedItemRenderingFeature;
import com.github.argon4w.acceleratedrendering.features.text.AcceleratedTextRenderingFeature;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.4 update — tryRender removed; now targets render(E, float, PoseStack, MultiBufferSource).
 */
@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {

	@Inject(
		method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
		at = @At("HEAD")
	)
	public void filterBlockEntity(
		BlockEntity					blockEntity,
		float						partialTick,
		PoseStack					poseStack,
		MultiBufferSource			bufferSource,
		CallbackInfo				ci,
		@Share("filtered") LocalBooleanRef	filtered
	) {
		if (!CoreFeature.isLoaded()
			|| !FilterFeature.isEnabled()
			|| !FilterFeature.shouldFilterBlockEntities()
		) {
			filtered.set(false);
			return;
		}

		if (!FilterFeature.testBlockEntity(blockEntity)) {
			AcceleratedEntityRenderingFeature	.useVanillaPipeline();
			AcceleratedItemRenderingFeature		.useVanillaPipeline();
			AcceleratedTextRenderingFeature		.useVanillaPipeline();
			filtered.set(true);
		} else {
			filtered.set(false);
		}
	}

	@Inject(
		method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
		at = @At("RETURN")
	)
	public void resetBlockEntityFilter(
		BlockEntity					blockEntity,
		float						partialTick,
		PoseStack					poseStack,
		MultiBufferSource			bufferSource,
		CallbackInfo				ci,
		@Share("filtered") LocalBooleanRef	filtered
	) {
		if (filtered.get()) {
			AcceleratedEntityRenderingFeature	.resetPipeline();
			AcceleratedItemRenderingFeature		.resetPipeline();
			AcceleratedTextRenderingFeature		.resetPipeline();
		}
	}
}
