package com.namelessgod2008.features.emf.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import traben.entity_model_features.models.parts.EMFModelPartVanilla;

import java.util.Set;

@Pseudo
@Mixin(EMFModelPartVanilla.class)
public interface EMFModelPartVanillaAccessor {

	@Accessor("hideInTheseStates")
	Set<Integer> getHideInTheseStates();
}
