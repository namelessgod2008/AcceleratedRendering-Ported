package com.namelessgod2008.core.mixins.compatibility;

import com.namelessgod2008.core.CoreFeature;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 26.1: energySwirl/breezeWind 从 RenderType 移到了 RenderTypes 工厂类
@Mixin(RenderTypes.class)
public class RenderTypeMixin {

	@Unique private static final Map<Pair<Identifier, Integer>, RenderType> ENERGY_SWIRL	= new ConcurrentHashMap<>();
	@Unique private static final Map<Pair<Identifier, Integer>, RenderType> BREEZE_WIND	= new ConcurrentHashMap<>();

	@WrapMethod(method = "energySwirl")
	private static RenderType cacheEnergySwirl(
			Identifier		location,
			float					u,
			float					v,
			Operation<RenderType>	original
	) {
		return ENERGY_SWIRL.computeIfAbsent(Pair.of(location, CoreFeature.packDynamicUV(u, v)), pair -> original.call(
				location,
				CoreFeature.unpackDynamicU(pair.getSecond()),
				CoreFeature.unpackDynamicV(pair.getSecond())
		));
	}

	@WrapMethod(method = "breezeWind")
	private static RenderType cacheBreezeWind(
			Identifier		location,
			float					u,
			float					v,
			Operation<RenderType>	original
	) {
		return BREEZE_WIND.computeIfAbsent(Pair.of(location, CoreFeature.packDynamicUV(u, v)), pair -> original.call(
				location,
				CoreFeature.unpackDynamicU(pair.getSecond()),
				CoreFeature.unpackDynamicV(pair.getSecond())
		));
	}
}
