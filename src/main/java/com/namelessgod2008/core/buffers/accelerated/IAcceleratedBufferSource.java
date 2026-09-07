package com.namelessgod2008.core.buffers.accelerated;

import com.namelessgod2008.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import net.minecraft.client.renderer.RenderType;

public interface IAcceleratedBufferSource {

	AcceleratedBufferBuilder getBuffer(RenderType renderType, Runnable before, Runnable after, int layer);
}
