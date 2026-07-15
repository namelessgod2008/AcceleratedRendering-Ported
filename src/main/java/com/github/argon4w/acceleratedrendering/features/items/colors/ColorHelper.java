package com.github.argon4w.acceleratedrendering.features.items.colors;

import com.github.argon4w.acceleratedrendering.features.items.mixins.accessors.BlockColorsAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.IdMapper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

public class ColorHelper {
    public static BlockColor getBlockColorOrDefault(Block block) {
        IdMapper<BlockColor> blockColors = ((BlockColorsAccessor) Minecraft.getInstance().getBlockColors()).getBlockColors();
        int id = BuiltInRegistries.BLOCK.getId(block);
        BlockColor blockColor = blockColors.byId(id);
        if (blockColor == null) return EmptyBlockColor.INSTANCE;
        return blockColor;
    }

    // TODO: ItemColors was removed in 1.21.4. Re-implement item color tinting using TintSource / data components.
    // For now, ItemLayerColors always returns -1 (pass-through no-tint).
}
