package com.namelessgod2008.compat.tweakeroo.mixins;

import com.namelessgod2008.features.items.gui.GuiBatchingController;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * KNOWN BUG (2026-07-18, root cause fixed 2026-07-21):
 * Tweakeroo Free Camera（灵魂出窍）激活时若同时启用 "GUI物品加速"+"GUI物品合批"，
 * 所有 HUD overlay 文字全局消失。
 *
 * <p><b>根因</b>：AR 的 GuiMixin 在 renderItemHotbar 的 {@code @At("HEAD")}
 * 处调用 startBatching 设置 GUI_BATCHING=true。Tweakeroo Free Camera 的
 * MixinInGameHud_freeCam 在同一点通过 ci.cancel() 取消方法体——但 Mixin 的
 * HEAD 回调全部执行完毕后才会检查取消标志，startBatching 已执行，GUI_BATCHING 已
 * 被设为 true，即使方法体已被跳过无任何渲染发生。</p>
 *
 * <p><b>根因修复</b>（2026-07-21）：GuiMixin.startBatching 注入点从 HEAD 移至
 * renderItemHotbar 方法体内的首个 INVOKE（getCameraPlayer()）之后——
 * {@code @At(value = "INVOKE", target = "...getCameraPlayer...", shift = AFTER)}。
 * 若方法体被取消，getCameraPlayer() 永不调用，注入点永不触发，GUI_BATCHING 永不为
 * true。此修复通用且不依赖 camera entity 检测。</p>
 *
 * <p>本 mixin 保留为安全网，cameraEntity != player 判定已移除。</p>
 */
@Mixin(value = GuiBatchingController.class, remap = false)
public class TweakerooGuiMixin {

	@Inject(method = "startBatching", at = @At("HEAD"), cancellable = true, require = 0)
	private void skipBatchingDuringFreeCamera(GuiGraphics graphics, CallbackInfoReturnable<Boolean> cir) {
		// Root cause fixed in GuiMixin — startBatching injection point moved
		// from HEAD to inside the method body (getCameraPlayer() INVOKE AFTER).
		// If Free Camera cancels the method, the body never executes and
		// startBatching is never called. No camera check needed.
	}
}
