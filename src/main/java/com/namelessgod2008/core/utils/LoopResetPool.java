package com.namelessgod2008.core.utils;

public abstract class LoopResetPool<T, C> extends SimpleResetPool<T, C> {

	public LoopResetPool(int size, C context) {
		super(size, context);
	}

	@Override
	@SuppressWarnings("unchecked")
	public T get(boolean force) {
		for (var i = 0; i < size; i++) {
			var t = (T) pool[i];

			if (test(t)) {
				init(t);
				return t;
			}
		}

		return fail(force);
	}
}