package com.namelessgod2008.features.text.mixins;

import com.namelessgod2008.core.CoreReloads;
import com.namelessgod2008.features.text.extensions.FontExtension;
import com.namelessgod2008.features.text.renderers.AcceleratedSequenceEffectRenderer;
import com.namelessgod2008.features.text.renderers.AcceleratedStyledSequenceRenderer;
import lombok.experimental.ExtensionMethod;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ExtensionMethod(FontExtension	.class)
@Mixin			(CoreReloads	.class)
public class CoreReloadsMixin {

	@Inject(
			method	= "onResourceManagerReload",
			at		= @At("TAIL")
	)
	public void onReload(ResourceManager resourceManager, CallbackInfo ci) {
		AcceleratedStyledSequenceRenderer.INSTANCE		.reload();
		AcceleratedSequenceEffectRenderer.INSTANCE		.reload();
		Minecraft.getInstance().font.getAccelerated()	.reload();
	}
}
