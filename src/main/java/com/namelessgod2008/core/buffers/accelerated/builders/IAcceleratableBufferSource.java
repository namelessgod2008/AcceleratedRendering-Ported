package com.namelessgod2008.core.buffers.accelerated.builders;

import com.namelessgod2008.core.buffers.accelerated.IAcceleratedBufferSource;

import java.util.function.Supplier;

public interface IAcceleratableBufferSource {

	Supplier<IAcceleratedBufferSource>	getBoundAcceleratedBufferSource	();
	boolean								isBufferSourceAcceleratable		();
	void								bindAcceleratedBufferSource		(Supplier<IAcceleratedBufferSource> bufferSource);
}
