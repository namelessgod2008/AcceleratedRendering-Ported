package com.namelessgod2008.features.items.gui;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.util.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import net.minecraft.util.TriState;

import java.util.function.Function;

public class GuiRenderTypes extends RenderType {

	public static final ShaderStateShard POSITION_TEX_COLOR_SHADER = POSITION_TEXTURE_COLOR_SHADER;

	public static final Function<Identifier, RenderType> BLIT = Util.memoize(atlasLocation -> create(
				"acceleratedrendering:blit",
				DefaultVertexFormat	.POSITION_TEX_COLOR,
				VertexFormat.Mode	.QUADS,
				256,
				false,
				false,
				CompositeState
						.builder()
						.setTextureState		(new TextureStateShard(atlasLocation, TriState.FALSE, false))
						.setShaderState			(POSITION_TEX_COLOR_SHADER)
						.setDepthTestState		(LEQUAL_DEPTH_TEST)
						.setTransparencyState	(TRANSLUCENT_TRANSPARENCY)
						.createCompositeState	(false)
	));

	private GuiRenderTypes(
			String				name,
			VertexFormat		format,
			VertexFormat.Mode	formatMode,
			int					bufferSize,
			boolean				affectsCrumbling,
			boolean				sortOnUpload,
			Runnable			setupState,
			Runnable			clearState
	) {
		super(
				name,
				format,
				formatMode,
				bufferSize,
				affectsCrumbling,
				sortOnUpload,
				setupState,
				clearState
		);
	}

	public static RenderType blit(Identifier atlasLocation) {
		return BLIT.apply(atlasLocation);
	}
}
