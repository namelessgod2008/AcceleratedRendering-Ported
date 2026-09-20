package com.namelessgod2008.compat.iris.mixins.iris;

import com.namelessgod2008.compat.iris.IrisCompatBuffers;
import com.namelessgod2008.compat.iris.IrisCompatBuffersProvider;
import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.core.CoreStates;
import com.namelessgod2008.core.buffers.accelerated.builders.BufferSourceExtension;
import com.namelessgod2008.core.buffers.accelerated.layers.LayerDrawType;
import lombok.experimental.ExtensionMethod;
import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Iris 手部渲染通道的加速支持。
 *
 * <p><b>26.1 / Iris 1.11.4 适配</b>（相对 1.21.4，三处结构性变化）：
 * <ol>
 *   <li>{@code bufferSource} 字段类型由 {@code FullyBufferedMultiBufferSource}
 *       改为原版 {@code RenderBuffers}（Iris 移除了整个
 *       {@code net.irisshaders.batchedentityrendering} 包）；</li>
 *   <li>{@code renderSolid} / {@code renderTranslucent} 签名新增 {@code CameraRenderState} 参数，
 *       且内部调用由 {@code ItemInHandRenderer.renderHandsWithItems} 改为 Iris 自己的
 *       {@code iris$renderHandsWithCustomRenderer}（其参数含 {@code SubmitNodeStorage} ——
 *       26.1 的「提交-渲染两段式」）；</li>
 *   <li>缓冲刷新的时机由原先的 {@code renderSolid}/{@code renderTranslucent} 之后，
 *       移到 {@code endRender()} —— 那里才真正执行
 *       {@code FeatureRenderDispatcher.renderAllFeatures()} 与 {@code BufferSource.endBatch()}。</li>
 * </ol>
 *
 * <p>语义不变：手部渲染期间置 {@code isRenderingHand} 标志，并把缓冲绑定切到
 * {@link IrisCompatBuffersProvider#HAND} 的独立缓冲集，避免手部投影矩阵污染世界实体的缓冲。
 */
@Pseudo
@ExtensionMethod(BufferSourceExtension.class)
@Mixin(HandRenderer.class)
public class HandRendererMixin {

	@Shadow @Final private RenderBuffers bufferSource;

	@Inject(method = "<init>", at = @At("TAIL"))
	public void bindAcceleratedBufferSourceHand(CallbackInfo ci) {
		bufferSource.bufferSource			().getAcceleratable().bindAcceleratedBufferSource(IrisCompatBuffersProvider.HAND);
		bufferSource.crumblingBufferSource	().getAcceleratable().bindAcceleratedBufferSource(IrisCompatBuffersProvider.HAND);
		bufferSource.outlineBufferSource	().getAcceleratable().bindAcceleratedBufferSource(IrisCompatBuffersProvider.HAND);
	}

	@Inject(
			method = "renderSolid",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;iris$renderHandsWithCustomRenderer(Lnet/irisshaders/iris/pathways/HandRenderer;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeStorage;Lnet/minecraft/client/player/LocalPlayer;I)V",
					shift = At.Shift.BEFORE
			)
	)
	public void startRenderSolidFast(
			Matrix4fc				modelMatrix,
			float					tickDelta,
			Camera					camera,
			CameraRenderState		cameraRenderState,
			GameRenderer			gameRenderer,
			WorldRenderingPipeline	pipeline,
			CallbackInfo			ci
	) {
		CoreFeature.setRenderingHand();
	}

	@Inject(
			method = "renderSolid",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;iris$renderHandsWithCustomRenderer(Lnet/irisshaders/iris/pathways/HandRenderer;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeStorage;Lnet/minecraft/client/player/LocalPlayer;I)V",
					shift = At.Shift.AFTER
			)
	)
	public void stopRenderSolidFast(
			Matrix4fc				modelMatrix,
			float					tickDelta,
			Camera					camera,
			CameraRenderState		cameraRenderState,
			GameRenderer			gameRenderer,
			WorldRenderingPipeline	pipeline,
			CallbackInfo			ci
	) {
		CoreFeature.resetRenderingHand();
	}

	@Inject(
			method = "renderTranslucent",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;iris$renderHandsWithCustomRenderer(Lnet/irisshaders/iris/pathways/HandRenderer;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeStorage;Lnet/minecraft/client/player/LocalPlayer;I)V",
					shift = At.Shift.BEFORE
			)
	)
	public void startRenderTranslucentFast(
			Matrix4fc				modelMatrix,
			float					tickDelta,
			Camera					camera,
			CameraRenderState		cameraRenderState,
			GameRenderer			gameRenderer,
			WorldRenderingPipeline	pipeline,
			CallbackInfo			ci
	) {
		CoreFeature.setRenderingHand();
	}

	@Inject(
			method = "renderTranslucent",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;iris$renderHandsWithCustomRenderer(Lnet/irisshaders/iris/pathways/HandRenderer;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeStorage;Lnet/minecraft/client/player/LocalPlayer;I)V",
					shift = At.Shift.AFTER
			)
	)
	public void stopRenderTranslucentFast(
			Matrix4fc				modelMatrix,
			float					tickDelta,
			Camera					camera,
			CameraRenderState		cameraRenderState,
			GameRenderer			gameRenderer,
			WorldRenderingPipeline	pipeline,
			CallbackInfo			ci
	) {
		CoreFeature.resetRenderingHand();
	}

	/**
	 * 绘制手部加速缓冲。
	 *
	 * <p>注入 {@code endRender} 的 TAIL：Iris 1.11.4 在那里才执行
	 * {@code FeatureRenderDispatcher.renderAllFeatures()} + {@code BufferSource.endBatch()}，
	 * 即顶点真正提交到 GPU 的时机。原实现挂在 renderSolid/renderTranslucent 之后，
	 * 在 26.1 的两段式架构下会过早（几何尚未提交）。
	 */
	@Inject(method = "endRender", at = @At("TAIL"))
	public void drawHandBuffers(CallbackInfo ci) {
		if (!CoreFeature.isLoaded()) {
			return;
		}

		CoreStates									.recordBuffers		();
		IrisCompatBuffers.ENTITY_HAND				.prepareBuffers		();
		IrisCompatBuffers.BLOCK_HAND				.prepareBuffers		();
		IrisCompatBuffers.POS_HAND					.prepareBuffers		();
		IrisCompatBuffers.POS_COLOR_HAND			.prepareBuffers		();
		IrisCompatBuffers.POS_TEX_HAND				.prepareBuffers		();
		IrisCompatBuffers.POS_TEX_COLOR_HAND		.prepareBuffers		();
		IrisCompatBuffers.POS_COLOR_TEX_LIGHT_HAND	.prepareBuffers		();
		CoreStates									.restoreBuffers		();

		IrisCompatBuffers.ENTITY_HAND				.drawBuffers		(LayerDrawType.ALL);
		IrisCompatBuffers.BLOCK_HAND				.drawBuffers		(LayerDrawType.ALL);
		IrisCompatBuffers.POS_HAND					.drawBuffers		(LayerDrawType.ALL);
		IrisCompatBuffers.POS_COLOR_HAND			.drawBuffers		(LayerDrawType.ALL);
		IrisCompatBuffers.POS_TEX_HAND				.drawBuffers		(LayerDrawType.ALL);
		IrisCompatBuffers.POS_TEX_COLOR_HAND		.drawBuffers		(LayerDrawType.ALL);
		IrisCompatBuffers.POS_COLOR_TEX_LIGHT_HAND	.drawBuffers		(LayerDrawType.ALL);

		IrisCompatBuffers.ENTITY_HAND				.clearBuffers		();
		IrisCompatBuffers.BLOCK_HAND				.clearBuffers		();
		IrisCompatBuffers.POS_HAND					.clearBuffers		();
		IrisCompatBuffers.POS_COLOR_HAND			.clearBuffers		();
		IrisCompatBuffers.POS_TEX_HAND				.clearBuffers		();
		IrisCompatBuffers.POS_TEX_COLOR_HAND		.clearBuffers		();
		IrisCompatBuffers.POS_COLOR_TEX_LIGHT_HAND	.clearBuffers		();
	}
}
