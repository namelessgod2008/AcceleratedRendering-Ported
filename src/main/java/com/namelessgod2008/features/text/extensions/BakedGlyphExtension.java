package com.namelessgod2008.features.text.extensions;

import com.namelessgod2008.features.text.IAcceleratedBakedGlyph;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;

public class BakedGlyphExtension {

	public static IAcceleratedBakedGlyph getAccelerated(BakedGlyph in) {
		return (IAcceleratedBakedGlyph) in;
	}
}
