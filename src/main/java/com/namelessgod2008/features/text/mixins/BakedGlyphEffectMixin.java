package com.namelessgod2008.features.text.mixins;

import lombok.EqualsAndHashCode;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@EqualsAndHashCode
@Mixin(BakedGlyph.Effect.class)
public class BakedGlyphEffectMixin {

	@Shadow @Final public float x0;
	@Shadow @Final public float x1;
	@Shadow @Final public float y0;
	@Shadow @Final public float y1;
	@Shadow @Final public float depth;
}
