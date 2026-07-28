package com.namelessgod2008.features.items.mixins;

import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.namelessgod2008.core.utils.DirectionUtils;
import com.namelessgod2008.features.items.AcceleratedItemRenderingFeature;
import com.namelessgod2008.features.items.AcceleratedQuadsRenderer;
import com.namelessgod2008.features.items.BakedModelExtension;
import com.namelessgod2008.features.items.colors.TintLayerColors;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.experimental.ExtensionMethod;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.4 ItemRendererMixin — accelerates item rendering by injecting at
 * renderItem() HEAD with priority=999 to run before Sodium's FRAPI mixin
 * (which cancels the method body, preventing @WrapOperation from firing).
 *
 * renderItem is public static with 9 params:
 * (ItemDisplayContext, PoseStack, MultiBufferSource, int, int, int[], BakedModel, RenderType, FoilType)
 *
 * Foiled items (FoilType != NONE) are NOT accelerated because the
 * accelerated pipeline does not support VertexMultiConsumer foil wrapping.
 */
@ExtensionMethod(value = {VertexConsumerExtension.class, BakedModelExtension.class})
@Mixin(value = {ItemRenderer.class}, priority = 999)
public class ItemRendererMixin {

	@Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
	private static void accelerateAtHead(
		ItemDisplayContext displayContext,
		PoseStack poseStack,
		MultiBufferSource bufferSource,
		int packedLight,
		int packedOverlay,
		int[] tintLayers,
		BakedModel bakedModel,
		RenderType renderType,
		ItemStackRenderState.FoilType foilType,
		CallbackInfo ci
	) {
		if (!CoreFeature.isLoaded()
			|| !AcceleratedItemRenderingFeature.isEnabled()
			|| !AcceleratedItemRenderingFeature.shouldUseAcceleratedPipeline()
			|| !CoreFeature.isRenderingLevel()
			|| foilType != ItemStackRenderState.FoilType.NONE
		) {
			return;
		}

		var buffer = bufferSource.getBuffer(renderType);
		var extension = buffer.getAccelerated();
		if (!extension.isAccelerated()) {
			return;
		}

		ci.cancel();

		var pose = poseStack.last();
		var random = RandomSource.create(42L);

		if (tintLayers == null || tintLayers.length == 0) {
			if (bakedModel instanceof com.namelessgod2008.features.items.IAcceleratedBakedModel accelModel
				&& accelModel.isAccelerated()) {
				accelModel.renderItemFast(null, random, pose, extension, packedLight, packedOverlay);
				return;
			}
		}

		if (AcceleratedItemRenderingFeature.shouldBakeMeshForQuad()) {
			var color = new TintLayerColors(tintLayers);
			for (var direction : DirectionUtils.FULL) {
				random.setSeed(42L);
				extension.doRender(
					AcceleratedQuadsRenderer.INSTANCE,
					AcceleratedQuadsRenderer.context(bakedModel.getQuads(null, direction, random), color),
					pose.pose(), pose.normal(), packedLight, packedOverlay, -1);
			}
		}
	}
}
