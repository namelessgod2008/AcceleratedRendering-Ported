package com.namelessgod2008.features.filter.mixins;

import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.features.entities.AcceleratedEntityRenderingFeature;
import com.namelessgod2008.features.filter.FilterFeature;
import com.namelessgod2008.features.items.AcceleratedItemRenderingFeature;
import com.namelessgod2008.features.text.AcceleratedTextRenderingFeature;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

	@WrapOperation(
			method	= "renderEntities",
			at		= @At(
					value	= "INVOKE",
					target	= "Lnet/minecraft/client/renderer/LevelRenderer;renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"
			)
	)
	public void filterEntity(
			LevelRenderer		instance,
			Entity				entity,
			double				camX,
			double				camY,
			double				camZ,
			float				partialTick,
			PoseStack			poseStack,
			MultiBufferSource	bufferSource,
			Operation<Void>		original
	) {
		var pass =	!	CoreFeature		.isLoaded				()
				||	!	FilterFeature	.isEnabled				()
				||	!	FilterFeature	.shouldFilterEntities	()
				||		FilterFeature	.testEntity				(entity);

		if (!pass) {
			AcceleratedEntityRenderingFeature	.useVanillaPipeline();
			AcceleratedItemRenderingFeature		.useVanillaPipeline();
			AcceleratedTextRenderingFeature		.useVanillaPipeline();
		}

		original.call(
				instance,
				entity,
				camX,
				camY,
				camZ,
				partialTick,
				poseStack,
				bufferSource
		);

		if (!pass) {
			AcceleratedEntityRenderingFeature	.resetPipeline();
			AcceleratedItemRenderingFeature		.resetPipeline();
			AcceleratedTextRenderingFeature		.resetPipeline();
		}
	}
}
