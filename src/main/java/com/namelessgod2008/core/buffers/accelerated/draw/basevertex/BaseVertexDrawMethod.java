package com.namelessgod2008.core.buffers.accelerated.draw.basevertex;

import com.namelessgod2008.core.buffers.accelerated.draw.IDrawMethod;
import com.namelessgod2008.core.buffers.accelerated.draw.pools.IDrawContextPool;
import com.namelessgod2008.core.buffers.accelerated.draw.pools.IElementPool;
import com.namelessgod2008.core.programs.culling.EmptyCullingProgramSelector;
import com.namelessgod2008.core.programs.culling.ICullingProgramSelector;
import com.mojang.blaze3d.vertex.VertexFormat;

public class BaseVertexDrawMethod implements IDrawMethod {

	public static final BaseVertexDrawMethod INSTANCE = new BaseVertexDrawMethod();

	@Override
	public ICullingProgramSelector getCullingProgramSelector(VertexFormat vertexFormat) {
		return EmptyCullingProgramSelector.INSTANCE;
	}

	@Override
	public IDrawContextPool getDrawContextPool(int size) {
		return new BaseVertexDrawContextPool(size);
	}

	@Override
	public IElementPool getElementPool(int size) {
		return new BaseVertexElementPool(size);
	}
}
