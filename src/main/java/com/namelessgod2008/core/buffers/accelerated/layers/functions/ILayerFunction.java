package com.namelessgod2008.core.buffers.accelerated.layers.functions;

public interface ILayerFunction {

	void addBefore	(Runnable before);
	void addAfter	(Runnable after);
	void runBefore	();
	void runAfter	();
	void reset		();
}
