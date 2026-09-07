package com.namelessgod2008;

import net.fabricmc.loader.api.FabricLoader;

public class FabricUtils {
    public static boolean modExists(String modid) {
        return FabricLoader.getInstance().getModContainer(modid).isPresent();
    }
}
