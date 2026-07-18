package com.namelessgod2008.core.utils;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public class FuzzyMatrix4f extends Matrix4f {

	public FuzzyMatrix4f(Matrix4f matrix) {
		super(matrix);
	}

	public FuzzyMatrix4f() {
		super();
	}

	@Override
	public int hashCode() {
		return 61;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Matrix4fc matrix && equals(matrix, 1e-2f);
	}
}
