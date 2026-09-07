package com.namelessgod2008.core.programs.culling;

import com.namelessgod2008.core.buffers.accelerated.builders.AcceleratedBufferBuilder;

public interface ICullingProgramDispatcher {

	int		dispatch	(AcceleratedBufferBuilder builder);
	boolean	shouldCull	();
}
