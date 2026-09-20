package com.namelessgod2008.compat.iris.mixins.acceleratedrendering;

import com.namelessgod2008.compat.iris.IrisCompatBuffers;
import com.namelessgod2008.core.CoreBuffersProvider;
import com.namelessgod2008.core.buffers.AcceleratedBufferSources;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CoreBuffersProvider.class)
public class CoreBuffersProviderMixin {

	@WrapOperation(
			method	= "lambda$static$1",
			at		= @At(
					value	= "FIELD",
					target	= "Lcom/namelessgod2008/core/CoreBuffers;CORE:Lcom/namelessgod2008/core/buffers/AcceleratedBufferSources;",
					opcode	= Opcodes.GETSTATIC
			)
	)
	private static AcceleratedBufferSources redirectMainBuffers(Operation<AcceleratedBufferSources> original) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
			return IrisCompatBuffers.SHADOW;
		}

		if (HandRenderer.INSTANCE.isActive()) {
			return IrisCompatBuffers.HAND;
		}

		return original.call();
	}

	// 原 1.21.4 在此包装 bindAcceleratedBufferSources，用 Iris 的 RenderBuffersExt
	// 在绑定期间临时进入「关卡渲染」状态（beginLevelRendering/endLevelRendering）。
	// Iris 1.11.4 已移除 net.irisshaders.batchedentityrendering 包（含 RenderBuffersExt），
	// 该状态改由 Iris 自己在 net.irisshaders.iris.mixin.MixinLevelRenderer 中调用
	// WorldRenderingPipeline.beginLevelRendering() / finalizeLevelRendering() 维护，
	// 故此处不再需要额外包装。
}
