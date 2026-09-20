package com.namelessgod2008.compat.iris.mixins.acceleratedrendering;

import com.namelessgod2008.compat.iris.IrisRenderTypeUnwrapper;
import com.namelessgod2008.core.utils.RenderTypeUtils;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Iris 兼容：在 {@link RenderTypeUtils} 的各入口处把 Iris 的包装 RenderType 解包
 * 成内部真正的 RenderType。
 *
 * <p><b>为什么必须解包</b>：Iris 用 {@code net.irisshaders.iris.layer.OuterWrappedRenderType}
 * 包装部分 RenderType。该类 {@code extends RenderType}，但内部用的是 **{@code FAKE_SETUP}**
 * （伪造的 RenderSetup），真实 setup 在私有字段 {@code wrapped} 里。
 *
 * <p>{@code RenderTypeUtils} 靠反射读 {@code RenderType.state} 来解析纹理/矩阵 ——
 * 不解包就会读到 {@code FAKE_SETUP}，得到空结果。后果不只是贴图错误，而是
 * <b>整个绘制被跳过</b>：{@code BaseVertexDrawContextPool} 在 {@code preparedTextures}
 * 为空时直接 return（否则 {@code RenderPass.bindTexture} 会抛 "Missing sampler"）。
 *
 * <p><b>实际故障（2026-09-14）</b>：箱子/末影箱模型完全消失，碰撞箱仍在。
 * 探针实测：{@code rtClass=OuterWrappedRenderType isIris=true texturesEmpty=true}
 * → {@code collectorVerts=0 meshCls=EmptyMesh} → {@code 跳过绘制}。
 * 未被包装的类型（如羊的 {@code entity_cutout}）不受影响，故表现为「只有部分方块实体消失」。
 *
 * <p><b>覆盖范围</b>：凡从 RenderType 读取 setup 相关信息的公开方法都要解包 ——
 * 尤其是 {@code getTextures}（纹理绑定，箱子消失的直接原因）与 {@code getTextureMatrix}，
 * 这两个是本次新补的（旧版本 mixin 只覆盖了另外 5 个，且引用的
 * {@code WrappableRenderType} 接口在当前 Iris 1.11.4 中已随 {@code batchedentityrendering}
 * 包一起移除 —— 该 mixin 自移植以来未编译过，详见 git 历史）。
 *
 * <p>解包逻辑在 {@link IrisRenderTypeUnwrapper}（反射实现，原因见其类注释）。
 */
@Mixin(RenderTypeUtils.class)
public class RenderTypeUtilsMixin {

	/** Iris 包装 → 内部真实 RenderType；非包装类型原样返回。 */
	private static RenderType acceleratedrendering$unwrap(RenderType renderType) {
		return IrisRenderTypeUnwrapper.unwrap(renderType);
	}

	@ModifyVariable(method = "getTextures", at = @At("HEAD"), ordinal = 0, argsOnly = true)
	private static RenderType unwrapForGetTextures(RenderType renderType) {
		return acceleratedrendering$unwrap(renderType);
	}

	@ModifyVariable(method = "getTextureMatrix", at = @At("HEAD"), ordinal = 0, argsOnly = true)
	private static RenderType unwrapForGetTextureMatrix(RenderType renderType) {
		return acceleratedrendering$unwrap(renderType);
	}

	@ModifyVariable(method = "getTextureLocation", at = @At("HEAD"), ordinal = 0, argsOnly = true)
	private static RenderType unwrapForGetTextureLocation(RenderType renderType) {
		return acceleratedrendering$unwrap(renderType);
	}

	@ModifyVariable(method = "applyLayeringTransform", at = @At("HEAD"), ordinal = 0, argsOnly = true)
	private static RenderType unwrapForApplyLayeringTransform(RenderType renderType) {
		return acceleratedrendering$unwrap(renderType);
	}

	@ModifyVariable(method = "isCulled", at = @At("HEAD"), ordinal = 0, argsOnly = true)
	private static RenderType unwrapForIsCulled(RenderType renderType) {
		return acceleratedrendering$unwrap(renderType);
	}

	@ModifyVariable(method = "isDynamic", at = @At("HEAD"), ordinal = 0, argsOnly = true)
	private static RenderType unwrapForIsDynamic(RenderType renderType) {
		return acceleratedrendering$unwrap(renderType);
	}

	@ModifyVariable(method = "hasDepth", at = @At("HEAD"), ordinal = 0, argsOnly = true)
	private static RenderType unwrapForHasDepth(RenderType renderType) {
		return acceleratedrendering$unwrap(renderType);
	}

	@ModifyVariable(method = "withDepth", at = @At("HEAD"), ordinal = 0, argsOnly = true)
	private static RenderType unwrapForWithDepth(RenderType renderType) {
		return acceleratedrendering$unwrap(renderType);
	}

	@ModifyVariable(method = "getDrawType", at = @At("HEAD"), ordinal = 0, argsOnly = true)
	private static RenderType unwrapForGetDrawType(RenderType renderType) {
		return acceleratedrendering$unwrap(renderType);
	}

	@ModifyVariable(method = "isTranslucent", at = @At("HEAD"), ordinal = 0, argsOnly = true)
	private static RenderType unwrapForIsTranslucent(RenderType renderType) {
		return acceleratedrendering$unwrap(renderType);
	}
}
