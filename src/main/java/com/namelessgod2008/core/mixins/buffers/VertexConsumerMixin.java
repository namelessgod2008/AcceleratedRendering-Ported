package com.namelessgod2008.core.mixins.buffers;

import com.namelessgod2008.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(VertexConsumer.class)
public interface VertexConsumerMixin extends IAcceleratedVertexConsumer {

	@Unique
	@Override
	default boolean isAccelerated() {
		return false;
	}
}
