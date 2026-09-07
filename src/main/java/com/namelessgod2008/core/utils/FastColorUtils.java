package com.namelessgod2008.core.utils;

public class FastColorUtils {

	/**
	 * In 1.21.4, BakedQuad vertex colors are already stored in ARGB format.
	 * The old 1.21.1 R/B byte-swap is no longer needed.
	 */
	public static int convert(int color) {
		return color;
	}
}
