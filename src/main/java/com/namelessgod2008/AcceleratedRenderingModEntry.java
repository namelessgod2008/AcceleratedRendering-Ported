package com.namelessgod2008;

import com.namelessgod2008.configs.FeatureConfig;
import com.namelessgod2008.core.programs.ComputeShaderPrograms;
import com.namelessgod2008.core.utils.AvailabilityUtils;
import com.namelessgod2008.features.culling.OrientationCullingPrograms;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.config.ModConfig;

public class AcceleratedRenderingModEntry implements ClientModInitializer {

    public static final String MOD_ID = "acceleratedrendering";
    @Getter
    private static ModContainer container;

    public static Identifier location(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitializeClient() {
        ConfigRegistry.INSTANCE.register(MOD_ID, ModConfig.Type.CLIENT, FeatureConfig.SPEC);
        container = ModLoader.createModContainer(MOD_ID);
        IEventBus eventBus = container.getModEventBus();
        eventBus.register(ComputeShaderPrograms.class);
        eventBus.register(OrientationCullingPrograms.class);
        conditionalInitialize(container.getModEventBus());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!AvailabilityUtils.isAvailable()) {
                client.player.sendSystemMessage(
                    Component.translatable("acceleratedrendering.component.not_available")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                );
            }
        });
    }

    public void conditionalInitialize(IEventBus modEventBus) {
        //intentionally empty
    }
}
