package com.github.argon4w.acceleratedrendering.features.modernui.mixins;

import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.github.argon4w.acceleratedrendering.features.modernui.renderers.AcceleratedMUIEffectRenderer;
import com.github.argon4w.acceleratedrendering.features.mods.ModsFeature;
import com.github.argon4w.acceleratedrendering.features.text.AcceleratedTextRenderingFeature;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import icyllis.modernui.mc.text.TextRenderEffect;
import lombok.experimental.ExtensionMethod;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.github.argon4w.acceleratedrendering.core.utils.FastColorCompat;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@ExtensionMethod(VertexConsumerExtension.class)
@Mixin(TextRenderEffect.class)
public class TextRenderEffectMixin {

	@Unique private static final Matrix3f NORMAL = new Matrix3f().identity();

	@WrapMethod(method = "drawUnderline(Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFIIIIIZ)V")
	private static void drawUnderlineFast(
		Matrix4f transform, VertexConsumer builder, float start, float end, float baseline,
		int red, int green, int blue, int alpha, int packedLight, boolean effect,
		Operation<Void> original
	) {
		var extension = builder.getAccelerated();
		if (CoreFeature.isLoaded() && AcceleratedTextRenderingFeature.isEnabled()
			&& AcceleratedTextRenderingFeature.shouldUseAcceleratedPipeline()
			&& ModsFeature.isEnabled() && ModsFeature.shouldAccelerateModernUI()
			&& (CoreFeature.isRenderingLevel() || CoreFeature.isRenderingGui())
			&& extension.isAccelerated()
		) {
			extension.doRender(AcceleratedMUIEffectRenderer.INSTANCE,
				AcceleratedMUIEffectRenderer.context(baseline + 0.6666667F, start, end),
				transform, NORMAL, packedLight, OverlayTexture.NO_OVERLAY,
				FastColorCompat.ARGB32.color(alpha, red, green, blue));
			return;
		}
		original.call(transform, builder, start, end, baseline, red, green, blue, alpha, packedLight, effect);
	}

	@WrapMethod(method = "drawStrikethrough(Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFIIIIIZ)V")
	private static void drawStrikethroughFast(
		Matrix4f transform, VertexConsumer builder, float start, float end, float baseline,
		int red, int green, int blue, int alpha, int packedLight, boolean effect,
		Operation<Void> original
	) {
		var extension = builder.getAccelerated();
		if (CoreFeature.isLoaded() && AcceleratedTextRenderingFeature.isEnabled()
			&& AcceleratedTextRenderingFeature.shouldUseAcceleratedPipeline()
			&& ModsFeature.isEnabled() && ModsFeature.shouldAccelerateModernUI()
			&& (CoreFeature.isRenderingLevel() || CoreFeature.isRenderingGui())
			&& extension.isAccelerated()
		) {
			extension.doRender(AcceleratedMUIEffectRenderer.INSTANCE,
				AcceleratedMUIEffectRenderer.context(baseline - 3.5F, start, end),
				transform, NORMAL, packedLight, OverlayTexture.NO_OVERLAY,
				FastColorCompat.ARGB32.color(alpha, red, green, blue));
			return;
		}
		original.call(transform, builder, start, end, baseline, red, green, blue, alpha, packedLight, effect);
	}
}
