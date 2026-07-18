package com.namelessgod2008.core.backends;

import static org.lwjgl.opengl.GL46.*;

public class VertexArray {

	private final int vaoHandle;

	public VertexArray() {
		this.vaoHandle = glCreateVertexArrays();
	}

	public void bind() {
		glBindVertexArray(vaoHandle);
	}

	public void unbind() {
		glBindVertexArray(0);
	}

	public void delete() {
		glDeleteVertexArrays(vaoHandle);
	}
}
