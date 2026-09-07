package com.namelessgod2008.features.items.contexts;

import com.namelessgod2008.features.items.colors.ILayerColors;
import net.minecraft.util.RandomSource;

public record AcceleratedModelRenderContext(RandomSource randomSource, ILayerColors layerColors) {

}
