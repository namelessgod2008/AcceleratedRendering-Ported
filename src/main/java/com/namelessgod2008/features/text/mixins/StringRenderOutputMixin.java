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
import net.minecraft.client.renderer.rendertype.RenderType;
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
	@Unique private					RenderType					effect		= null;
	@Unique private					Style						style		= null;
	@Unique private					boolean						accelerated	= false;
	@Unique private					boolean						outline		= false;
	@Unique private					int							computedColor= 0;	// our calculated rendering color
	@Unique private					float						advance		= 0.0f;

	@Inject(
			method = "<init>(Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/renderer/MultiBufferSource;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/gui/Font$DisplayMode;I)V",
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

	// 1.21.4 新增 11 参构造 (+backgroundColor, +inverseDepth) — renderText(String/FormattedCharSequence) 走这个。
	// 没有此注入时 renderText 路径的 sink 永远 accelerated=false → FontMixin 缓存空 mesh → 普通文字只闪现一帧后消失。
	@Inject(
			method = "<init>(Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/renderer/MultiBufferSource;FFIIZLorg/joml/Matrix4f;Lnet/minecraft/client/gui/Font$DisplayMode;IZ)V",
			at = @At("TAIL")
	)
	public void onInitWithBackground(
			Font				this$0,
			MultiBufferSource	bufferSource,
			float				positionX,
			float				positionY,
			int					color,
			int					backgroundColor,
			boolean				shadow,
			Matrix4f			pose,
			Font.DisplayMode	mode,
			int					light,
			boolean				inverseDepth,
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
			var effect	= fontSet.whiteGlyph()	.renderType		(mode);
			var type	= glyph					.renderType		(mode);
			var advance	= info					.getAdvance		(bold);

			if (this.style == null) {
				setup(
						type,
						effect,
						style
				);
			} else {
				if (		!this.type	.equals(type)
						||	!this.style	.equals(style)
						||	!this.effect.equals(effect)
				) {
					flush0();
					setup(
							type,
							effect,
							style
					);
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

				var buffer = bufferSource.getBuffer(type);

				var boldOffset		= bold			? info.getBoldOffset	() : 0.0f;
				var shadowOffset	= drawShadow	? info.getShadowOffset	() : 0.0f;

				var extension1 = glyph	.getAccelerated();
				var extension2 = buffer	.getAccelerated();

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
	private void setup(
			RenderType	type,
			RenderType	effect,
			Style		style
	) {
		this.style	= style;
		this.effect	= effect;
		this.type	= type;

		var textColor = style.getColor();

		if (textColor != null) {
			// TextColor.getValue() 只有 24-bit RGB（alpha=0）。按 vanilla getTextColor() 语义
			// 继承构造色的 alpha，否则 style 染色文字（含 8xOutline 描边经 WithColorSink 注入的
			// 颜色）alpha=0 全透明。
			this.computedColor = FastColorCompat.ARGB32.color(
					FastColorCompat.ARGB32.alpha(this.color),
					textColor.getValue()
			);
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
				&&		this.effect	!= null
				&&	!	MUTABLE.isEmpty()
		) {
			flush0();
		}

		this.style		= null;
		this.type		= null;
		this.effect		= null;
		this.outline	= false;
		this.computedColor	= 0;
		this.advance	= 0.0f;

		MUTABLE.reset();
	}

	@Unique
	private void flush0() {
		var buffer1 = bufferSource.getBuffer(type);

		if (mesh != null) {
			mesh.addAdvance	(MUTABLE.getAdvance());
			mesh.addSequence(
					MUTABLE.bake(),
					this.type,
					this.effect,
					this.advance
			);
		}

		SCRATCH.set			(pose);
		SCRATCH.translate	(
				this.x,
				this.y,
				0.0f
		);

		var extension1 = buffer1.getAccelerated();

		if (extension1.isAccelerated()) {
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
			AcceleratedStyledSequenceRenderer.INSTANCE.buildSequenceMesh(
					buffer1,
					MUTABLE,
					SCRATCH,
					computedColor,
					packedLightCoords
			);
		}

		if (		style.isStrikethrough	()
				||	style.isUnderlined		()
		) {
			var buffer2 = bufferSource.getBuffer(effect);

			var extension2 = buffer2.getAccelerated();

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
				AcceleratedSequenceEffectRenderer.INSTANCE.buildSequenceMesh(
						buffer2,
						MUTABLE,
						SCRATCH,
						computedColor,
						packedLightCoords
				);
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
