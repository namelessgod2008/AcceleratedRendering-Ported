package com.namelessgod2008.features.emf.mixins;

import com.namelessgod2008.features.emf.IEMFHideable;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ModelPart.class)
public class ModelPartMixin implements IEMFHideable {

	@Override
	public boolean isHidden() {
		return false;
	}
}
