package com.namelessgod2008.features.text.mixins;

import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.core.buffers.accelerated.builders.IBufferGraph;
import com.namelessgod2008.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.namelessgod2008.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import com.namelessgod2008.core.meshes.IMesh;
import com.namelessgod2008.core.meshes.collectors.SimpleMeshCollector;
import com.namelessgod2008.features.entities.AcceleratedEntityRenderingFeature;
import com.namelessgod2008.features.text.renderers.AcceleratedBakedGlyphRenderer;
import com.namelessgod2008.features.text.AcceleratedTextRenderingFeature;
import com.namelessgod2008.features.text.IAcceleratedBakedGlyph;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.experimental.ExtensionMethod;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.namelessgod2008.core.utils.FastColorCompat;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@ExtensionMethod(value = VertexConsumerExtension.class)
@Mixin			(value = BakedGlyph				.class, priority = 0)
public class BakedGlyphMixin implements IAcceleratedRenderer<BakedGlyph.Effect>, IAcceleratedBakedGlyph {

	@Shadow @Final public			float												u0;
	@Shadow @Final public			float												v0;
	@Shadow @Final public			float												u1;
	@Shadow @Final public			float												v1;

	@Unique private static	final	Matrix4f											TRANSFORM		= new Matrix4f().identity();
	@Unique private static	final	Matrix3f											NORMAL			= new Matrix3f().identity();

	@Unique private			final	AcceleratedBakedGlyphRenderer						normalRenderer	= new AcceleratedBakedGlyphRenderer	((BakedGlyph) (Object) this, false);
	@Unique private			final	AcceleratedBakedGlyphRenderer						italicRenderer	= new AcceleratedBakedGlyphRenderer	((BakedGlyph) (Object) this, true);
	@Unique private			final	Map<BakedGlyph.Effect, Map<IBufferGraph, IMesh>>	effectMeshes	= new Object2ObjectOpenHashMap<>	();

	@Inject(
			method		= "render",
			at			= @At("HEAD"),
			cancellable	= true
	)
	public void renderFast(
			boolean			pItalic,
			float			positionX,
			float			positionY,
			Matrix4f		pMatrix,
			VertexConsumer	pBuffer,
			int				pColor,
			boolean			pBold,
			int				pPackedLight,
			CallbackInfo	ci
	) {
		var extension = pBuffer.getAccelerated();

		if (			CoreFeature						.isLoaded						()
				&&		AcceleratedTextRenderingFeature	.isEnabled						()
				&&		AcceleratedTextRenderingFeature	.shouldUseAcceleratedPipeline	()
				&&	(	CoreFeature						.isRenderingLevel				()
				||		CoreFeature						.isRenderingGui					())
				&&		extension						.isAccelerated					()
		) {
			TRANSFORM.set		(pMatrix);
			TRANSFORM.translate	(
					positionX,
					positionY,
					0.0f
			);

			ci			.cancel		();
			extension	.doRender	(
					pItalic
							? italicRenderer
							: normalRenderer,
					null,
					TRANSFORM,
					NORMAL,
					pPackedLight,
					OverlayTexture	.NO_OVERLAY,
					pColor
			);
		}
	}

	@Inject(
			method		= "renderEffect",
			at			= @At("HEAD"),
			cancellable	= true
	)
	public void renderEffectFast(
			BakedGlyph.Effect	effect,
			Matrix4f			matrix,
			VertexConsumer		buffer,
			int					packedLight,
			CallbackInfo		ci
	) {
		var extension = buffer.getAccelerated();

		if (			CoreFeature						.isLoaded						()
				&&		AcceleratedTextRenderingFeature	.isEnabled						()
				&&		AcceleratedTextRenderingFeature	.shouldUseAcceleratedPipeline	()
				&&	(	CoreFeature						.isRenderingLevel				()
				||		CoreFeature						.isRenderingGui					())
				&&		extension						.isAccelerated					()
		) {
			ci			.cancel		();
			extension	.doRender	(
					this,
					effect,
					matrix,
					NORMAL,
					packedLight,
					OverlayTexture	.NO_OVERLAY,
				effect.color()
			);
		}
	}

	@Unique
	@Override
	public void render(
			VertexConsumer		vertexConsumer,
			BakedGlyph.Effect	context,
			Matrix4f			transform,
			Matrix3f			normal,
			int					light,
			int					overlay,
			int					color
	) {
		var extension	= vertexConsumer.getAccelerated	();
		var meshes		= effectMeshes	.get			(context);

		extension.beginTransform(transform, normal);

		if (meshes == null) {
			meshes = new Object2ObjectOpenHashMap<>	();
			effectMeshes.put						(context, meshes);
		}

		var mesh = meshes.get(extension);

		if (mesh != null) {
			mesh.write(
					extension,
					color,
					light,
					overlay
			);

			extension.endTransform();
			return;
		}

		var meshCollector	= new SimpleMeshCollector	(extension.getLayout());
		var meshBuilder		= extension.decorate		(meshCollector);

		var positions = new Vector2f[] {
				new Vector2f(context.x0, context.y0),
				new Vector2f(context.x1, context.y0),
				new Vector2f(context.x1, context.y1),
				new Vector2f(context.x0, context.y1),
		};

		var texCoords = new Vector2f[] {
				new Vector2f(u0, v0),
				new Vector2f(u0, v1),
				new Vector2f(u1, v1),
				new Vector2f(u1, v0),
		};

		for (var i = 0; i < 4; i ++) {
			var position = new Vector3f(positions[i], context.depth);
			var texCoord = texCoords[i];

			meshBuilder.addVertex(
					position.x(),
					position.y(),
					position.z(),
					0xFF_FF_FF_FF,
					texCoord.x(),
					texCoord.y(),
					OverlayTexture.NO_OVERLAY,
					0,
					0.0f,
					0.0f,
					0.0f
			);
		}

		var builder = AcceleratedEntityRenderingFeature
				.getMeshType()
				.getBuilder	();

		mesh = builder.build(
				meshCollector,
				false,
				true,
				0
		);

		meshes	.put	(extension, mesh);
		mesh	.write	(
				extension,
				color,
				light,
				overlay
		);

		extension.endTransform();
	}

	@Unique
	@Override
	public AcceleratedBakedGlyphRenderer getRenderer(boolean italic) {
		return italic
				? italicRenderer
				: normalRenderer;
	}
}
