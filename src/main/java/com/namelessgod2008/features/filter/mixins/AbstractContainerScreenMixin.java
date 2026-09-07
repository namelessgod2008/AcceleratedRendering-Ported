package com.namelessgod2008.features.filter.mixins;

import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.features.entities.AcceleratedEntityRenderingFeature;
import com.namelessgod2008.features.filter.FilterFeature;
import com.namelessgod2008.features.items.AcceleratedItemRenderingFeature;
import com.namelessgod2008.features.text.AcceleratedTextRenderingFeature;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

	@Shadow @Final protected AbstractContainerMenu menu;

	@WrapMethod(method = "render")
	public void startBatching(
			GuiGraphics		guiGraphics,
			int				mouseX,
			int				mouseY,
			float			partialTick,
			Operation<Void>	original
	) {
		var pass =	!	CoreFeature		.isLoaded			()
				||	!	FilterFeature	.isEnabled			()
				||	!	FilterFeature	.shouldFilterMenus	()
				||		FilterFeature	.testMenu			(menu);

		if (!pass) {
			AcceleratedEntityRenderingFeature	.useVanillaPipeline();
			AcceleratedItemRenderingFeature		.useVanillaPipeline();
			AcceleratedTextRenderingFeature		.useVanillaPipeline();
		}

		original.call(
				guiGraphics,
				mouseX,
				mouseY,
				partialTick
		);

		if (!pass) {
			AcceleratedEntityRenderingFeature	.resetPipeline();
			AcceleratedItemRenderingFeature		.resetPipeline();
			AcceleratedTextRenderingFeature		.resetPipeline();
		}
	}
}
