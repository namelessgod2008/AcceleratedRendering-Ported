package com.namelessgod2008.features.modernui.mixins;

import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.features.items.gui.GuiBatchingController;
import com.namelessgod2008.features.modernui.contexts.MUIOutlineDrawContext;
import com.namelessgod2008.features.modernui.contexts.MUIStringDrawContext;
import com.namelessgod2008.features.mods.ModsFeature;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import icyllis.modernui.mc.text.ModernTextRenderer;
import icyllis.modernui.mc.text.TextLayout;
import net.minecraft.client.renderer.MultiBufferSource;
import com.namelessgod2008.core.utils.FastColorCompat;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(ModernTextRenderer.class)
public class ModernTextRendererMixin {

	@WrapOperation(
			method	= "drawText(Licyllis/modernui/mc/text/TextLayout;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;IIZ)F",
			at		= @At(
					value	= "INVOKE",
					target	= "Licyllis/modernui/mc/text/TextLayout;drawText(Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;FFIIIIZIZFIIZ)F"
			)
	)
	public float renderTextFast(
			TextLayout			instance,
			Matrix4f			matrix,
			MultiBufferSource	source,
			float				textX,
			float				textTop,
			int					red,
			int					green,
			int					blue,
			int					alpha,
			boolean				isShadow,
			int					preferredMode,
			boolean				polygonOffset,
			float				uniformScale,
			int					backgroundColor,
			int					packedLight,
			boolean				isSdf,
			Operation<Float>	original
	) {
		if (		!	CoreFeature.isLoaded				()
				||	!	CoreFeature.isGuiBatching			()
				||		CoreFeature.shouldByPassGuiBatching	()
				||	!	ModsFeature.isEnabled				()
				||	!	ModsFeature.shouldAccelerateModernUI()
		) {
			return original.call(
					instance,
					matrix,
					source,
					textX,
					textTop,
					red,
					green,
					blue,
					alpha,
					isShadow,
					preferredMode,
					polygonOffset,
					uniformScale,
					backgroundColor,
					packedLight,
					isSdf
			);
		}

		GuiBatchingController.INSTANCE.submitString(new MUIStringDrawContext(
				new Matrix4f(matrix),
				instance,
				textX,
				textTop,
				uniformScale,
				FastColorCompat.ARGB32.color(
						alpha,
						red,
						green,
						blue
				),
				backgroundColor,
				preferredMode,
				packedLight,
				isShadow,
				polygonOffset,
				isSdf
		));

		return ((TextLayoutAccessor) instance).getTotalAdvance();
	}

	@WrapMethod(method = "drawText8xOutline")
	public void renderOutlineFast(
			FormattedCharSequence	text,
			float					textX,
			float					textY,
			int						textColor,
			int						backgroundColor,
			Matrix4f				transform,
			MultiBufferSource		bufferSource,
			int						packedLight,
			Operation<Void>			original
	) {
		if (		!	CoreFeature.isLoaded				()
				||	!	CoreFeature.isGuiBatching			()
				||		CoreFeature.shouldByPassGuiBatching	()
				||	!	ModsFeature.isEnabled				()
				||	!	ModsFeature.shouldAccelerateModernUI()
		) {
			original.call(
					text,
					textX,
					textY,
					textColor,
					backgroundColor,
					transform,
					bufferSource,
					packedLight
			);

			return;
		}

		GuiBatchingController.INSTANCE.submitString(new MUIOutlineDrawContext(
				new Matrix4f(transform),
				(ModernTextRenderer) (Object) this,
				text,
				textX,
				textY,
				textColor,
				backgroundColor,
				packedLight
		));
	}
}
