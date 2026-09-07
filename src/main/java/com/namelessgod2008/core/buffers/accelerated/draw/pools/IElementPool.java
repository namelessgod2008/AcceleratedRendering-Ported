package com.namelessgod2008.core.buffers.accelerated.draw.pools;

import com.namelessgod2008.core.backends.buffers.IServerBuffer;

public interface IElementPool {

	void			reset		();
	void			delete		();
	void			prepare		();
	void			bindBuffer	();
	boolean			isResized	();
	IServerBuffer	getBuffer	();
	IElementSegment get			();

	interface IElementSegment {

		long	getCount	();
		void	setup		();
		void	count		(int count);
	}
}
