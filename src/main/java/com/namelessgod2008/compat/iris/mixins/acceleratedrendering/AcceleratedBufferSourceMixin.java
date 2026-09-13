package com.namelessgod2008.compat.iris.mixins.acceleratedrendering;

import com.namelessgod2008.core.buffers.accelerated.AcceleratedBufferSource;
import com.namelessgod2008.core.buffers.accelerated.layers.LayerKey;
import net.irisshaders.batchedentityrendering.impl.WrappableRenderType;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AcceleratedBufferSource.class)
public class AcceleratedBufferSourceMixin {

	@ModifyArg(
			method	= "getBuffer",
			at		= @At(
					value	= "INVOKE",
					target	= "Lcom/namelessgod2008/core/buffers/accelerated/builders/AcceleratedBufferBuilder;<init>(Lcom/namelessgod2008/core/buffers/accelerated/pools/StagingBufferPool$StagingBuffer;Lcom/namelessgod2008/core/buffers/accelerated/pools/StagingBufferPool$StagingBuffer;Lcom/namelessgod2008/core/buffers/accelerated/draw/pools/IElementPool$IElementSegment;Lcom/namelessgod2008/core/buffers/accelerated/AcceleratedRingBuffers$Buffers;Lcom/namelessgod2008/core/buffers/accelerated/layers/functions/ILayerFunction;Lcom/namelessgod2008/core/buffers/accelerated/layers/LayerKey;)V"
			),
			index	= 5
	)
	public LayerKey unwrapIrisRenderType(LayerKey layerKey) {
		return layerKey.renderType() instanceof WrappableRenderType wrapped ? new LayerKey(layerKey.layer(), wrapped.unwrap()) : layerKey;
	}

	@Inject(
			method	= "drawBuffers",
			at		= @At(
					value	= "INVOKE",
					target	= "Lcom/namelessgod2008/core/buffers/accelerated/AcceleratedRingBuffers$Buffers;bindDrawBuffers()V",
					shift	= At.Shift.BEFORE,
					remap 	= false
			),
			remap 	= false
	)
	private void beforeBindDrawBuffers(CallbackInfo ci) {
		if (!ImmediateState.isRenderingLevel) {
			ImmediateState.renderWithExtendedVertexFormat = false;
		}
	}

	@Inject(
			method	= "drawBuffers",
			at		= @At(
					value	= "INVOKE",
					target	= "Lcom/namelessgod2008/core/buffers/accelerated/AcceleratedRingBuffers$Buffers;bindDrawBuffers()V",
					shift	= At.Shift.AFTER,
					remap 	= false
			),
			remap 	= false
	)
	private void afterBindDrawBuffers(CallbackInfo ci) {
		if (!ImmediateState.isRenderingLevel) {
			ImmediateState.renderWithExtendedVertexFormat = true;
		}
	}
}
