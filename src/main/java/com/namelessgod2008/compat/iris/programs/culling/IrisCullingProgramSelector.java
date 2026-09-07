package com.namelessgod2008.compat.iris.programs.culling;

import com.namelessgod2008.compat.iris.IrisCompatFeature;
import com.namelessgod2008.core.programs.culling.ICullingProgramDispatcher;
import com.namelessgod2008.core.programs.culling.ICullingProgramSelector;
import com.namelessgod2008.core.utils.RenderTypeUtils;
import com.namelessgod2008.features.culling.OrientationCullingFeature;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class IrisCullingProgramSelector implements ICullingProgramSelector {

	private			final ICullingProgramSelector	parent;
	private			final ICullingProgramDispatcher	quadDispatcher;
	private			final ICullingProgramDispatcher	triangleDispatcher;

	public IrisCullingProgramSelector(
			ICullingProgramSelector	parent,
			ResourceLocation		quadProgramKey,
			ResourceLocation		triangleProgramKey
	) {
		this.parent				= parent;
		this.quadDispatcher		= new IrisCullingProgramDispatcher(VertexFormat.Mode.QUADS,		quadProgramKey);
		this.triangleDispatcher	= new IrisCullingProgramDispatcher(VertexFormat.Mode.TRIANGLES,	triangleProgramKey);
	}

	@Override
	public ICullingProgramDispatcher select(RenderType renderType) {
		if (			IrisCompatFeature			.isEnabled					()
				&&		IrisCompatFeature			.isIrisCompatCullingEnabled	()
				&&	(	IrisCompatFeature			.isShadowCullingEnabled		()	|| !	ShadowRenderingState.areShadowsCurrentlyBeingRendered())
				&&		OrientationCullingFeature	.isEnabled					()
				&&	(	OrientationCullingFeature	.shouldIgnoreCullState		()	|| 		RenderTypeUtils		.isCulled(renderType))
		) {
			return switch (renderType.mode) {
				case QUADS		-> quadDispatcher;
				case TRIANGLES	-> triangleDispatcher;
				default			-> parent.select(renderType);
			};
		}

		return parent.select(renderType);
	}
}
