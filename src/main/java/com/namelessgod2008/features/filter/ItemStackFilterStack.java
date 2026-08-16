package com.namelessgod2008.features.filter;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tracks the ItemStack currently being rendered.
 *
 * In 1.21.4 the accelerated item entry point ItemRenderer.renderItem() is a
 * static method without an ItemStack parameter (it only receives a BakedModel
 * and tintLayers). The ItemStack IS available at the higher-level entry points
 * (ItemModelResolver.updateForTopItem for world rendering, and
 * GuiBatchingController.renderItemContexts for batched GUI rendering), so those
 * push onto this stack and renderItem pops it. The filter mixin then decides,
 * based on FilterFeature.testItem, whether to switch the pipeline to vanilla.
 *
 * This replaces the 1.21.1 upstream approach of @WrapMethod on
 * ItemRenderer.render(ItemStack, ...), which does not exist in 1.21.4.
 */
public class ItemStackFilterStack {

	private static final ThreadLocal<Deque<ItemStack>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

	public static void push(ItemStack itemStack) {
		STACK.get().push(itemStack);
	}

	public static ItemStack peek() {
		var stack = STACK.get();
		return stack.isEmpty() ? ItemStack.EMPTY : stack.peek();
	}

	public static void pop() {
		STACK.get().pop();
	}
}