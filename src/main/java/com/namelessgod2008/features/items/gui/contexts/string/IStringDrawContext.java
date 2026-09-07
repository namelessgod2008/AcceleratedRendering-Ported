package com.namelessgod2008.features.items.gui.contexts.string;

import com.namelessgod2008.features.items.gui.GuiBatchingController;
import com.namelessgod2008.features.items.gui.contexts.IGuiElementContext;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix3f;

public interface IStringDrawContext extends IGuiElementContext {

	Matrix3f NORMAL = new Matrix3f().identity();

	void drawString(MultiBufferSource buffer);

	@Override
	default float thickness() {
		return GuiBatchingController.DELTA;
	}

	@Override
	default Matrix3f normal() {
		return NORMAL;
	}

	@Override
	default float depth() {
		return 0.0f;
	}
}
