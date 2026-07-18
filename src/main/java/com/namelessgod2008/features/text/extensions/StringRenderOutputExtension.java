package com.namelessgod2008.features.text.extensions;

import com.namelessgod2008.features.text.IAcceleratedStringRenderOutput;
import net.minecraft.client.gui.Font;

public class StringRenderOutputExtension {

	public static IAcceleratedStringRenderOutput getAccelerated(Font.StringRenderOutput in) {
		return (IAcceleratedStringRenderOutput) in;
	}
}
