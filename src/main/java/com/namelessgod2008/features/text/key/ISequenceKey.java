package com.namelessgod2008.features.text.key;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.resources.Identifier;

public interface ISequenceKey {

	IntArrayList		getTexts		();
	Identifier	getFont			();
	float				getAdvance		();
	int					getColor		();
	boolean				hasColor		();
	boolean				isBold			();
	boolean				isItalic		();
	boolean				isStrikethrough	();
	boolean				isUnderlined	();
	boolean				isShadow		();
	boolean				isOutline		();
	ISequenceKey		bake			();
}
