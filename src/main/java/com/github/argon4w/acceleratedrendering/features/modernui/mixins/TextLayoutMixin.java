package com.github.argon4w.acceleratedrendering.features.modernui.mixins;

import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.github.argon4w.acceleratedrendering.core.utils.DoNothingVertexConsumer;
import com.github.argon4w.acceleratedrendering.features.modernui.renderers.AcceleratedMUIBgRenderer;
import com.github.argon4w.acceleratedrendering.features.modernui.renderers.AcceleratedMUIGlyphRenderer;
import com.github.argon4w.acceleratedrendering.features.modernui.renderers.AcceleratedMUIOutlineRenderer;
import com.github.argon4w.acceleratedrendering.features.mods.ModsFeature;
import com.github.argon4w.acceleratedrendering.features.text.AcceleratedTextRenderingFeature;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mojang.blaze3d.vertex.VertexConsumer;
import icyllis.modernui.mc.text.GLBakedGlyph;
import icyllis.modernui.mc.text.TextLayout;
import lombok.experimental.ExtensionMethod;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.github.argon4w.acceleratedrendering.core.utils.FastColorCompat;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Modern UI 3.12.0: drawText bytecode reordered — glyph quad moved after background quad.
 * Ordinals updated: bg=0-3, glyph=4-7. @Local uses index-based resolution (names removed in 3.12.0).
 */
@Pseudo
@ExtensionMethod(VertexConsumerExtension.class)
@Mixin(TextLayout.class)
public class TextLayoutMixin {

	@Shadow @Final private float mTotalAdvance;

	@Unique private static final Matrix3f NORMAL = new Matrix3f().identity();

