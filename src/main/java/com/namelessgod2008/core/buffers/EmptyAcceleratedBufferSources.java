package com.namelessgod2008.core.buffers;

import com.namelessgod2008.core.buffers.accelerated.IAcceleratedBufferSource;
import com.namelessgod2008.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import net.minecraft.client.renderer.RenderType;

public class EmptyAcceleratedBufferSources implements IAcceleratedBufferSource {

	public static final IAcceleratedBufferSource INSTANCE = new EmptyAcceleratedBufferSources();

	@Override
	public AcceleratedBufferBuilder getBuffer(
			RenderType	renderType,
			Runnable	before,
			Runnable	after,
			int			layer
	) {
		return null;
	}
}
