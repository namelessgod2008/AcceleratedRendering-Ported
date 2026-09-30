package com.namelessgod2008.features.items.mixins;

import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.namelessgod2008.features.entities.AcceleratedEntityRenderingFeature;
import com.namelessgod2008.features.items.AcceleratedItemRenderingFeature;
import com.namelessgod2008.features.items.AcceleratedQuadsRenderer;
import com.namelessgod2008.features.items.colors.TintLayerColors;
import lombok.experimental.ExtensionMethod;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.1 物品加速（对应 1.21.4 的 {@code ItemRendererMixin}）。
 *
 * <p>1.21.4 注入 {@code ItemRenderer.renderItem} 的 HEAD 并 cancel；26.1 **没有该类**，
 * 物品渲染改为与实体同构的「提交-渲染两段式」：
 * <ul>
 *   <li>提交段：{@code ItemStackRenderState.submit} → {@code LayerRenderState.submit}，
 *       只往 {@code SubmitNodeCollector} 记录 {@code ItemSubmit}（含 pose/quads/tintLayers/foilType），
 *       不写顶点；</li>
 *   <li>渲染段：{@link ItemFeatureRenderer#renderSolid} / {@link ItemFeatureRenderer#renderTranslucent}
 *       → {@code renderItem}，此时才 {@code putBakedQuad} 写顶点。</li>
 * </ul>
 * 故注入点落在渲染段的 {@code renderItem} —— 与 {@code ShadowFeatureRendererMixin} 同构。
 *
 * <p><b>不加速的情形</b>：
 * <ul>
 *   <li>{@code FoilType != NONE}（附魔光效）：需要 {@code VertexMultiConsumer} 双写，
 *       加速管线不支持，交还原版；</li>
 *   <li>{@code outlineColor != 0}（发光实体轮廓）：需要额外写入 outline 缓冲，同上。</li>
 * </ul>
 */
@ExtensionMethod(VertexConsumerExtension.class)
@Mixin		(
		value		= ItemFeatureRenderer.class,
		priority	= 999
)
public class ItemFeatureRendererMixin {

	@Inject(
			method		= "renderItem",
			at			= @At("HEAD"),
			cancellable	= true
	)
	private void accelerateItem(
			MultiBufferSource.BufferSource	bufferSource,
			OutlineBufferSource				outlineBufferSource,
			SubmitNodeStorage.ItemSubmit	submit,
			CallbackInfo					ci
	) {
		com.namelessgod2008.core.AccelStats.ITEM_CALLS ++;

		// [AR-ITEMAUDIT]
		final long tGate = System.nanoTime();

		if (		!CoreFeature						.isLoaded						()
				||	!CoreFeature						.isRenderingLevel				()
				||	!AcceleratedEntityRenderingFeature	.isEnabled						()
				||	!AcceleratedEntityRenderingFeature	.shouldUseAcceleratedPipeline	()
				||	!AcceleratedItemRenderingFeature	.isEnabled						()
		) {
			// [AR-PROBE-ITEM]
			com.namelessgod2008.core.AccelStats.itemProbe("GATE",
					"loaded=" + CoreFeature.isLoaded()
					+ " lvl=" + CoreFeature.isRenderingLevel()
					+ " ent=" + AcceleratedEntityRenderingFeature.isEnabled()
					+ " entPipe=" + AcceleratedEntityRenderingFeature.shouldUseAcceleratedPipeline()
					+ " item=" + AcceleratedItemRenderingFeature.isEnabled());
			return;
		}

		// 附魔光效需 VertexMultiConsumer 双写，加速管线不支持
		if (submit.foilType() != ItemStackRenderState.FoilType.NONE) {
			com.namelessgod2008.core.AccelStats.itemProbe("FOIL", String.valueOf(submit.foilType()));
			return;
		}

		// 发光轮廓需额外写 outline 缓冲，同样不在加速范围内
		if (submit.outlineColor() != 0) {
			com.namelessgod2008.core.AccelStats.itemProbe("OUTLINE", String.valueOf(submit.outlineColor()));
			return;
		}

		com.namelessgod2008.core.AccelStats.ITEM_GATE_NANOS += System.nanoTime() - tGate;

		var quads = submit.quads();

		if (quads.isEmpty()) {
			com.namelessgod2008.core.AccelStats.itemProbe("EMPTY", "");
			return;
		}

		// [AR-ITEMAUDIT]
		final long tGroup = System.nanoTime();

		// 26.1 的 BakedQuad 自带 MaterialInfo（含 RenderType），一个 ItemSubmit 的 quads
		// 可能分属多个 RenderType。必须按 RenderType 分组后分别交给各自的加速缓冲，
		// 否则不同 RenderType 的几何会写进同一 layer，导致贴图错乱。
		// 只有全部 quad 的 RenderType 都能加速时才接管整个 submit。
		var byRenderType = new java.util.LinkedHashMap<RenderType, java.util.List<BakedQuad>>();

		for (var quad : quads) {
			byRenderType.computeIfAbsent(quad.materialInfo().itemRenderType(), ignored -> new java.util.ArrayList<>())
					.add(quad);
		}

		com.namelessgod2008.core.AccelStats.ITEM_GROUP_NANOS += System.nanoTime() - tGroup;

		// [AR-ITEMAUDIT]
		final long tFmt = System.nanoTime();

		for (var renderType : byRenderType.keySet()) {
			if (!bufferSource.getBuffer(renderType).getAccelerated().isAccelerated()) {
				// [AR-PROBE-ITEM] 格式守卫失败 —— 物品的 RenderType 未被加速源接受
				com.namelessgod2008.core.AccelStats.itemProbe("FMT",
						"rt=" + renderType + " fmt=" + renderType.format().getVertexSize()
						+ " mode=" + renderType.mode()
						+ " translucent=" + com.namelessgod2008.core.utils.RenderTypeUtils.isTranslucent(renderType)
						+ " dynamic=" + com.namelessgod2008.core.utils.RenderTypeUtils.isDynamic(renderType));
				return;
			}
		}

		com.namelessgod2008.core.AccelStats.ITEM_FMT_NANOS += System.nanoTime() - tFmt;

		ci.cancel();

		// 注：这里曾有 TAKEOVER / DORENDER 两个 itemProbe 探针，它们的字符串拼接是**实参求值**、
		// 每次调用都会执行（DORENDER 还带 RenderType.toString()，会打印整个 RenderSetup）。
		// 实测二者合计 ~1.0 ms/帧（占物品路径的 1/3），已于 2026-09-30 移除。
		// 需要重新诊断时，请改用「只在计数器上自增、不拼字符串」的形式。

		final long tColors = System.nanoTime();
		var colors = new TintLayerColors(submit.tintLayers());
		com.namelessgod2008.core.AccelStats.ITEM_COLORS_NANOS += System.nanoTime() - tColors;

		final long tDrLoop = System.nanoTime();

		var pose = submit.pose();

		for (var entry : byRenderType.entrySet()) {
			// [AR-ITEMAUDIT]
			final long tDrBuf = System.nanoTime();

			var extension	= bufferSource.getBuffer(entry.getKey()).getAccelerated();
			var context		= AcceleratedQuadsRenderer.context(entry.getValue(), colors);

			com.namelessgod2008.core.AccelStats.ITEM_DR_BUF_NANOS += System.nanoTime() - tDrBuf;

			// [AR-ITEMAUDIT]
			final long tDrCall = System.nanoTime();

			extension.doRender(
					AcceleratedQuadsRenderer.INSTANCE,
					context,
					pose.pose(),
					pose.normal(),
					submit.lightCoords(),
					submit.overlayCoords(),
					-1
			);

			com.namelessgod2008.core.AccelStats.ITEM_DR_CALL_NANOS += System.nanoTime() - tDrCall;
		}

		com.namelessgod2008.core.AccelStats.ITEM_DR_LOOP_NANOS += System.nanoTime() - tDrLoop;
	}
}
