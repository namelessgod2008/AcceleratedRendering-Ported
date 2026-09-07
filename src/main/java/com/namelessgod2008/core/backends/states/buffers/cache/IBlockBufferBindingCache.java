package com.namelessgod2008.core.backends.states.buffers.cache;

public interface IBlockBufferBindingCache {

	void	delete		();
	void	setup		(int bindingPoint, int buffer, long offset, long size);
	int		getBuffer	(int bindingPoint);
	long	getOffset	(int bindingPoint);
	long	getSize		(int bindingPoint);
}
