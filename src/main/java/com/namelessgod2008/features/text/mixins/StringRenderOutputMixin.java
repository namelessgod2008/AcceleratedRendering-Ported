package com.namelessgod2008.features.text.mixins;

import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.core.buffers.accelerated.builders.BufferSourceExtension;
import com.namelessgod2008.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.namelessgod2008.features.text.cache.ComponentMesh;
import com.namelessgod2008.features.text.IAcceleratedStringRenderOutput;
import com.namelessgod2008.features.text.renderers.AcceleratedSequenceEffectRenderer;
import com.namelessgod2008.features.text.renderers.AcceleratedStyledSequenceRenderer;
import com.namelessgod2008.features.text.AcceleratedTextRenderingFeature;
import com.namelessgod2008.features.text.extensions.BakedGlyphExtension;
import com.namelessgod2008.features.text.key.SimpleSequenceKey;
import lombok.experimental.ExtensionMethod;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Style;
import com.namelessgod2008.core.utils.FastColorCompat;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@ExtensionMethod({
		VertexConsumerExtension	.class,
		BufferSourceExtension	.class,
		BakedGlyphExtension		.class
})
@Mixin(Font.StringRenderOutput.class)
public class StringRenderOutputMixin implements IAcceleratedStringRenderOutput {

	@Shadow										float						x;
	@Shadow										float						y;
	@Shadow(aliases = "field_24240") 	@Final	Font						this$0;
	@Shadow 							@Final	MultiBufferSource			bufferSource;
	@Shadow 							@Final	private	int					color;			// 1.21.4: packed color (was r/g/b/a floats)

	@Shadow @Final @Mutable private	Font.DisplayMode			mode;
	@Shadow @Final private			Matrix4f					pose;
	@Shadow @Final private			boolean						drawShadow;		// 1.21.4: renamed from dropShadow
	@Shadow @Final private			int							packedLightCoords;

	@Unique private static final	Matrix4f					SCRATCH	= new Matrix4f().identity		();
	@Unique private static final	Matrix3f					NORMAL	= new Matrix3f().identity		();
	@Unique private static final	SimpleSequenceKey.Mutable	MUTABLE	= new SimpleSequenceKey.Mutable	();

	@Unique	private					ComponentMesh.Builder		mesh		= null;
	@Unique private					RenderType					type		= null;
	@Unique private					Style						style		= null;
	@Unique private					boolean						accelerated	= false;
	@Unique private					boolean						outline		= false;
	@Unique private					int							computedColor= 0;	// our calculated rendering color
	@Unique private					float						advance		= 0.0f;

	@Inject(
			method = "<init>",
			at = @At("TAIL")
	)
	public void onInit(
			Font				this$0,
			MultiBufferSource	bufferSource,
			float				positionX,
			float				positionY,
			int					color,
			boolean				shadow,
			Matrix4f			pose,
			Font.DisplayMode	mode,
			int					light,
			CallbackInfo		ci
	) {
		if (			CoreFeature						.isLoaded						()
				&&		bufferSource.getAcceleratable()	.isBufferSourceAcceleratable	()
				&&		AcceleratedTextRenderingFeature	.isEnabled						()
				&&		AcceleratedTextRenderingFeature	.shouldUseAcceleratedPipeline	()
				&&	(	CoreFeature						.isRenderingLevel				()
				||		CoreFeature						.isRenderingGui					())
		) {
			this.accelerated	= true;
			this.advance		= 0.0f;
		}
	}

	@Inject(
			method		= "accept",
			at			= @At("HEAD"),
			cancellable	= true
	)
	public void onAccept(
			int								position,
			Style							style,
			int								codePoint,
			CallbackInfoReturnable<Boolean>	cir
	) {
		if (accelerated) {
			cir.setReturnValue(true);

			var italic	= style		.isItalic		();
			var bold	= style		.isBold			();
			var font	= style		.getFont		();
			var fontSet	= this$0	.getFontSet		(font);
			var info	= fontSet	.getGlyphInfo	(codePoint, this$0.filterFishyGlyphs);
			var glyph	= fontSet	.getGlyph		(codePoint);
			var type	= glyph		.renderType		(mode);
			var advance	= info		.getAdvance		(bold);

			if (this.style == null) {
				setup(style, type);
			} else {
				if (		!this.type	.equals(type)
						||	!this.style	.equals(style)
				) {
					flush(fontSet);
					setup(style, type);
				}
			}

			if (style.isObfuscated() && codePoint != 32) {
				if (mesh != null) {
					mesh.addAdvance		(advance);
					mesh.addObfuscated	(
							info,
							this.style,
							this.advance + MUTABLE.getAdvance()
					);
				}

				glyph = fontSet.getRandomGlyph(info);

				var boldOffset		= bold			? info.getBoldOffset	() : 0.0f;
				var shadowOffset	= drawShadow	? info.getShadowOffset	() : 0.0f;

				var extension1 = glyph							.getAccelerated();
				var extension2 = bufferSource.getBuffer(type)	.getAccelerated();

				if (extension2.isAccelerated()) {
					var renderer = extension1.getRenderer(italic);

					SCRATCH.set			(pose);
					SCRATCH.translate	(
							this.x + shadowOffset + MUTABLE.getAdvance(),
							this.y + shadowOffset,
							0.0f
					);

					extension2.doRender(
							renderer,
							null,
							SCRATCH,
							NORMAL,
							packedLightCoords,
							OverlayTexture.NO_OVERLAY,
							computedColor
					);

					if (bold) {
						SCRATCH.translate(
								boldOffset,
								0.0f,
								0.0f
						);

						extension2.doRender(
								renderer,
								null,
								SCRATCH,
								NORMAL,
								packedLightCoords,
								OverlayTexture.NO_OVERLAY,
								computedColor
						);
					}
				} else {
					throw new IllegalStateException("Someone uses incorrect render type in the baked glyph.");
				}

				MUTABLE.addHidden	(codePoint);
				MUTABLE.addAdvance	(advance);
			} else {
				MUTABLE.addCodePoint	(codePoint);
				MUTABLE.addAdvance		(advance);
			}
		}
	}

