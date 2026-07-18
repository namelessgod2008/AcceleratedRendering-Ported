package com.namelessgod2008.core.backends.buffers;

public interface IServerBuffer {

	int getBufferHandle ();
	void delete			();
	void bind           (int		target);
	void bindBase       (int		target, int		index);
	void bindRange      (int		target, int		index,	long offset, long size);
	void data			(long		offset, long	size,	long address);
}
