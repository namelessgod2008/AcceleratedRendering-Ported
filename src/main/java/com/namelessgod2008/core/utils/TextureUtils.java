package com.namelessgod2008.core.utils;

import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;


public class TextureUtils {

	public 	static	final TextureUtils													INSTANCE	= new TextureUtils						();
	private	static	final Object2ObjectLinkedOpenHashMap<Identifier, NativeImage> IMAGE_CACHE	= new Object2ObjectLinkedOpenHashMap<>	();

	public static void reload() {
		IMAGE_CACHE.clear();
	}

	/**
	 * 26.1: AbstractTexture.bind() 与 NativeImage.downloadTexture() 已移除，纹理像素读取
	 * 需改用 GpuDevice command encoder 实现（后续专项）。当前先解析纹理位置但不下载像素，
	 * 返回 null —— CulledMeshCollector 对 null 纹理安全降级（不基于纹理剔除），不影响顶点收集。
	 */
	public static NativeImage downloadTexture(RenderType renderType, int mipmapLevel) {
		return null;
	}
}
