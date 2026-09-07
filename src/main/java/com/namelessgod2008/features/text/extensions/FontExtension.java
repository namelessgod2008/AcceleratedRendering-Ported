package com.namelessgod2008.features.text.extensions;

import com.namelessgod2008.features.text.IAcceleratedFont;
import net.minecraft.client.gui.Font;

public class FontExtension {

	public static IAcceleratedFont getAccelerated(Font in) {
		return (IAcceleratedFont) in;
	}
}
