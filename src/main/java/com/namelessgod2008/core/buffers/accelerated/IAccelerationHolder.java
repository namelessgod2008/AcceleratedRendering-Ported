package com.namelessgod2008.core.buffers.accelerated;

import com.namelessgod2008.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;

import java.util.function.Supplier;

public interface IAccelerationHolder {

	VertexConsumer				initAcceleration(RenderType renderType, Supplier<IAcceleratedBufferSource> bufferSource);
	AcceleratedBufferBuilder	getAccelerated	();
}
