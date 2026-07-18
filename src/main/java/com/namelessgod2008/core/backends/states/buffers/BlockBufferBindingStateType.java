package com.namelessgod2008.core.backends.states.buffers;

import com.namelessgod2008.core.backends.states.EmptyBindingState;
import com.namelessgod2008.core.backends.states.IBindingState;
import com.namelessgod2008.core.backends.states.buffers.cache.BlockBufferBindingCacheType;

public enum BlockBufferBindingStateType {

	IGNORED,
	RESTORED;

	public IBindingState create(
			BlockBufferBindingCacheType	cacheType,
			BufferBlockType				blockType,
			int							bindingRange
	) {
		return create(
				this,
				cacheType,
				blockType,
				bindingRange
		);
	}

	public static IBindingState create(
			BlockBufferBindingStateType		stateType,
			BlockBufferBindingCacheType		cacheType,
			BufferBlockType					blockType,
			int								bindingRange
	) {
		return switch (stateType) {
			case IGNORED	-> EmptyBindingState.INSTANCE;
			case RESTORED	-> new SimpleBlockBufferBindingState(
					cacheType,
					bindingRange,
					blockType.getBindingBlock	(),
					blockType.getBufferParam	(),
					blockType.getOffsetParam	(),
					blockType.getSizeParam		()
			);
		};
	}
}
