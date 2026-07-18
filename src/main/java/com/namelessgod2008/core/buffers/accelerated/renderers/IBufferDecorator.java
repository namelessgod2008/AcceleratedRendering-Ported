package com.namelessgod2008.core.buffers.accelerated.renderers;

import com.mojang.blaze3d.vertex.VertexConsumer;

public interface IBufferDecorator {

	VertexConsumer decorate(VertexConsumer buffer);
}
