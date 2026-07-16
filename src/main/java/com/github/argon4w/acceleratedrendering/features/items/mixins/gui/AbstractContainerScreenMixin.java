package com.github.argon4w.acceleratedrendering.features.items.mixins.gui;

import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.features.items.AcceleratedItemRenderingFeature;
import com.github.argon4w.acceleratedrendering.features.items.gui.GuiBatchingController;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import java.util.function.Function;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

	@SuppressWarnings("rawtypes")
	@WrapOperation(
		method = "renderBackground",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderTransparentBackground(Lnet/minecraft/client/gui/GuiGraphics;)V"
		)
	)
	public void immediateDrawTransparentBackground(
		AbstractContainerScreen	instance,
		GuiGraphics				guiGraphics,
		Operation<Void>			original
	) {
		CoreFeature.forceBypassGuiItemBatching();
		original.call(instance, guiGraphics);
		CoreFeature.resetBypassGuiBatching();
	}

	@Inject(
		method = "render",
		at = @At("HEAD")
	)
	public void startBackgroundBatching(
		GuiGraphics						guiGraphics,
		int								mouseX,
		int								mouseY,
		float							partialTick,
		CallbackInfo					ci,
		@Share("depth") LocalFloatRef	depth
	) {
		depth.set(0.0f);
		GuiBatchingController.INSTANCE.startBatching(guiGraphics);
	}

	@Inject(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V",
			shift = At.Shift.BEFORE
		)
	)
	public void flushBackgroundBatching(
		GuiGraphics						guiGraphics,
		int								mouseX,
		int								mouseY,
		float							partialTick,
		CallbackInfo					ci,
		@Share("depth") LocalFloatRef	depth
	) {
		if (!AcceleratedItemRenderingFeature.shouldMergeGuiItemBatches()) {
			depth.set(depth.get() + GuiBatchingController.INSTANCE.flushBatching(guiGraphics));

			guiGraphics
				.pose()
				.last()
				.pose()
				.translateLocal(0.0f, 0.0f, depth.get());
		}
	}

	@Inject(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V",
			shift = At.Shift.AFTER
		)
	)
	public void startItemBatching(
		GuiGraphics		guiGraphics,
		int				mouseX,
		int				mouseY,
		float			partialTick,
		CallbackInfo	ci
	) {
		GuiBatchingController.INSTANCE.startBatching(guiGraphics);
	}

	@Inject(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderLabels(Lnet/minecraft/client/gui/GuiGraphics;II)V",
			shift = At.Shift.AFTER
		)
	)
	public void flushItemBatching(
		GuiGraphics						guiGraphics,
		int								mouseX,
		int								mouseY,
		float							partialTick,
		CallbackInfo					ci,
		@Share("depth") LocalFloatRef	depth
	) {
		depth.set(depth.get() + GuiBatchingController.INSTANCE.flushBatching(guiGraphics));
	}

	@Inject(
		method = "render",
		at = @At("TAIL")
	)
	public void liftGlobalLayer(
		GuiGraphics						guiGraphics,
		int								mouseX,
		int								mouseY,
		float							partialTick,
		CallbackInfo					ci,
		@Share("depth") LocalFloatRef	depth
	) {
		guiGraphics
			.pose()
			.last()
			.pose()
			.translateLocal(0.0f, 0.0f, depth.get());
	}

	// Wrap blitSprite INVOKE inside renderSlotHighlightBack — captures the Function<RL,RenderType> used for render type
	@com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(
		method = "renderSlotHighlightBack",
		at = @org.spongepowered.asm.mixin.injection.At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIII)V"
		)
	)
	public void batchHighlightBack(GuiGraphics guiGraphics, Function<ResourceLocation, RenderType> renderTypeGetter, ResourceLocation spriteLocation, int x, int y, int width, int height, Operation<Void> original) {
		if (!CoreFeature.isLoaded() || !CoreFeature.isGuiBatching() || CoreFeature.shouldByPassGuiBatching()) {
			original.call(guiGraphics, renderTypeGetter, spriteLocation, x, y, width, height);
			return;
		}
		var sprite = guiGraphics.sprites.getSprite(spriteLocation);
		var last   = guiGraphics.pose().last();
		GuiBatchingController.INSTANCE.submitBlit(
			last.pose(), last.normal(), sprite.atlasLocation(),
			x, x + width, y, y + height, 0, -1,
			sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(),
			renderTypeGetter
		);
	}

	@com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(
		method = "renderSlotHighlightFront",
		at = @org.spongepowered.asm.mixin.injection.At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIII)V"
		)
	)
	public void batchHighlightFront(GuiGraphics guiGraphics, Function<ResourceLocation, RenderType> renderTypeGetter, ResourceLocation spriteLocation, int x, int y, int width, int height, Operation<Void> original) {
		if (!CoreFeature.isLoaded() || !CoreFeature.isGuiBatching() || CoreFeature.shouldByPassGuiBatching()) {
			original.call(guiGraphics, renderTypeGetter, spriteLocation, x, y, width, height);
			return;
		}
		var sprite = guiGraphics.sprites.getSprite(spriteLocation);
		var last   = guiGraphics.pose().last();
		GuiBatchingController.INSTANCE.submitBlit(
			last.pose(), last.normal(), sprite.atlasLocation(),
			x, x + width, y, y + height, 0, -1,
			sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(),
			renderTypeGetter
		);
	}
}
