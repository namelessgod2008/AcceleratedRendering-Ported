package com.namelessgod2008.features.items.mixins;

import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.namelessgod2008.core.utils.DirectionUtils;
import com.namelessgod2008.features.items.AcceleratedItemRenderingFeature;
import com.namelessgod2008.features.items.AcceleratedQuadsRenderer;
import com.namelessgod2008.features.items.BakedModelExtension;
import com.namelessgod2008.features.items.colors.ItemLayerColors;
import com.namelessgod2008.features.items.colors.TintLayerColors;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.experimental.ExtensionMethod;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 1.21.4 ItemRendererMixin — wraps renderModelLists() call from within renderItem().
 * renderModelLists is now private static with (BakedModel, int[] tintLayers, int, int, PoseStack, VertexConsumer).
 */
@ExtensionMethod(value = {VertexConsumerExtension.class, BakedModelExtension.class})
@Mixin(value = {ItemRenderer.class}, priority = 0)
public class ItemRendererMixin {

    @WrapOperation(
        method = "renderItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderModelLists(Lnet/minecraft/client/resources/model/BakedModel;[IIILcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V"
        )
    )
    private static void renderFast(
        BakedModel bakedModel,
        int[] tintLayers,
        int combinedLight,
        int combinedOverlay,
        PoseStack poseStack,
        VertexConsumer buffer,
        Operation<Void> original
    ) {
        var extension = buffer.getAccelerated();

        if (!CoreFeature.isLoaded()
            || !AcceleratedItemRenderingFeature.isEnabled()
            || !AcceleratedItemRenderingFeature.shouldUseAcceleratedPipeline()
            || !CoreFeature.isRenderingLevel()
            || !extension.isAccelerated()
        ) {
            original.call(bakedModel, tintLayers, combinedLight, combinedOverlay, poseStack, buffer);
            return;
        }

        var pose   = poseStack.last();
        var random = RandomSource.create(42L);

        // Use the accelerated model if available (skip if tint layers need applying)
        if (tintLayers == null || tintLayers.length == 0) {
            if (bakedModel instanceof com.namelessgod2008.features.items.IAcceleratedBakedModel accelModel
                && accelModel.isAccelerated()) {
                accelModel.renderItemFast(null, random, pose, extension, combinedLight, combinedOverlay);
                return;
            }
        }

        if (!AcceleratedItemRenderingFeature.shouldBakeMeshForQuad()) {
            original.call(bakedModel, tintLayers, combinedLight, combinedOverlay, poseStack, buffer);
            return;
        }

        var color = new TintLayerColors(tintLayers);

        for (var direction : DirectionUtils.FULL) {
            random.setSeed(42L);
            extension.doRender(
                AcceleratedQuadsRenderer.INSTANCE,
                AcceleratedQuadsRenderer.context(
                    bakedModel.getQuads(null, direction, random),
                    color
                ),
                pose.pose(),
                pose.normal(),
                combinedLight,
                combinedOverlay,
                -1
            );
        }
    }
}
