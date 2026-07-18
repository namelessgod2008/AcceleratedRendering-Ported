package com.namelessgod2008.features.modernui.mixins;

import icyllis.modernui.mc.text.TextLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(TextLayout.class)
public interface TextLayoutAccessor {

	@Accessor("mTotalAdvance")
	float getTotalAdvance();
}
