package com.namelessgod2008.features.filter.mixins;

import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.core.utils.PoseStackExtension;
import com.namelessgod2008.features.entities.AcceleratedEntityRenderingFeature;
import com.namelessgod2008.features.filter.FilterFeature;
import com.namelessgod2008.features.items.AcceleratedItemRenderingFeature;
import com.namelessgod2008.features.items.gui.GuiBatchingController;
import com.namelessgod2008.features.items.gui.contexts.ItemDrawContext;
import com.namelessgod2008.features.text.AcceleratedTextRenderingFeature;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import lombok.experimental.ExtensionMethod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@ExtensionMethod(PoseStackExtension		.class)
@Mixin			(GuiBatchingController	.class)
public class GuiBatchingControllerMixin {

	@Unique private final List<ItemDrawContext> filteredFlatItemDrawContexts	= new ReferenceArrayList<>();
	@Unique private final List<ItemDrawContext> filteredBlockItemDrawContexts	= new ReferenceArrayList<>();

	@WrapOperation(
			method	= "submitItem",
			at		= @At(
					value	= "INVOKE",
					target	= "Ljava/util/List;add(Ljava/lang/Object;)Z"
			)
	)
	public boolean filterItem(
			List<?>								drawContexts,
			Object								itemRenderContext,
			Operation<Boolean>					original,
			@Local(name = "blockLight") boolean	blockLight
	) {
		var pass =	!	CoreFeature		.isLoaded			()
				||	!	FilterFeature	.isEnabled			()
				||	!	FilterFeature	.shouldFilterItems	()
				||		FilterFeature	.testItem			(((ItemDrawContext) itemRenderContext).itemStack());

		if (!pass) {
			drawContexts = blockLight
					? filteredBlockItemDrawContexts
					: filteredFlatItemDrawContexts;
		}

		return original.call(drawContexts, itemRenderContext);
	}

	@Inject(
			method	= "flushBatching(Lnet/minecraft/client/gui/GuiGraphics;)F",
			at		= @At(
					value	= "INVOKE",
					target	= "Lcom/namelessgod2008/features/items/gui/GuiBatchingController;flushBatching()V",
					shift	= At.Shift.AFTER
			)
	)
	public void flushFilteredItems(
			GuiGraphics														graphics,
			CallbackInfoReturnable<Float>									cir,
			@Local(name = "bufferSource")	MultiBufferSource.BufferSource	bufferSource,
			@Local(name = "itemRenderer")	ItemRenderer					itemRenderer,
			@Local(name = "poseStack")		PoseStack						poseStack
	) {
		AcceleratedEntityRenderingFeature	.useVanillaPipeline();
		AcceleratedItemRenderingFeature		.useVanillaPipeline();
		AcceleratedTextRenderingFeature		.useVanillaPipeline();

		Lighting.setupForFlatItems();

		for (var context : filteredFlatItemDrawContexts) {
			poseStack.pushPose	();
			poseStack.setPose	(context.transform(), context.normal());

// TODO 1.21.4: 			itemRenderer.render(
// TODO 1.21.4: 					context.itemStack		(),
// TODO 1.21.4: 					context.displayContext	(),
// TODO 1.21.4: 					context.leftHand		(),
// TODO 1.21.4: 					poseStack,
// TODO 1.21.4: 					bufferSource,
// TODO 1.21.4: 					context.combinedLight	(),
// TODO 1.21.4: 					context.combinedOverlay	(),
// TODO 1.21.4: 					context.bakedModel		()
// TODO 1.21.4: 			);

			poseStack.popPose();
		}

		graphics	.flush			();
		Lighting	.setupFor3DItems();

		for (var context : filteredBlockItemDrawContexts) {
			poseStack.pushPose	();
			poseStack.setPose	(context.transform(), context.normal());

// TODO 1.21.4: 			itemRenderer.render(
// TODO 1.21.4: 					context.itemStack		(),
// TODO 1.21.4: 					context.displayContext	(),
// TODO 1.21.4: 					context.leftHand		(),
// TODO 1.21.4: 					poseStack,
// TODO 1.21.4: 					bufferSource,
// TODO 1.21.4: 					context.combinedLight	(),
// TODO 1.21.4: 					context.combinedOverlay	(),
// TODO 1.21.4: 					context.bakedModel		()
// TODO 1.21.4: 			);

			poseStack.popPose();
		}

		graphics							.flush			();
		AcceleratedEntityRenderingFeature	.resetPipeline	();
		AcceleratedItemRenderingFeature		.resetPipeline	();
		AcceleratedTextRenderingFeature		.resetPipeline	();
	}

	@Inject(
			method	= "flushBatching(Lnet/minecraft/client/gui/GuiGraphics;)F",
			at		= @At(
					value	= "INVOKE",
					target	= "Ljava/util/List;clear()V",
					ordinal	= 0,
					shift	= At.Shift.BEFORE
			)
	)
	public void clearFilteredItems(GuiGraphics graphics, CallbackInfoReturnable<Float> cir) {
		filteredFlatItemDrawContexts	.clear();
		filteredBlockItemDrawContexts	.clear();
	}
}
