package com.namelessgod2008.core.backends.states;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public class EmptyBindingState implements IBindingState {

	public static final IBindingState INSTANCE = new EmptyBindingState();

	@Override
	public void record(GuiGraphicsExtractor graphics) {

	}

	@Override
	public void restore() {

	}

	@Override
	public void delete() {

	}
}
