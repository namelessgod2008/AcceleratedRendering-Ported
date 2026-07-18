package com.namelessgod2008.features.items.mixins.models;

import com.namelessgod2008.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.namelessgod2008.features.items.IAcceleratedBakedModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.MultiPartBakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Mixin(MultiPartBakedModel.class)
public abstract class MultipartBakedModelMixin implements IAcceleratedBakedModel {

    @Shadow @Final private List<Pair<Predicate<BlockState>, BakedModel>> selectors;
    @Shadow @Final private Map<BlockState, BitSet> selectorCache;

    // 1.21.4: removed @Inject on constructor — lazy evaluation avoids model baking conflicts
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
        if (selectors == null || selectors.isEmpty()) return false;
        for (var pair : selectors) {
            var model = pair.getRight();
            if (!(model instanceof IAcceleratedBakedModel acc) || !check.test(acc)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void renderItemFast(ItemStack itemStack, RandomSource random, PoseStack.Pose pose,
            IAcceleratedVertexConsumer extension, int light, int overlay) {
        // Items not supported for multipart models
    }

    @Override
    public void renderBlockFast(BlockState state, RandomSource random, PoseStack.Pose pose,
            IAcceleratedVertexConsumer extension, int light, int overlay, int color) {
        var bitSet = selectorCache.get(state);
        if (bitSet == null) {
            bitSet = new BitSet();
            for (int i = 0; i < this.selectors.size(); ++i) {
                if (this.selectors.get(i).getLeft().test(state)) {
                    bitSet.set(i);
                }
            }
            this.selectorCache.put(state, bitSet);
        }
        var seed = random.nextLong();

        for (var j = 0; j < bitSet.length(); j++) {
            if (bitSet.get(j)) {
                var model = selectors.get(j).getRight();
                if (model instanceof IAcceleratedBakedModel acc) {
                    acc.renderBlockFast(state, RandomSource.create(seed), pose, extension, light, overlay, getCustomColor(-1, color));
                }
            }
        }
    }

    @Override
    public int getCustomColor(int layer, int color) {
        return color;
    }
}
