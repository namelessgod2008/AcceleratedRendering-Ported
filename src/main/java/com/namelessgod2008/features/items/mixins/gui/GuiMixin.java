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

	@Inject(method = "renderItemHotbar", at = @At("HEAD"))
	public void startBatching(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		GuiBatchingController.INSTANCE.startBatching(guiGraphics);
	}

	@Inject(method = "renderItemHotbar", at = @At("RETURN"))
	public void flushBatching(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		GuiBatchingController.INSTANCE.flushBatching(guiGraphics);
	}
}
