package com.namelessgod2008.core.utils;

import java.util.function.IntFunction;

public class SimpleCachedArray<T extends SimpleCachedArray.Element> extends SimpleResetPool<T, IntFunction<T>> {

	public SimpleCachedArray(int size, IntFunction<T> initializer) {
		super(size, initializer);
	}

	@Override
	protected T create(IntFunction<T> context, int i) {
		return context.apply(i);
	}

	@Override
	protected void reset(T t) {
		t.reset();
	}

	@Override
	protected void delete(T t) {
		t.delete();
	}

	@Override
	public T fail() {
		expand();
		return get();
	}

	public interface Element {

		void reset	();
		void delete	();
	}
}
