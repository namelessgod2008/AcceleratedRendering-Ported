package com.namelessgod2008.core.buffers.accelerated.layers.storage;

import com.namelessgod2008.core.buffers.accelerated.layers.LayerDrawType;

public interface ILayerStorage {

	ILayerContexts	get		(LayerDrawType type);
	void			reset	();
}
