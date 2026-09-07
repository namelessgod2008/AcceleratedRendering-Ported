package com.namelessgod2008.features.items.mixins.models;

import com.namelessgod2008.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.namelessgod2008.features.items.IAcceleratedBakedModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * 1.21.4: WeightedBakedModel refactored — now extends DelegateBakedModel,
 * list field type changed from List<WeightedEntry.Wrapper<BakedModel>> to SimpleWeightedRandomList<BakedModel>.
 * getRandomValue(RandomSource) returns Optional<BakedModel> directly.
 */
@Mixin(WeightedBakedModel.class)
public class WeightedBakedModelMixin implements IAcceleratedBakedModel {

    @Shadow @Final private SimpleWeightedRandomList<BakedModel> list;

    @Unique private Boolean acceleratedCache = null;
    @Unique private Boolean acceleratedInHandCache = null;
    @Unique private Boolean acceleratedInGuiCache = null;

    @Unique
    @Override
    public boolean isAccelerated() {
        if (acceleratedCache == null) acceleratedCache = checkAll(IAcceleratedBakedModel::isAccelerated);
        return acceleratedCache;
    }

    @Unique
    @Override
    public boolean isAcceleratedInHand() {
        if (acceleratedInHandCache == null) acceleratedInHandCache = checkAll(IAcceleratedBakedModel::isAcceleratedInHand);
        return acceleratedInHandCache;
    }

    @Unique
    @Override
    public boolean isAcceleratedInGui() {
        if (acceleratedInGuiCache == null) acceleratedInGuiCache = checkAll(IAcceleratedBakedModel::isAcceleratedInGui);
        return acceleratedInGuiCache;
    }

    @Unique
    private boolean checkAll(java.util.function.Predicate<IAcceleratedBakedModel> check) {
        if (list == null) return false;
        for (var item : list.unwrap()) {
            var model = item.data();
            if (!(model instanceof IAcceleratedBakedModel acc) || !check.test(acc)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void renderItemFast(ItemStack itemStack, RandomSource random, PoseStack.Pose pose,
            IAcceleratedVertexConsumer extension, int light, int overlay) {
        list.getRandomValue(random).ifPresent(model -> {
            if (model instanceof IAcceleratedBakedModel acc) {
                acc.renderItemFast(itemStack, random, pose, extension, light, overlay);
            }
        });
    }

    @Override
    public void renderBlockFast(BlockState state, RandomSource random, PoseStack.Pose pose,
            IAcceleratedVertexConsumer extension, int light, int overlay, int color) {
        list.getRandomValue(random).ifPresent(model -> {
            if (model instanceof IAcceleratedBakedModel acc) {
                acc.renderBlockFast(state, random, pose, extension, light, overlay, getCustomColor(-1, color));
            }
        });
    }

    @Override
    public int getCustomColor(int layer, int color) {
        return color;
    }
}
