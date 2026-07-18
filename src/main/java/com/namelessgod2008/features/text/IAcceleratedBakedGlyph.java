package com.namelessgod2008.features.text;

import com.namelessgod2008.features.text.renderers.AcceleratedBakedGlyphRenderer;

public interface IAcceleratedBakedGlyph {

	AcceleratedBakedGlyphRenderer getRenderer(boolean italic);
}
