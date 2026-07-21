package com.namelessgod2008.features.items.mixins.gui;

import com.namelessgod2008.features.items.gui.GuiBatchingController;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

	// Inject after getCameraPlayer() — the first method call inside
	// renderItemHotbar body. If Tweakeroo Free Camera (or any other mixin)
	// cancels the method at HEAD, the body never executes and this injection
	// never fires, preventing GUI_BATCHING from being set without work.
	@Inject(
		method = "renderItemHotbar",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/Gui;getCameraPlayer()Lnet/minecraft/world/entity/player/Player;",
			shift = At.Shift.AFTER
		)
	)
	public void startBatching(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		GuiBatchingController.INSTANCE.startBatching(guiGraphics);
	}

	@Inject(method = "renderItemHotbar", at = @At("RETURN"))
	public void flushBatching(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		GuiBatchingController.INSTANCE.flushBatching(guiGraphics);
	}
}
