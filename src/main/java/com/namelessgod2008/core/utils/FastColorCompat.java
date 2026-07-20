package com.namelessgod2008.core.utils;

/**
 * Compatibility layer for {@code net.minecraft.util.FastColor} which was removed in Minecraft 1.21.4.
 * Delegates to {@code net.minecraft.util.ARGB}.
 * <p>
 * IMPORTANT: 上游 {@code FastColor.ARGB32.color(alpha, red, green, blue)} 与 1.21.4 的
 * {@code ARGB.color(alpha, red, green, blue)} 参数顺序相同 — 都是 alpha FIRST。
 * 本类 4 参签名必须保持 alpha FIRST：全项目 9 处调用点均为上游移植代码，全部按 (alpha, r, g, b) 传参。
 */
public class FastColorCompat {

    public static final class ARGB32 {
        /** 与上游 FastColor.ARGB32.color 一致：alpha FIRST */
        public static int color(int alpha, int red, int green, int blue) {
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

        /** 与上游 FastColor.ARGB32.colorFromFloat 一致：alpha FIRST */
        public static int colorFromFloat(float alpha, float red, float green, float blue) {
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
