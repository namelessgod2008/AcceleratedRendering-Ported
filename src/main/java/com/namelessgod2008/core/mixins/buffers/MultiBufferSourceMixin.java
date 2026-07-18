package com.namelessgod2008.core.mixins.buffers;

import com.namelessgod2008.core.buffers.accelerated.IAcceleratedBufferSource;
import com.namelessgod2008.core.buffers.accelerated.builders.IAcceleratableBufferSource;
import net.minecraft.client.renderer.MultiBufferSource;
import org.apache.commons.lang3.function.Suppliers;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.Supplier;

@Mixin(MultiBufferSource.class)
public interface MultiBufferSourceMixin extends IAcceleratableBufferSource {

	@Override
	default Supplier<IAcceleratedBufferSource> getBoundAcceleratedBufferSource() {
		return Suppliers.nul();
	}

	@Override
	default boolean isBufferSourceAcceleratable() {
		return false;
	}

	@Override
	default void bindAcceleratedBufferSource(Supplier<IAcceleratedBufferSource> bufferSource) {

	}
}
