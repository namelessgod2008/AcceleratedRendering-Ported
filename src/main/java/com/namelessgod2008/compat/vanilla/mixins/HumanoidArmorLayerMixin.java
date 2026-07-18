package com.namelessgod2008.compat.vanilla.mixins;

import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.features.mods.ModsFeature;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 1.21.4 update — renderArmorPiece now takes ItemStack instead of LivingEntity.
 * renderTrim no longer exists; trim is now handled by EquipmentLayerRenderer.renderLayers.
 */
@Mixin(HumanoidArmorLayer.class)
public class HumanoidArmorLayerMixin {

	@SuppressWarnings("rawtypes")
	@WrapOperation(
		method = "renderArmorPiece",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/entity/layers/EquipmentLayerRenderer;renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
		)
	)
	public void setupTrimLayer(
		EquipmentLayerRenderer		instance,
		EquipmentClientInfo.LayerType	layerType,
		ResourceKey<?>				assetKey,
		Model						model,
		ItemStack					itemStack,
		PoseStack					poseStack,
		MultiBufferSource			bufferSource,
		int							packedLight,
		Operation<Void>				original
	) {
		if (		!CoreFeature.isLoaded			()
				||	!ModsFeature.isEnabled			()
				||	!ModsFeature.shouldFixVanilla	()
		) {
			original.call(
				instance,
				layerType,
				assetKey,
				model,
				itemStack,
				poseStack,
				bufferSource,
				packedLight
			);
			return;
		}

		CoreFeature.forceIncrementDefaultLayer();

		original.call(
			instance,
			layerType,
			assetKey,
			model,
			itemStack,
			poseStack,
			bufferSource,
			packedLight
		);

		CoreFeature.resetDefaultLayer();
	}
}
