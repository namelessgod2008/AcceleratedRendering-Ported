package com.namelessgod2008.core.programs.dispatchers;

import com.namelessgod2008.core.buffers.accelerated.builders.AcceleratedBufferBuilder;

public class EmptyProgramDispatcher implements IPolygonProgramDispatcher {

	public static final EmptyProgramDispatcher INSTANCE = new EmptyProgramDispatcher();

	@Override
	public int dispatch(AcceleratedBufferBuilder builder) {
		return 0;
	}
}
