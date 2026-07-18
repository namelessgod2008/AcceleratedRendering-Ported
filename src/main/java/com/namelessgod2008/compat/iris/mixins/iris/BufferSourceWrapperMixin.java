package com.namelessgod2008.compat.iris.mixins.iris;

import com.namelessgod2008.core.buffers.accelerated.IAcceleratedBufferSource;
import com.namelessgod2008.core.buffers.accelerated.builders.BufferSourceExtension;
import com.namelessgod2008.core.buffers.accelerated.builders.IAcceleratableBufferSource;
import com.namelessgod2008.core.buffers.accelerated.builders.VertexConsumerExtension;
import lombok.experimental.ExtensionMethod;
import net.irisshaders.iris.layer.BufferSourceWrapper;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Supplier;

@Pseudo
@ExtensionMethod({
		VertexConsumerExtension	.class,
		BufferSourceExtension	.class
})
@Mixin(BufferSourceWrapper.class)
public class BufferSourceWrapperMixin implements IAcceleratableBufferSource {

	@Shadow
	@Final
	private MultiBufferSource bufferSource;

	@Override
	public Supplier<IAcceleratedBufferSource> getBoundAcceleratedBufferSource() {
		return bufferSource.getAcceleratable().getBoundAcceleratedBufferSource();
	}

	@Override
	public boolean isBufferSourceAcceleratable() {
		return bufferSource.getAcceleratable().isBufferSourceAcceleratable();
	}

	@Override
	public void bindAcceleratedBufferSource(Supplier<IAcceleratedBufferSource> bufferSource) {
		this.bufferSource.getAcceleratable().bindAcceleratedBufferSource(bufferSource);
	}
}
