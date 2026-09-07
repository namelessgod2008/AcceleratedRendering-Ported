package com.namelessgod2008.core.buffers.accelerated.layers.storage.empty;

import com.namelessgod2008.core.buffers.accelerated.layers.storage.ILayerContexts;
import com.namelessgod2008.core.buffers.accelerated.draw.pools.IDrawContextPool.IDrawContext;
import com.namelessgod2008.core.utils.EmptyIterator;

import java.util.Iterator;

public class EmptyLayerContexts implements ILayerContexts {

	public static final EmptyLayerContexts INSTANCE = new EmptyLayerContexts();

	@Override
	public void add(IDrawContext drawContext) {

	}

	@Override
	public void reset() {

	}

	@Override
	public void prepare() {

	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public Iterator<IDrawContext> iterator() {
		return EmptyIterator.of();
	}
}
