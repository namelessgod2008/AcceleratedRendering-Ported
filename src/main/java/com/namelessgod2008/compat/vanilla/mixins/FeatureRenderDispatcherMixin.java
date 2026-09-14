package com.namelessgod2008.compat.vanilla.mixins;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.features.mods.ModsFeature;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.1 原版渲染修复（对应 1.21.4 的 HumanoidArmorLayerMixin / LivingEntityRendererMixin）。
 *
 * <p>26.1 的实体渲染改为「提交 + 延后渲染」两段式：
 * <ol>
 *   <li>提交段 —— {@code LivingEntityRenderer.submit} → {@code RenderLayer.submit} →
 *       {@code EquipmentLayerRenderer.renderLayers}，此阶段<b>只往 SubmitNodeCollector 记录</b>，不写顶点；</li>
 *   <li>渲染段 —— {@link FeatureRenderDispatcher} 按 order 桶（{@code Int2ObjectAVLTreeMap}，升序）
 *       依次取出 {@code SubmitNodeCollection}，再交给各 FeatureRenderer 写顶点。</li>
 * </ol>
 * 因此 1.21.4 那两个「包住 renderLayers / RenderLayer.render 调用」的注入点在 26.1 完全失效
 * ——在提交段 push defaultLayer，读它的时候（{@code ModelPart.compile} → {@code getAccelerated()}）
 * 栈早已弹回 0。
 *
 * <p>26.1 原版已经把盔甲纹饰/渲染层的顺序内建为 order：身体 order 0、盔甲 1/2、
 * 纹饰 3（{@code EquipmentLayerRenderer} 内部 {@code nextOrder++}）。但加速路径把这些几何全部
 * 归到 layer 0，而同一层内是按 {@code HashMap<RenderType, …>} 的任意顺序批绘的，
 * 顺序就此丢失——这正是本修复要解决的问题。
 *
 * <p>做法：把「原版 order 桶」映射为「加速 layer」。{@code AcceleratedBufferSource.activeLayers}
 * 是 {@code IntAVLTreeSet}，按升序逐层绘制，于是原版的 order 顺序在加速路径中得以还原。
 * 只需相对顺序，故直接用桶的迭代序号计数（与 1.21.4 {@code LivingEntityRendererMixin}
 * 的 {@code layer++} 同构）。
 *
 * <p>注：粒子（{@code renderTranslucentParticles}）不走 {@code MultiBufferSource}，
 * 与 layer 无关，故不处理。
 */
@Mixin(FeatureRenderDispatcher.class)
public class FeatureRenderDispatcherMixin {

	@Inject(
		method	= "renderSolidFeatures",
		at		= @At(
			value	= "INVOKE",
			target	= "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer;renderSolid(Lnet/minecraft/client/renderer/SubmitNodeCollection;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V"
		)
	)
	private void pushSolidLayer(
		CallbackInfo					ci,
		@Share("layer") LocalIntRef		layer
	) {
		pushOrderLayer(layer);
	}

	@Inject(method = "renderSolidFeatures", at = @At("RETURN"))
	private void popSolidLayer(
		CallbackInfo					ci,
		@Share("layer") LocalIntRef		layer
	) {
		popOrderLayer(layer);
	}

	@Inject(
		method	= "renderTranslucentFeatures",
		at		= @At(
			value	= "INVOKE",
			target	= "Lnet/minecraft/client/renderer/feature/ShadowFeatureRenderer;renderTranslucent(Lnet/minecraft/client/renderer/SubmitNodeCollection;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V"
		)
	)
	private void pushTranslucentLayer(
		CallbackInfo					ci,
		@Share("layer") LocalIntRef		layer
	) {
		pushOrderLayer(layer);
	}

	@Inject(method = "renderTranslucentFeatures", at = @At("RETURN"))
	private void popTranslucentLayer(
		CallbackInfo					ci,
		@Share("layer") LocalIntRef		layer
	) {
		popOrderLayer(layer);
	}

	/**
	 * 进入下一个 order 桶：先弹掉上一个桶压入的层，再压入新的层号。
	 * 每轮循环调用一次，故此处栈内始终只有本方法压入的那一项。
	 */
	private void pushOrderLayer(LocalIntRef layer) {
		if (!shouldFixVanilla()) {
			return;
		}

		if (layer.get() > 0) {
			CoreFeature.resetDefaultLayer();
		}

		CoreFeature.forceSetDefaultLayer(layer.get());
		layer.set(layer.get() + 1);
	}

	/** 循环结束：弹掉最后一个桶压入的层，使 defaultLayer 栈回到基线。 */
	private void popOrderLayer(LocalIntRef layer) {
		if (!shouldFixVanilla()) {
			return;
		}

		if (layer.get() > 0) {
			CoreFeature.resetDefaultLayer();
		}
	}

	private boolean shouldFixVanilla() {
		return		CoreFeature	.isLoaded			()
				&&	ModsFeature	.isEnabled			()
				&&	ModsFeature	.shouldFixVanilla	();
	}
}
