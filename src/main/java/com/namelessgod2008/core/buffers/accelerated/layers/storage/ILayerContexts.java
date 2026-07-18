package com.namelessgod2008.core.buffers.accelerated.layers.storage;

import com.namelessgod2008.core.buffers.accelerated.draw.pools.IDrawContextPool.IDrawContext;

public interface ILayerContexts extends Iterable<IDrawContext> {

	void	add		(IDrawContext drawContext);
	void	reset	();
	void	prepare	();
	boolean	isEmpty	();
}
