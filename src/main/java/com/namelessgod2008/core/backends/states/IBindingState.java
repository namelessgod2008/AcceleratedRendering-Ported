package com.namelessgod2008.core.backends.states;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface IBindingState {

	void record	(GuiGraphicsExtractor graphics);
	void restore();
	void delete	();
}
