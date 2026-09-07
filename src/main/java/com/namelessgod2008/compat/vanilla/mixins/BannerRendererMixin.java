package com.namelessgod2008.compat.vanilla.mixins;

import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.features.mods.ModsFeature;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 1.21.4 port of upstream BannerRendererMixin (af6b560).
 *
 * In 1.21.4, renderPatterns was split into a 9-arg wrapper overload and an
 * 11-arg real implementation (appends `false, true`). The renderPatternLayer
 * INVOKEs live in the 11-arg overload, so the `method` descriptor must target
 * that one. The INVOKE target descriptor and ordinal semantics are unchanged.
 *
 * Fixes banner/sophisticated storage rendering order by bumping the default
 * layer around each pattern layer draw (base color uses forceIncrementDefaultLayer,
 * the pattern loop uses forceAddDefaultLayer(index + 1)).
 */
@Mixin(BannerRenderer.class)
public class BannerRendererMixin {

	@WrapOperation(
			method = "renderPatterns(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/resources/model/Material;ZLnet/minecraft/world/item/DyeColor;Lnet/minecraft/world/level/block/entity/BannerPatternLayers;ZZ)V",
			at = @At(
					value	= "INVOKE",
					target	= "Lnet/minecraft/client/renderer/blockentity/BannerRenderer;renderPatternLayer(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/resources/model/Material;Lnet/minecraft/world/item/DyeColor;)V",
					ordinal	= 0
			)
	)
	private static void wrapPatternLayer1(
			PoseStack			poseStack,
			MultiBufferSource	buffer,
			int					packedLight,
			int					packedOverlay,
			ModelPart			flagPart,
			Material			material,
			DyeColor			color,
			Operation<Void>		original
	) {
		var pass = CoreFeature.isLoaded			()
				&& ModsFeature.isEnabled		()
				&& ModsFeature.shouldFixVanilla	();

		if (pass) {
			CoreFeature.forceIncrementDefaultLayer();
		}

		original.call(
				poseStack,
				buffer,
				packedLight,
				packedOverlay,
				flagPart,
				material,
				color
		);

		if (pass) {
			CoreFeature.resetDefaultLayer();
		}
	}

	@WrapOperation(
			method = "renderPatterns(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/resources/model/Material;ZLnet/minecraft/world/item/DyeColor;Lnet/minecraft/world/level/block/entity/BannerPatternLayers;ZZ)V",
			at = @At(
					value	= "INVOKE",
					target	= "Lnet/minecraft/client/renderer/blockentity/BannerRenderer;renderPatternLayer(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/resources/model/Material;Lnet/minecraft/world/item/DyeColor;)V",
					ordinal	= 1
			)
	)
	private static void wrapPatternLayer2(
			PoseStack				poseStack,
			MultiBufferSource		buffer,
			int						packedLight,
			int						packedOverlay,
			ModelPart				flagPart,
			Material				material,
			DyeColor				color,
			Operation<Void>			original,
			@Local(name = "i") int	index
	) {
		var pass = CoreFeature.isLoaded			()
				&& ModsFeature.isEnabled		()
				&& ModsFeature.shouldFixVanilla	();

		if (pass) {
			CoreFeature.forceAddDefaultLayer(index + 1);
		}

		original.call(
				poseStack,
				buffer,
				packedLight,
				packedOverlay,
				flagPart,
				material,
				color
		);

		if (pass) {
			CoreFeature.resetDefaultLayer();
		}
	}
}
