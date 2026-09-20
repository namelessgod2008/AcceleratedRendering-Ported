package com.namelessgod2008.compat.iris.mixins.acceleratedrendering;

import com.namelessgod2008.core.buffers.accelerated.AcceleratedBufferSource;
import com.namelessgod2008.compat.iris.IrisRenderTypeUnwrapper;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AcceleratedBufferSource.class)
public class AcceleratedBufferSourceMixin {

	/**
	 * 入口解包：把 Iris 的包装 RenderType 换成内部真实类型。
	 *
	 * <p>必须在 {@code getBuffer} 入口解包（而非构造 {@code AcceleratedBufferBuilder} 时），
	 * 因为 {@code AcceleratedBufferSource} 用同一个 {@link LayerKey} 既做
	 * {@code builders} map 的键，又在 {@code prepareBuffers} 绘制时从
	 * {@code layerKey.renderType()} 取 RenderType 传给 {@code setupContext}。
	 * 只在构造参数上解包，map 键仍是包装类型，绘制路径拿到的还是 {@code FAKE_SETUP} ——
	 * 箱子/末影箱依旧消失（解包失效）。
	 *
	 * <p>上游 {@code AcceleratedBufferSources.getBuffer}（唯一调用方是
	 * {@code mixins/buffers/BufferBuilderMixin:112}）在委派进来之前只用
	 * {@code format()} / {@code mode()} 做分派 —— 这两个方法
	 * {@code OuterWrappedRenderType} 本身就委派给内部真实类型，无需额外处理。
	 */
	@ModifyVariable(method = "getBuffer", at = @At("HEAD"), ordinal = 0, argsOnly = true)
	public RenderType unwrapIrisRenderType(RenderType renderType) {
		return IrisRenderTypeUnwrapper.unwrap(renderType);
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
