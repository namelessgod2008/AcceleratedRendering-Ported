package com.namelessgod2008.core.utils;

import com.namelessgod2008.AcceleratedRenderingModEntry;
import net.minecraft.resources.ResourceLocation;

public class ResourceLocationUtils {

	public static ResourceLocation create(String path) {
		return ResourceLocation.fromNamespaceAndPath(AcceleratedRenderingModEntry.MOD_ID, path);
	}
}