	// 3.12.0: background vertices (ordinals 1-3, was 5-7)
	@WrapOperation(method = "drawText",
		at = {
			@At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 1),
			@At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 2),
			@At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 3)
	})
	public VertexConsumer preventVanillaBgVertex(
		VertexConsumer instance, Matrix4f pose, float positionX, float positionY, float positionZ,
		Operation<VertexConsumer> original, @Share("skipBg") LocalBooleanRef skipBg
	) {
		return skipBg.get() ? DoNothingVertexConsumer.INSTANCE : original.call(instance, pose, positionX, positionY, positionZ);
	}

	// 3.12.0: glyph vertices (ordinals 5-7, was 1-3)
	@WrapOperation(method = "drawText",
		at = {
			@At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 5),
			@At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 6),
			@At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 7)
	})
	public VertexConsumer preventVanillaGlyphVertex(
		VertexConsumer instance, Matrix4f pose, float positionX, float positionY, float positionZ,
		Operation<VertexConsumer> original, @Share("skipGlyph") LocalBooleanRef skipGlyph
	) {
		return skipGlyph.get() ? DoNothingVertexConsumer.INSTANCE : original.call(instance, pose, positionX, positionY, positionZ);
	}

	// 3.12.0: first glyph vertex (ordinal 4, was 0). Locals use index: glyph=34, rx=36, ry=37, w=38, h=39, upSkew=47, downSkew=48
	@WrapOperation(method = "drawText",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 4))
	public VertexConsumer drawMUIGlyph(
		VertexConsumer instance, Matrix4f pose, float positionX, float positionY, float positionZ,
		Operation<VertexConsumer> original, @Share("skipGlyph") LocalBooleanRef skipGlyph,
		@Local(index = 34) GLBakedGlyph glyph,
		@Local(index = 36) float rx, @Local(index = 37) float ry,
		@Local(index = 38) float w, @Local(index = 39) float h,
		@Local(index = 47) float upSkew, @Local(index = 48) float downSkew,
		@Local(index = 5) int r, @Local(index = 6) int g, @Local(index = 7) int b, @Local(index = 8) int a,
		@Local(index = 14) int packedLight
	) {
		var extension = instance.getAccelerated();
		if (CoreFeature.isLoaded() && AcceleratedTextRenderingFeature.isEnabled()
			&& AcceleratedTextRenderingFeature.shouldUseAcceleratedPipeline()
			&& ModsFeature.isEnabled() && ModsFeature.shouldAccelerateModernUI()
			&& (CoreFeature.isRenderingLevel() || CoreFeature.isRenderingGui())
			&& extension.isAccelerated()
		) {
			skipGlyph.set(true);
			extension.doRender(AcceleratedMUIGlyphRenderer.INSTANCE,
				AcceleratedMUIGlyphRenderer.context(glyph, rx, ry, w, h, upSkew, downSkew),
				pose, NORMAL, packedLight, OverlayTexture.NO_OVERLAY,
				FastColorCompat.ARGB32.color(a, r, g, b));
			return DoNothingVertexConsumer.INSTANCE;
		}
		return original.call(instance, pose, positionX, positionY, positionZ);
	}

	// 3.12.0: first background vertex (ordinal 0, was 4). Locals use index: bgColor=13, packedLight=index 14
	@WrapOperation(method = "drawText",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 0))
	public VertexConsumer drawMUIBg(
		VertexConsumer instance, Matrix4f pose, float positionX, float positionY, float positionZ,
		Operation<VertexConsumer> original, @Share("skipBg") LocalBooleanRef skipBg,
		@Local(index = 3) float textX, @Local(index = 4) float textY,
		@Local(index = 13) int bgColor, @Local(index = 14) int packedLight
	) {
		var extension = instance.getAccelerated();
		if (CoreFeature.isLoaded() && AcceleratedTextRenderingFeature.isEnabled()
			&& AcceleratedTextRenderingFeature.shouldUseAcceleratedPipeline()
			&& ModsFeature.isEnabled() && ModsFeature.shouldAccelerateModernUI()
			&& (CoreFeature.isRenderingLevel() || CoreFeature.isRenderingGui())
			&& extension.isAccelerated()
		) {
			skipBg.set(true);
			extension.doRender(AcceleratedMUIBgRenderer.INSTANCE,
				AcceleratedMUIBgRenderer.context(textX, textY, mTotalAdvance),
				pose, NORMAL, packedLight, OverlayTexture.NO_OVERLAY, bgColor);
			return DoNothingVertexConsumer.INSTANCE;
		}
		return original.call(instance, pose, positionX, positionY, positionZ);
	}

	// drawTextOutline ordinals unchanged (0-3). Locals: glyph=21, rx=23, ry=24, sBloat=18
	@WrapOperation(method = "drawTextOutline",
		at = {
			@At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 1),
			@At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 2),
			@At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 3)
	})
	public VertexConsumer preventVanillaOutlineVertex(
		VertexConsumer instance, Matrix4f pose, float positionX, float positionY, float positionZ,
		Operation<VertexConsumer> original, @Share("skipOutline") LocalBooleanRef skipOutline
	) {
		return skipOutline.get() ? DoNothingVertexConsumer.INSTANCE : original.call(instance, pose, positionX, positionY, positionZ);
	}

	@WrapOperation(method = "drawTextOutline",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 0))
	public VertexConsumer drawMUIOutline(
		VertexConsumer instance, Matrix4f pose, float positionX, float positionY, float positionZ,
		Operation<VertexConsumer> original, @Share("skipOutline") LocalBooleanRef skipOutline,
		@Local(index = 21) GLBakedGlyph glyph,
		@Local(index = 23) float rx, @Local(index = 24) float ry,
		@Local(index = 18) float sBloat,
		@Local(index = 5) int r, @Local(index = 6) int g, @Local(index = 7) int b, @Local(index = 8) int a,
		@Local(index = 9) int packedLight
	) {
		var extension = instance.getAccelerated();
		if (CoreFeature.isLoaded() && AcceleratedTextRenderingFeature.isEnabled()
			&& AcceleratedTextRenderingFeature.shouldUseAcceleratedPipeline()
			&& ModsFeature.isEnabled() && ModsFeature.shouldAccelerateModernUI()
			&& (CoreFeature.isRenderingLevel() || CoreFeature.isRenderingGui())
			&& extension.isAccelerated()
		) {
			skipOutline.set(true);
			extension.doRender(AcceleratedMUIOutlineRenderer.INSTANCE,
				AcceleratedMUIOutlineRenderer.context(glyph, rx, ry, 0, 0, sBloat),
				pose, NORMAL, packedLight, OverlayTexture.NO_OVERLAY,
				FastColorCompat.ARGB32.color(a, r, g, b));
			return DoNothingVertexConsumer.INSTANCE;
		}
		return original.call(instance, pose, positionX, positionY, positionZ);
	}
}
