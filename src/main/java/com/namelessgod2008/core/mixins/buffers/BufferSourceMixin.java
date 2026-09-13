package com.namelessgod2008.core.mixins.buffers;

import com.namelessgod2008.core.CoreBuffersProvider;
import com.namelessgod2008.core.buffers.accelerated.IAcceleratedBufferSource;
import com.namelessgod2008.core.buffers.accelerated.builders.IAcceleratableBufferSource;
import com.namelessgod2008.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.namelessgod2008.core.programs.ComputeShaderProgramLoader;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.experimental.ExtensionMethod;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Supplier;

@ExtensionMethod(VertexConsumerExtension		.class)
@Mixin			(MultiBufferSource.BufferSource	.class)
public class BufferSourceMixin implements IAcceleratableBufferSource {

	@Unique private Supplier<IAcceleratedBufferSource> bufferSource = CoreBuffersProvider.EMPTY;

	@Unique
	@Override
	public Supplier<IAcceleratedBufferSource> getBoundAcceleratedBufferSource() {
		return bufferSource;
	}

	@Override
	public boolean isBufferSourceAcceleratable() {
		return bufferSource != CoreBuffersProvider.EMPTY;
	}

	@Unique
	@Override
	public void bindAcceleratedBufferSource(Supplier<IAcceleratedBufferSource> bufferSource) {
		this.bufferSource = bufferSource;
	}

	@ModifyReturnValue(
			method	= "getBuffer",
			at		= @At("RETURN")
	)
	public VertexConsumer initAcceleration(VertexConsumer original, RenderType renderType) {
		if (ComputeShaderProgramLoader.isProgramsLoaded()) {
			return original
				.getHolder			()
				.initAcceleration	(renderType, bufferSource);
		}
		return original;
	}
}
