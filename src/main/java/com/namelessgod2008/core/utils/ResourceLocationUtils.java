package com.namelessgod2008.core.utils;

import com.namelessgod2008.AcceleratedRenderingModEntry;
import net.minecraft.resources.Identifier;

public class ResourceLocationUtils {

	public static Identifier create(String path) {
		return Identifier.fromNamespaceAndPath(AcceleratedRenderingModEntry.MOD_ID, path);
	}
}
