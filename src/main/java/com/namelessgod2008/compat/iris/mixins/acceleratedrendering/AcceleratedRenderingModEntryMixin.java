package com.namelessgod2008.compat.iris.mixins.acceleratedrendering;

import com.namelessgod2008.AcceleratedRenderingModEntry;
import com.namelessgod2008.compat.iris.programs.IrisPrograms;
import net.neoforged.bus.api.IEventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AcceleratedRenderingModEntry.class, remap = false)
public class AcceleratedRenderingModEntryMixin {

    @Inject(method = "conditionalInitialize", at = @At("TAIL"))
    public void registerIrisEvents(
        IEventBus modEventBus,
        CallbackInfo ci
    ) {
        modEventBus.register(IrisPrograms.class);
    }
}
