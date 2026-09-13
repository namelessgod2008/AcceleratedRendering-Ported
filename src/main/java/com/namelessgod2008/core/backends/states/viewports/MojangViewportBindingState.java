package com.namelessgod2008.core.backends.states.viewports;

import com.namelessgod2008.core.backends.states.IBindingState;
import com.mojang.blaze3d.opengl.GlStateManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;

public class MojangViewportBindingState implements IBindingState {

	private int viewportX;
	private int viewportY;
	private int viewportWidth;
	private int viewportHeight;

	private boolean recorded;

	public MojangViewportBindingState() {
		this.viewportX		= 0;
		this.viewportY		= 0;
		this.viewportWidth	= 0;
		this.viewportHeight	= 0;
		this.recorded		= false;
	}

	@Override
	public void record(GuiGraphicsExtractor graphics) {
		// 26.1: GlStateManager.Viewport 嵌套类已移除，改用 LWJGL 读取当前 viewport
		try (MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer viewport = stack.callocInt(4);
			glGetIntegerv(GL_VIEWPORT, viewport);
			viewportX		= viewport.get(0);
			viewportY		= viewport.get(1);
			viewportWidth	= viewport.get(2);
			viewportHeight	= viewport.get(3);
		}
		recorded = true;
	}

	@Override
	public void restore() {
		if (!recorded) {
			return;
		}
		GlStateManager._viewport(
				viewportX,
				viewportY,
				viewportWidth,
				viewportHeight
		);
		recorded = false;
	}

	@Override
	public void delete() {

	}
}
