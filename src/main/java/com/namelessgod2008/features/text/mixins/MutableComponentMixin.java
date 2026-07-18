package com.namelessgod2008.features.text.mixins;

import com.namelessgod2008.features.text.cache.ISeekableFormattedText;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MutableComponent.class)
public class MutableComponentMixin implements ISeekableFormattedText {

}
