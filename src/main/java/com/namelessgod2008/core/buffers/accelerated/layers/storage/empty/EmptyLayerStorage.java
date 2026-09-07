package com.namelessgod2008.core.buffers.accelerated.layers.storage.empty;

import com.namelessgod2008.core.buffers.accelerated.layers.LayerDrawType;
import com.namelessgod2008.core.buffers.accelerated.layers.storage.ILayerContexts;
import com.namelessgod2008.core.buffers.accelerated.layers.storage.ILayerStorage;

public class EmptyLayerStorage implements ILayerStorage {

	public static final EmptyLayerStorage INSTANCE = new EmptyLayerStorage();

	@Override
	public ILayerContexts get(LayerDrawType type) {
		return EmptyLayerContexts.INSTANCE;
	}

	@Override
	public void reset() {

	}
}
