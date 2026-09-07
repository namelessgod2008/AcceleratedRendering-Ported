package com.namelessgod2008.core.backends.programs;

import lombok.Getter;

import static org.lwjgl.opengl.GL46.*;

@Getter
public class ComputeShader {

	private final int shaderHandle;

	public ComputeShader() {
		this.shaderHandle = glCreateShader(GL_COMPUTE_SHADER);
	}

	public void setShaderSource(String source) {
		glShaderSource(shaderHandle, source);
	}

	public void compileShader() {
		glCompileShader(shaderHandle);
	}

	public boolean isCompiled() {
		return glGetShaderi(shaderHandle, GL_COMPILE_STATUS) == GL_TRUE;
	}

	public String getInfoLog() {
		return glGetShaderInfoLog(shaderHandle);
	}

	public void delete() {
		glDeleteShader(shaderHandle);
	}
}
