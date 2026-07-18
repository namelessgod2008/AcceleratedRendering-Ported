package com.namelessgod2008.core.mixins.compatibility;

import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.core.CoreReloads;
import com.namelessgod2008.core.programs.ComputeShaderProgramLoader;
import com.namelessgod2008.core.backends.DebugOutput;
import com.namelessgod2008.core.utils.TextureUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow
    @Final
    private ReloadableResourceManager resourceManager;

    @Inject(
            method  = "<init>",
            at      = @At("TAIL")
    )
    public void setDebugContext(GameConfig gameConfig, CallbackInfo ci) {
        if (		CoreFeature.isLoaded				()
				&&	CoreFeature.isDebugContextEnabled	()
		) {
            DebugOutput.enable();
        }
    }


    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;updateVsync(Z)V"))
    void onRegisterClientReloadListeners(GameConfig gameConfig, CallbackInfo ci) {
        this.resourceManager.registerReloadListener(ComputeShaderProgramLoader.INSTANCE);
        this.resourceManager.registerReloadListener(new CoreReloads());
    }
}