	@Inject(
			method	= "finish",
			at		= @At("HEAD")
	)
	public void onFinish(
			float							x,
			CallbackInfoReturnable<Float>	cir
	) {
		flush();
	}

	@Unique
	private void setup(Style style, RenderType type) {
		this.style	= style;
		this.type	= type;

		var textColor = style.getColor();

		if (textColor != null) {
			// Style has a specific color — use it directly (1.21.4: no dimFactor)
			this.computedColor = textColor.getValue();
		} else {
			// Use the base color from the constructor (vanilla's packed color field)
			this.computedColor = color;
		}

		MUTABLE.reset		();
		MUTABLE.setStyle	(
				this.style,
				this.drawShadow,
				this.outline
		);
	}

	@Unique
	@Override
	public void flush() {
		if (			this.accelerated
				&& 		this.style	!= null
				&&		this.type	!= null
				&&	!	MUTABLE.isEmpty()
		) {
			flush(this$0.getFontSet(style.getFont()));
		}

		this.style		= null;
		this.type		= null;
		this.outline	= false;
		this.computedColor	= 0;
		this.advance	= 0.0f;

		MUTABLE.reset();
	}

	@Unique
	private void flush(FontSet fontSet) {
		var extension1 = bufferSource.getBuffer(type).getAccelerated();

		if (extension1.isAccelerated()) {
			if (mesh != null) {
				mesh.addAdvance	(MUTABLE.getAdvance());
				mesh.addSequence(
						MUTABLE.bake(),
						this.type,
						this.advance
				);
			}

			SCRATCH.set			(pose);
			SCRATCH.translate	(
					this.x,
					this.y,
					0.0f
			);

			extension1.doRender(
					AcceleratedStyledSequenceRenderer.INSTANCE,
					MUTABLE,
					SCRATCH,
					NORMAL,
					packedLightCoords,
					OverlayTexture.NO_OVERLAY,
					computedColor
			);
		} else {
			throw new IllegalStateException("Someone uses incorrect render type in the baked glyph.");
		}

		if (		style.isStrikethrough	()
				||	style.isUnderlined		()
		) {
			var extension2 = bufferSource.getBuffer(fontSet.whiteGlyph().renderType(mode)).getAccelerated();

			if (extension2.isAccelerated()) {
				extension2.doRender(
						AcceleratedSequenceEffectRenderer.INSTANCE,
						MUTABLE,
						SCRATCH,
						NORMAL,
						packedLightCoords,
						OverlayTexture.NO_OVERLAY,
						computedColor
				);
			} else {
				throw new IllegalStateException("Someone uses incorrect render type in the baked glyph.");
			}
		}

		this.x			+= MUTABLE.getAdvance();
		this.advance	+= MUTABLE.getAdvance();
	}

	@Unique
	@Override
	public void setPosition(float positionX, float positionY) {
		this.x = positionX;
		this.y = positionY;
	}

	@Unique
	@Override
	public void setAccelerated(boolean accelerated) {
		this.accelerated = accelerated;
	}

	@Unique
	@Override
	public void setOutline(boolean outline) {
		this.outline = outline;
	}

	@Unique
	@Override
	public void setColor(int color) {
		this.computedColor = color;
	}

	@Unique
	@Override
	public void setMode(Font.DisplayMode mode) {
		this.mode = mode;
	}

	@Unique
	@Override
	public void beginMesh() {
		mesh = new ComponentMesh.Builder();
	}

	@Unique
	@Override
	public ComponentMesh bake() {
		return mesh == null ? null : mesh.build(drawShadow);
	}
}
