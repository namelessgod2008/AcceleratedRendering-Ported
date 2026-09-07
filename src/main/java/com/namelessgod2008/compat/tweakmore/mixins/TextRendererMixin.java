package com.namelessgod2008.compat.tweakmore.mixins;

import com.namelessgod2008.features.text.AcceleratedTextRenderingFeature;
import me.fallenbreath.tweakermore.util.render.TextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TextRenderer.class, remap = false)
public class TextRendererMixin {
    @Inject(
        method = "render",
        at = @At("HEAD")
    )
    void onStartRender(CallbackInfo ci) {
        AcceleratedTextRenderingFeature.useVanillaPipeline();
    }

    @Inject(
        method = "render",
        at = @At("RETURN")
    )
    void onEndRender(CallbackInfo ci) {
        AcceleratedTextRenderingFeature.resetPipeline();
    }
}
