package com.namelessgod2008.compat.iris.mixins.iris;

import com.namelessgod2008.compat.iris.IrisCompatBuffers;
import com.namelessgod2008.compat.iris.IrisCompatBuffersProvider;
import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.core.CoreStates;
import com.namelessgod2008.core.buffers.accelerated.builders.BufferSourceExtension;
import com.namelessgod2008.core.buffers.accelerated.layers.LayerDrawType;
import lombok.experimental.ExtensionMethod;
import net.irisshaders.iris.mixin.LevelRendererAccessor;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shadows.ShadowCompositeRenderer;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.RenderBuffers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Iris 阴影通道的加速支持。
 *
 * <p>26.1 移植时相对 1.21.4 的两处适配（Iris 1.11.4）：
 * <ul>
 *   <li>{@code net.irisshaders.batchedentityrendering} 包已被 Iris 移除，
 *       原先用 {@code RenderBuffersExt.beginLevelRendering()/endLevelRendering()}
 *       包住缓冲绑定的写法不再需要 —— 该状态改由 Iris 自己在
 *       {@code MixinLevelRenderer} 中维护；</li>
 *   <li>{@code renderShadows} 签名新增第三参 {@code CameraRenderState}。</li>
 * </ul>
 * 注入点（{@code renderShadows} 内的 {@code BufferSource.endBatch()}）在 1.11.4 中仍存在，
 * 位于 {@code FeatureRenderDispatcher.renderAllFeatures()} 之后。
 */
@Pseudo
@ExtensionMethod(BufferSourceExtension	.class)
@Mixin			(ShadowRenderer			.class)
public class ShadowRendererMixin {

	@Shadow @Final private RenderBuffers	buffers;

	@Inject(
			method	= "<init>",
			at		= @At("TAIL")
	)
	public void bindAcceleratedShadowBufferSources(
			IrisRenderingPipeline	pipeline,
			ProgramSource			shadow,
			PackDirectives			directives,
			ShadowRenderTargets		shadowRenderTargets,
			ShadowCompositeRenderer	compositeRenderer,
			CustomUniforms			customUniforms,
			boolean					separateHardwareSamplers,
			CallbackInfo			ci
	) {
		buffers.bufferSource			().getAcceleratable().bindAcceleratedBufferSource(IrisCompatBuffersProvider.SHADOW);
		buffers.crumblingBufferSource	().getAcceleratable().bindAcceleratedBufferSource(IrisCompatBuffersProvider.SHADOW);
		buffers.outlineBufferSource		().getAcceleratable().bindAcceleratedBufferSource(IrisCompatBuffersProvider.SHADOW);
	}

	@Inject(
			method	= "renderShadows",
			at		= @At(
					value	= "INVOKE",
					target	= "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V"
			)
	)
	public void endAllBatches(
			LevelRendererAccessor	levelRenderer,
			Camera					playerCamera,
			CameraRenderState		cameraRenderState,
			CallbackInfo			ci
	) {
		if (!CoreFeature.isLoaded()) {
			return;
		}

		CoreStates								.recordBuffers	();
		IrisCompatBuffers.BLOCK_SHADOW			.prepareBuffers	();
		IrisCompatBuffers.ENTITY_SHADOW			.prepareBuffers	();
		IrisCompatBuffers.GLYPH_SHADOW			.prepareBuffers	();
		IrisCompatBuffers.POS_TEX_SHADOW		.prepareBuffers	();
		IrisCompatBuffers.POS_TEX_COLOR_SHADOW	.prepareBuffers	();
		CoreStates								.restoreBuffers	();

		IrisCompatBuffers.BLOCK_SHADOW			.drawBuffers	(LayerDrawType.ALL);
		IrisCompatBuffers.ENTITY_SHADOW			.drawBuffers	(LayerDrawType.ALL);
		IrisCompatBuffers.GLYPH_SHADOW			.drawBuffers	(LayerDrawType.ALL);
		IrisCompatBuffers.POS_TEX_SHADOW		.drawBuffers	(LayerDrawType.ALL);
		IrisCompatBuffers.POS_TEX_COLOR_SHADOW	.drawBuffers	(LayerDrawType.ALL);

		IrisCompatBuffers.BLOCK_SHADOW			.clearBuffers	();
		IrisCompatBuffers.ENTITY_SHADOW			.clearBuffers	();
		IrisCompatBuffers.GLYPH_SHADOW			.clearBuffers	();
		IrisCompatBuffers.POS_TEX_SHADOW		.clearBuffers	();
		IrisCompatBuffers.POS_TEX_COLOR_SHADOW	.clearBuffers	();
	}
}
