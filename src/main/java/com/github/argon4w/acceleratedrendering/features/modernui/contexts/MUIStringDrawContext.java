package com.github.argon4w.acceleratedrendering.features.modernui.contexts;

import com.github.argon4w.acceleratedrendering.features.items.gui.contexts.string.IStringDrawContext;
import icyllis.modernui.mc.text.TextLayout;
import net.minecraft.client.renderer.MultiBufferSource;
import com.github.argon4w.acceleratedrendering.core.utils.FastColorCompat;
import org.joml.Matrix4f;

public record MUIStringDrawContext(
		Matrix4f	transform,
		TextLayout	textLayout,
		float		textX,
		float		textTop,
		float		uniformScale,
		int			textColor,
		int			backgroundColor,
		int			preferredMode,
		int			packedLight,
		boolean		isShadow,
		boolean		polygonOffset
) implements IStringDrawContext {

	@Override
	public void drawString(MultiBufferSource buffer) {
		textLayout.drawText(
				transform,
				buffer,
				textX,
				textTop,
				FastColorCompat.ARGB32.red	(textColor),
				FastColorCompat.ARGB32.green	(textColor),
				FastColorCompat.ARGB32.blue	(textColor),
				FastColorCompat.ARGB32.alpha	(textColor),
				isShadow,
				preferredMode,
				polygonOffset,
				uniformScale,
				backgroundColor,
				packedLight
		);
	}
}
