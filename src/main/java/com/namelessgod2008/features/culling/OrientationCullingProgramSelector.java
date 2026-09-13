package com.namelessgod2008.features.culling;

import com.namelessgod2008.core.programs.culling.ICullingProgramDispatcher;
import com.namelessgod2008.core.programs.culling.ICullingProgramSelector;
import com.namelessgod2008.core.utils.RenderTypeUtils;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

public class OrientationCullingProgramSelector implements ICullingProgramSelector {

	private final ICullingProgramSelector	parent;
	private final ICullingProgramDispatcher	quadDispatcher;
	private final ICullingProgramDispatcher	triangleDispatcher;

	public OrientationCullingProgramSelector(
			ICullingProgramSelector	parent,
			Identifier		quadProgramKey,
			Identifier		triangleProgramKey
	) {
		this.parent				= parent;
		this.quadDispatcher		= new OrientationCullingProgramDispatcher(VertexFormat.Mode.QUADS,		quadProgramKey);
		this.triangleDispatcher	= new OrientationCullingProgramDispatcher(VertexFormat.Mode.TRIANGLES,	triangleProgramKey);
	}

	@Override
	public ICullingProgramDispatcher select(RenderType renderType) {
		if (			OrientationCullingFeature	.isEnabled				()
				&&	(	OrientationCullingFeature	.shouldIgnoreCullState	() || RenderTypeUtils.isCulled(renderType))
		) {
			return switch (renderType.mode()) {
				case QUADS		-> quadDispatcher;
				case TRIANGLES	-> triangleDispatcher;
				default			-> parent.select(renderType);
			};
		}

		return parent.select(renderType);
	}
}
