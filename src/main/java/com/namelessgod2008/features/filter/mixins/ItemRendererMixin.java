package com.namelessgod2008.features.filter.mixins;

import com.namelessgod2008.features.entities.AcceleratedEntityRenderingFeature;
import com.namelessgod2008.features.filter.FilterFeature;
import com.namelessgod2008.features.filter.ItemStackFilterStack;
import com.namelessgod2008.features.items.AcceleratedItemRenderingFeature;
import com.namelessgod2008.features.text.AcceleratedTextRenderingFeature;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.4 item filter.
 *
 * The 1.21.1 upstream used @WrapMethod on ItemRenderer.render(ItemStack, ...)
 * which no longer exists in 1.21.4. Instead we inject at renderItem HEAD/RETURN
 * (the static entry that all world-item rendering funnels through) and read the
 * currently-rendered ItemStack from {@link ItemStackFilterStack}, populated by
 * ItemModelResolverMixin (world) and GuiBatchingController.renderItemContexts
 * (batched GUI).
 *
 * When the stack fails the filter test (should be excluded from acceleration),
 * switch the entity/item/text pipelines to vanilla for the duration of the
 * draw call.
 */
@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

	@Inject(method = "renderItem", at = @At("HEAD"))
	private static void startFilter(
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
		if (		!FilterFeature.isEnabled			()
				||	!FilterFeature.shouldFilterItems	()
		) {
			return;
		}

		var itemStack = ItemStackFilterStack.peek();

		if (itemStack.isEmpty() || FilterFeature.testItem(itemStack)) {
			return;
		}

		AcceleratedEntityRenderingFeature.useVanillaPipeline();
		AcceleratedItemRenderingFeature.useVanillaPipeline();
		AcceleratedTextRenderingFeature.useVanillaPipeline();
	}

	@Inject(method = "renderItem", at = @At("RETURN"))
	private static void endFilter(
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
		if (		!FilterFeature.isEnabled			()
				||	!FilterFeature.shouldFilterItems	()
		) {
			return;
		}

		var itemStack = ItemStackFilterStack.peek();

		if (itemStack.isEmpty() || FilterFeature.testItem(itemStack)) {
			return;
		}

		AcceleratedEntityRenderingFeature.resetPipeline();
		AcceleratedItemRenderingFeature.resetPipeline();
		AcceleratedTextRenderingFeature.resetPipeline();
	}
}