package com.namelessgod2008.features.items.colors;

/**
 * 1.21.4 ILayerColors implementation backed by int[] tintLayers array.
 * The tintLayers array maps layer index → pre-computed tint color (ARGB format).
 */
public record TintLayerColors(int[] tintLayers) implements ILayerColors {

    @Override
    public int getColor(int layer) {
        if (tintLayers == null || layer < 0 || layer >= tintLayers.length) {
            return -1;
        }
        return tintLayers[layer];
    }
}
