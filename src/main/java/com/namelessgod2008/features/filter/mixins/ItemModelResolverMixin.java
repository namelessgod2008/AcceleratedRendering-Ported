package com.namelessgod2008.features.filter.mixins;

import com.namelessgod2008.features.filter.ItemStackFilterStack;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 1.21.4 item filter support.
 *
 * In 1.21.4, ItemStack is resolved into an ItemStackRenderState by
 * ItemModelResolver BEFORE the actual render happens (ItemRenderer.renderItem
 * is static and has no ItemStack parameter). This mixin pushes the ItemStack
 * onto {@link ItemStackFilterStack} around updateForTopItem/updateForNonLiving
 * so the filter mixin at ItemRenderer.renderItem HEAD can decide whether to
 * switch the pipeline back to vanilla.
 *
 * Replaces the 1.21.1 upstream @WrapMethod on ItemRenderer.render(ItemStack,...).
 */
@Mixin(ItemModelResolver.class)
public class ItemModelResolverMixin {

	@WrapMethod(method = "updateForTopItem")
	public void wrapUpdateForTopItem(
			ItemStackRenderState		state,
			ItemStack					stack,
			ItemDisplayContext			displayContext,
			boolean						leftHand,
			Level						level,
			LivingEntity				entity,
			int							seed,
			Operation<Void>				original
	) {
		ItemStackFilterStack.push(stack);
		try {
			original.call(state, stack, displayContext, leftHand, level, entity, seed);
		} finally {
			ItemStackFilterStack.pop();
		}
	}

	@WrapMethod(method = "updateForNonLiving")
	public void wrapUpdateForNonLiving(
			ItemStackRenderState		state,
			ItemStack					stack,
			ItemDisplayContext			displayContext,
			Entity						entity,
			Operation<Void>				original
	) {
		ItemStackFilterStack.push(stack);
		try {
			original.call(state, stack, displayContext, entity);
		} finally {
			ItemStackFilterStack.pop();
		}
	}
}