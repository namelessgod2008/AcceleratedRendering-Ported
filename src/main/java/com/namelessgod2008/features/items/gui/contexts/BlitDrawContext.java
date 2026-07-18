package com.namelessgod2008.features.items.gui.contexts;

import com.namelessgod2008.features.items.gui.GuiBatchingController;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public record BlitDrawContext(
		Matrix4f			transform,
		Matrix3f			normal,
		ResourceLocation	atlasLocation,
		int					minX,
		int					maxX,
		int					minY,
		int					maxY,
		int					blitOffset,
		int					blitColor,
		int					blitLight,
		int					blitOverlay,
		float				minU,
		float				maxU,
		float				minV,
		float				maxV,
		java.util.function.Function<ResourceLocation, net.minecraft.client.renderer.RenderType> renderTypeGetter
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
