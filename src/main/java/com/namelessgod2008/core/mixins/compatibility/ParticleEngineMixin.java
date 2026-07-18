package com.namelessgod2008.core.mixins.compatibility;

import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.features.entities.AcceleratedEntityRenderingFeature;
import com.namelessgod2008.features.items.AcceleratedItemRenderingFeature;
import com.namelessgod2008.features.text.AcceleratedTextRenderingFeature;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

	@Inject(
			method	= "render",
			at		= @At("HEAD")
	)
	public void disableParticleAcceleration(
		Camera camera,
		float partialTick,
		MultiBufferSource.BufferSource bufferSource,
		CallbackInfo ci
	) {
		if (!CoreFeature.isLoaded()) {
			return;
		}

		AcceleratedEntityRenderingFeature	.useVanillaPipeline();
		AcceleratedItemRenderingFeature		.useVanillaPipeline();
		AcceleratedTextRenderingFeature		.useVanillaPipeline();
	}

	@Inject(
		method	= "render",
		at		= @At("RETURN")
	)
	public void resetParticleAcceleration(
		Camera camera,
		float partialTick,
		MultiBufferSource.BufferSource bufferSource,
		CallbackInfo ci
	) {
		if (!CoreFeature.isLoaded()) {
			return;
		}

		AcceleratedEntityRenderingFeature	.resetPipeline();
		AcceleratedItemRenderingFeature		.resetPipeline();
		AcceleratedTextRenderingFeature		.resetPipeline();
	}
}
