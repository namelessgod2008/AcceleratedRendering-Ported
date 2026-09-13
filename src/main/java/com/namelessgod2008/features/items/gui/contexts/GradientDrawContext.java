package com.namelessgod2008.features.items.gui.contexts;

import com.namelessgod2008.features.items.gui.GuiBatchingController;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public record GradientDrawContext(
		Matrix4f	transform,
		Matrix3f	normal,
		RenderType	renderType,
		int			minX,
		int			minY,
		int			maxX,
		int			maxY,
		int			blitOffset,
		int			colorFrom,
		int			colorTo,
		int			light,
		int			overlay
) implements IGuiElementContext {

	@Override
	public float depth() {
		return blitOffset;
	}

	@Override
	public float thickness() {
		return GuiBatchingController.DELTA;
	}
}
