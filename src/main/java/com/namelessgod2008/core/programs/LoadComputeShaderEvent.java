package com.namelessgod2008.core.programs;

import com.namelessgod2008.core.backends.programs.BarrierFlags;
import com.google.common.collect.ImmutableMap;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.util.Map;

public class LoadComputeShaderEvent extends Event implements IModBusEvent {

	private final ImmutableMap.Builder<Identifier, ComputeShaderDefinition> shaderLocations;

	public LoadComputeShaderEvent() {
		this.shaderLocations = ImmutableMap.builder();
	}

	public void loadComputeShader(
			Identifier	key,
			Identifier	location,
			BarrierFlags...		barrierFlags
	) {
		shaderLocations.put(key, new ComputeShaderDefinition(location, BarrierFlags.getFlags(barrierFlags)));
	}

	public Map<Identifier, ComputeShaderDefinition> build() {
		return shaderLocations.build();
	}
}
