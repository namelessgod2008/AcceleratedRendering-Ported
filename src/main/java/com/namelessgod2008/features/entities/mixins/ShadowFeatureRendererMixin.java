package com.namelessgod2008.features.entities.mixins;

import com.namelessgod2008.core.AccelStats;
import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.namelessgod2008.features.entities.AcceleratedEntityRenderingFeature;
import com.namelessgod2008.features.entities.AcceleratedEntityShadowRenderer;
import lombok.experimental.ExtensionMethod;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.ShadowFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.1 实体阴影加速（对应 1.21.4 的 {@code EntityRenderDispatcherMixin}）。
 *
 * <p>1.21.4 注入的是 {@code EntityRenderDispatcher.renderBlockShadow} —— 该方法把「判定」与
 * 「写顶点」放在一起，所以当时能在 HEAD 处取消并转交加速管线。26.1 已把它拆成两段：
 * <ul>
 *   <li>提交段：{@code EntityRenderer.extractShadowPiece} 完成全部可见性/亮度/碰撞判定，
 *       把结果记为 {@code EntityRenderState.ShadowPiece}，经 {@code submitShadow} 落入
 *       {@code SubmitNodeStorage.ShadowSubmit}；</li>
 *   <li>渲染段：{@link ShadowFeatureRenderer#renderTranslucent} 才真正写顶点。</li>
 * </ul>
 * 因此新注入点落在渲染段——这是唯一还能拿到 {@code VertexConsumer} 的地方
 * （提交段只有 {@code SubmitNodeCollector}，拿不到顶点消费者，无法加速）。
 *
 * <p>layer 无需在此处理：{@code FeatureRenderDispatcherMixin} 的 push 注入点就在
 * {@code ShadowFeatureRenderer.renderTranslucent} 调用之前，故此处读到的 defaultLayer
 * 已经是本 order 桶对应的加速层，阴影与同桶实体几何的相对顺序自动一致。
 *
 * <p>RenderType 用 {@code RenderTypes.entityShadow(...)} 现场取，而非 {@code @Shadow} 原版的
 * private 字段：{@code RenderTypes.ENTITY_SHADOW} 经 {@code Util.memoize} 缓存，相同入参必返回
 * 同一实例，因此取到的一定就是原版那个 RenderType，既不必依赖私有字段，也不受其改名影响。
 *
 * <p><b>priority = 999 是必需的</b>：Sodium 0.9.1 的
 * {@code features.render.entity.shadows.ShadowFeatureRendererMixin} 同样注入本方法的 HEAD，
 * 且在开头就无条件 {@code ci.cancel()} 用 Sodium 自己的写入器接管整个阴影渲染。两者优先级默认
 * 相同时后应用者排在后面（实测 Sodium 先应用 → 本 mixin 永不执行）。取 999 使其先于 Sodium
 * （默认 1000）执行，从而由本 mod 的加速管线接管阴影；本 mixin 的守卫不满足时不 cancel，
 * 控制权仍会交给 Sodium/原版。这与 1.21.4 项目对 Sodium FRAPI 的处理方式一致
 * （见其 {@code ItemRendererMixin} 的 priority = 999）。
 */
@ExtensionMethod(VertexConsumerExtension.class)
@Mixin		(
		value		= ShadowFeatureRenderer.class,
		priority	= 999
)
public class ShadowFeatureRendererMixin {

	@Unique private static final Matrix3f SHADOW_NORMAL_MATRIX = new Matrix3f().identity();

	@Inject(
			method		= "renderTranslucent",
			at			= @At("HEAD"),
			cancellable	= true
	)
	private void fastShadow(
			SubmitNodeCollection			nodeCollection,
			MultiBufferSource.BufferSource	bufferSource,
			CallbackInfo					ci
	) {
		// [临时探针] 统计 shadow 规模与耗时
		long probeStart = System.nanoTime();
		int  probeSubmits = nodeCollection.getShadowSubmits().size();
		int  probePieces  = 0;
		for (var s : nodeCollection.getShadowSubmits()) {
			probePieces += s.pieces().size();
		}

		if (		!CoreFeature						.isLoaded						()
				||	!CoreFeature						.isRenderingLevel				()
				||	!AcceleratedEntityRenderingFeature	.isEnabled						()
				||	!AcceleratedEntityRenderingFeature	.shouldUseAcceleratedPipeline	()
		) {
			return;
		}

		var renderType	= RenderTypes.entityShadow(Identifier.withDefaultNamespace("textures/misc/shadow.png"));
		var buffer		= bufferSource			.getBuffer			(renderType);
		var extension	= buffer				.getAccelerated		();

		if (!extension.isAccelerated()) {
			AccelStats.SHADOW_MISS_NANOS += System.nanoTime() - probeStart;
			return;
		}

		ci.cancel();

		AccelStats.SHADOW_CALLS		++;
		AccelStats.SHADOW_SUBMITS	+= probeSubmits;
		AccelStats.SHADOW_PIECES	+= probePieces;

		// doRender → beginTransform 会把矩阵写入共享缓冲（拷贝），故可安全复用同一实例
		var pose = new Matrix4f();

		for (var submit : nodeCollection.getShadowSubmits()) {
			pose.set(submit.pose());

			for (var piece : submit.pieces()) {
				extension.doRender(
						AcceleratedEntityShadowRenderer.INSTANCE,
						AcceleratedEntityShadowRenderer.context(
								piece.relativeX	(),
								piece.relativeY	(),
								piece.relativeZ	(),
								piece.shapeBelow(),
								submit.radius	(),
								piece.alpha		()
						),
						pose,
						SHADOW_NORMAL_MATRIX,
						LightCoordsUtil	.FULL_BRIGHT,
						OverlayTexture	.NO_OVERLAY,
						-1
				);
			}
		}

		AccelStats.SHADOW_NANOS += System.nanoTime() - probeStart;
	}
}
