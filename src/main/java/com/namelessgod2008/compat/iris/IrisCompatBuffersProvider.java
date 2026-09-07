package com.namelessgod2008.compat.iris;

import com.namelessgod2008.core.buffers.accelerated.IAcceleratedBufferSource;

import java.util.function.Supplier;

public class IrisCompatBuffersProvider {

	public static final Supplier<IAcceleratedBufferSource> SHADOW	= () -> IrisCompatBuffers.SHADOW;
	public static final Supplier<IAcceleratedBufferSource> HAND		= () -> IrisCompatBuffers.HAND;
}
