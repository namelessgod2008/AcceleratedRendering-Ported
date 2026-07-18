package com.namelessgod2008.core.buffers.accelerated.layers.storage.sorted;

import com.namelessgod2008.core.buffers.accelerated.layers.storage.SimpleLayerContexts;

import java.util.Comparator;

public class SortedLayerContexts extends SimpleLayerContexts {

	public SortedLayerContexts(int size) {
		super(size);
	}

	@Override
	public void prepare() {
		contexts.sort(Comparator.naturalOrder());
	}
}
