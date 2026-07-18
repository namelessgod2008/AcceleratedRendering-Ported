package com.namelessgod2008.core.backends;

import static org.lwjgl.opengl.GL46.GL_DEBUG_OUTPUT_SYNCHRONOUS;
import static org.lwjgl.opengl.GL46.glEnable;

public class DebugOutput {

	public static void enable() {
		glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
	}
}
