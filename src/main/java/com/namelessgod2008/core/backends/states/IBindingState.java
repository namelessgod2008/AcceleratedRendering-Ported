package com.namelessgod2008.core.backends.states;

import net.minecraft.client.gui.GuiGraphics;

public interface IBindingState {

	void record	(GuiGraphics graphics);
	void restore();
	void delete	();
}
