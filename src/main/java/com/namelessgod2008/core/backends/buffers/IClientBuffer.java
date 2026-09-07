package com.namelessgod2008.core.backends.buffers;

public interface IClientBuffer {

	long reserve	(long bytes);
	long reserve	(long bytes, boolean occupied);
	long addressAt	(long position);
}
