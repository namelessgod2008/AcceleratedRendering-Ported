package com.namelessgod2008.core.buffers.accelerated.layers.storage.sorted;

import com.namelessgod2008.core.buffers.accelerated.layers.LayerDrawType;
import com.namelessgod2008.core.buffers.accelerated.layers.storage.ILayerContexts;
import com.namelessgod2008.core.buffers.accelerated.layers.storage.ILayerStorage;
import com.namelessgod2008.core.buffers.accelerated.layers.storage.SimpleLayerContexts;

public class SortedLayerStorage implements ILayerStorage {

	private final SimpleLayerContexts contexts;

	public SortedLayerStorage(int size) {
		this.contexts = new SortedLayerContexts(size);
	}

	@Override
	public ILayerContexts get(LayerDrawType type) {
		return contexts;
	}

	@Override
	public void reset() {
		contexts.reset();
	}
}
