package com.namelessgod2008;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

/**
 * 26.1: 改用 forgeconfigapiport 26.1.5 自带的 NeoForge 配置界面 ConfigurationScreen。
 *
 * mod 原本自带的 net.neoforged.neoforge.client.gui.ConfigScreen（1.21.4 版，1404 行）
 * 依赖 26.1 已移除的 GuiGraphics / RealmsMainScreen / QuadFunction，无法编译；
 * 而 forgeconfigapiport 26.1.5 已提供适配 26.1 的官方实现，可直接从 ModConfigSpec 生成界面。
 *
 * 构造签名：ConfigurationScreen(String modId, Screen parent)
 */
public class ARModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new ConfigurationScreen(AcceleratedRenderingModEntry.MOD_ID, parent);
    }
}