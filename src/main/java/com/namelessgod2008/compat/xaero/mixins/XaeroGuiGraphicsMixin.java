package com.namelessgod2008.compat.xaero.mixins;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Xaero's Minimap / World Map 在 Tweakeroo 灵魂出窍关闭瞬间传入 null 的 RenderType
 * 到 GuiGraphics.fill(RenderType, ...) → 经 AR {@code @WrapMethod} 直接转发到
 * submitFill/submitGradient → FillDrawContext 存入 null renderType。
 * <p>
 * 前几次尝试在 submitFill 入口拦截均未生效（可能 @Inject/@ModifyVariable 与
 * 调用链路不兼容）。本版本改为在 {@code MultiBufferSource$BufferSource.getBuffer}
 * 调用处修复 null → {@code RenderType.gui()}，直接防住 NPE。
 */
@Mixin(value = MultiBufferSource.BufferSource.class)
public class XaeroGuiGraphicsMixin {

	@ModifyVariable(
			method = "getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
			at     = @At("HEAD"),
			argsOnly = true,
			index  = 1  // slot 0=this, 1=renderType
	)
	private RenderType fixNullRenderType(RenderType renderType) {
		return renderType != null ? renderType : RenderType.gui();
	}
}
