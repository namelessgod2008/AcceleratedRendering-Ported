package com.namelessgod2008.core.buffers.accelerated.draw;

import com.namelessgod2008.core.buffers.accelerated.draw.pools.IDrawContextPool;
import com.namelessgod2008.core.buffers.accelerated.draw.pools.IElementPool;
import com.namelessgod2008.core.programs.culling.ICullingProgramSelector;
import com.mojang.blaze3d.vertex.VertexFormat;

public interface IDrawMethod {

	ICullingProgramSelector	getCullingProgramSelector	(VertexFormat	vertexFormat);
	IDrawContextPool		getDrawContextPool			(int			size);
	IElementPool			getElementPool				(int			size);
}
