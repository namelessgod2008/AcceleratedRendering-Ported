package com.github.argon4w.acceleratedrendering.features.items.colors;

import net.minecraft.world.item.ItemStack;

public class ItemLayerColors implements ILayerColors {

    private final ItemStack itemStack;

    public ItemLayerColors(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    public int getColor(int layer) {
        // TODO: ItemColors was removed in 1.21.4. Return pass-through (no tint) for now.
        // Re-implement using TintSource / data components in a future update.
        return -1;
    }
}
