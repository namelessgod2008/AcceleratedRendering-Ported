package com.github.argon4w.acceleratedrendering.core.utils;

/**
 * Compatibility layer for {@code net.minecraft.util.FastColor} which was removed in Minecraft 1.21.4.
 * Delegates to {@code net.minecraft.util.ARGB} with correct parameter ordering.
 * <p>
 * IMPORTANT: {@code ARGB.color(a, r, g, b)} takes alpha FIRST, unlike
 * {@code FastColor.ARGB32.color(r, g, b, a)} which took alpha LAST.
 */
public class FastColorCompat {

    public static final class ARGB32 {
        /** ARGB.color(alpha, red, green, blue) — alpha first in 1.21.4 */
        public static int color(int red, int green, int blue, int alpha) {
            return net.minecraft.util.ARGB.color(alpha, red, green, blue);
        }

        public static int color(int red, int green, int blue) {
            return net.minecraft.util.ARGB.color(255, red, green, blue);
        }

        public static int red(int color) {
            return net.minecraft.util.ARGB.red(color);
        }

        public static int green(int color) {
            return net.minecraft.util.ARGB.green(color);
        }

        public static int blue(int color) {
            return net.minecraft.util.ARGB.blue(color);
        }

        public static int alpha(int color) {
            return net.minecraft.util.ARGB.alpha(color);
        }

        public static int color(int alpha, int packedColor) {
            return net.minecraft.util.ARGB.color(
                alpha,
                net.minecraft.util.ARGB.red(packedColor),
                net.minecraft.util.ARGB.green(packedColor),
                net.minecraft.util.ARGB.blue(packedColor)
            );
        }

        public static int colorFromFloat(float red, float green, float blue, float alpha) {
            return net.minecraft.util.ARGB.colorFromFloat(alpha, red, green, blue);
        }
    }

    /**
     * ABGR32 format (used for GPU buffer uploads).
     * In 1.21.4 there is no direct replacement — use manual conversion.
     */
    public static final class ABGR32 {
        public static int alpha(int abgr) {
            return (abgr >>> 24) & 0xFF;
        }

        /** Convert ARGB int to ABGR int (swap red and blue channels). */
        public static int fromArgb32(int argb) {
            return (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16);
        }
    }
}
