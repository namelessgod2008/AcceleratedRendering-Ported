package com.github.argon4w.acceleratedrendering.features.items.mixins.gui;

import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.features.items.AcceleratedItemRenderingFeature;
import com.github.argon4w.acceleratedrendering.features.items.gui.GuiBatchingController;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import com.github.argon4w.acceleratedrendering.core.utils.FastColorCompat;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Function;


@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {

	@Shadow @Final private PoseStack pose;

	@WrapMethod(method	= "fill(IIIII)V")
	public void renderFillFast(
			int				minX,
			int				minY,
			int				maxX,
			int				maxY,
			int				color,
			Operation<Void>	original
	) {
		if (		!	CoreFeature.isLoaded				()
				||	!	CoreFeature.isGuiBatching			()
				||		CoreFeature.shouldByPassGuiBatching	()
		) {
			original.call(
					minX,
					minY,
					maxX,
					maxY,
					color
			);
			return;
		}

		var last = pose.last();

		GuiBatchingController.INSTANCE.submitFill(
				last		.pose	(),
				last		.normal	(),
				RenderType	.gui	(),
				minX,
				minY,
				maxX,
				maxY,
				0,
				color
		);
	}

	@WrapMethod(method	= "fill(Lnet/minecraft/client/renderer/RenderType;IIIIII)V")
	public void renderFillFast(
			RenderType		renderType,
			int				minX,
			int				minY,
			int				maxX,
			int				maxY,
			int				blitOffset,
			int				color,
			Operation<Void>	original
	) {
		if (		!	CoreFeature.isLoaded				()
				||	!	CoreFeature.isGuiBatching			()
				||		CoreFeature.shouldByPassGuiBatching	()
		) {
			original.call(
					renderType,
					minX,
					minY,
					maxX,
					maxY,
					blitOffset,
					color
			);
			return;
		}

		var last = pose.last();

		GuiBatchingController.INSTANCE.submitFill(
				last.pose	(),
				last.normal	(),
				renderType,
				minX,
				minY,
				maxX,
				maxY,
				blitOffset,
				color
		);
	}

	@WrapMethod(method = "fillGradient(Lnet/minecraft/client/renderer/RenderType;IIIIIII)V")
	public void renderGradientFast(
			RenderType		renderType,
			int				minX,
			int				minY,
			int				maxX,
			int				maxY,
			int				colorFrom,
			int				colorTo,
			int				blitOffset,
			Operation<Void>	original
	) {
		if (		!	CoreFeature.isLoaded				()
				||	!	CoreFeature.isGuiBatching			()
				||		CoreFeature.shouldByPassGuiBatching	()
		) {
			original.call(
					renderType,
					minX,
					minY,
					maxX,
					maxY,
					colorFrom,
					colorTo,
					blitOffset
			);
			return;
		}

		var last = pose.last();

		GuiBatchingController.INSTANCE.submitGradient(
				last.pose	(),
				last.normal	(),
				renderType,
				minX,
				minY,
				maxX,
				maxY,
				blitOffset,
				colorFrom,
				colorTo
		);
	}

	@WrapMethod(method = "fillRenderType")
	public void renderRenderTypeFast(
			RenderType		renderType,
			int				minX,
			int				minY,
			int				maxX,
			int				maxY,
			int				blitOffset,
			Operation<Void>	original
	) {
		if (		!	CoreFeature.isLoaded				()
				||	!	CoreFeature.isGuiBatching			()
				||		CoreFeature.shouldByPassGuiBatching	()
		) {
			original.call(
					renderType,
					minX,
					minY,
					maxX,
					maxY,
					blitOffset
			);
			return;
		}

		var last = pose.last();

		GuiBatchingController.INSTANCE.submitGradient(
				last.pose	(),
				last.normal	(),
				renderType,
				minX,
				minY,
				maxX,
				maxY,
				blitOffset,
				-1,
				-1
		);
	}

	// 1.21.4 innerBlit: added Function<ResourceLocation, RenderType> param + trailing int; blitOffset removed
	@WrapMethod(method = "innerBlit", remap = false, require = 0)
	public void renderBlitFast(
			Function<ResourceLocation, RenderType>	renderTypeGetter,
			ResourceLocation						atlasLocation,
			int										minX,
			int										maxX,
			int										minY,
			int										maxY,
			float									minU,
			float									maxU,
			float									minV,
			float									maxV,
			int										color,
			Operation<Void>							original
	) {
		if (		!	CoreFeature.isLoaded				()
				||	!	CoreFeature.isGuiBatching			()
				||		CoreFeature.shouldByPassGuiBatching	()
		) {
			original.call(
					renderTypeGetter,
					atlasLocation,
					minX,
					maxX,
					minY,
					maxY,
					minU,
					maxU,
					minV,
					maxV,
					color
			);
			return;
		}

		var last = pose.last();

		GuiBatchingController.INSTANCE.submitBlit(
				last.pose	(),
				last.normal	(),
				atlasLocation,
				minX,
				maxX,
				minY,
				maxY,
				0,
				color,
				minU,
				maxU,
				minV,
				maxV,
				null
		);
	}

	// TODO 1.21.4: renderItem no longer calls ItemRenderer.render();
	// replaced by ItemModelResolver.updateForTopItem() + ItemStackRenderState.render()
	// @WrapOperation(
	//			method	= "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
	//			at		= @At(
	//					value	= "INVOKE",
	//					target	= "Lnet/minecraft/client/renderer/item/ItemStackRenderState;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"
	//			)
	// )
	// public void renderItemFast( ... ) { ... }
}
