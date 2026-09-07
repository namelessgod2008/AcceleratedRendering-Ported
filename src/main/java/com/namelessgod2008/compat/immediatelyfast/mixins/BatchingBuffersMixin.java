package com.namelessgod2008.compat.immediatelyfast.mixins;

import com.namelessgod2008.core.CoreBuffersProvider;
import com.namelessgod2008.core.buffers.accelerated.builders.BufferSourceExtension;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import lombok.experimental.ExtensionMethod;
import net.minecraft.client.renderer.MultiBufferSource;
import net.raphimc.immediatelyfast.feature.batching.BatchingBuffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@ExtensionMethod(BufferSourceExtension	.class)
@Mixin			(BatchingBuffers		.class)
public class BatchingBuffersMixin {

	@ModifyReturnValue(
			method	= "getNonBatchingEntityVertexConsumers",
			at		= @At("RETURN")
	)
	private static MultiBufferSource.BufferSource bindAcceleratableBufferSourceCore1(MultiBufferSource.BufferSource original) {
		original
				.getAcceleratable			()
				.bindAcceleratedBufferSource(CoreBuffersProvider.CORE);

		return original;
	}

	@ModifyReturnValue(
			method	= "getHudBatchingVertexConsumers",
			at		= @At("RETURN")
	)
	private static MultiBufferSource.BufferSource bindAcceleratableBufferSourceCore2(MultiBufferSource.BufferSource original) {
		original
				.getAcceleratable			()
				.bindAcceleratedBufferSource(CoreBuffersProvider.CORE);

		return original;
	}
}
