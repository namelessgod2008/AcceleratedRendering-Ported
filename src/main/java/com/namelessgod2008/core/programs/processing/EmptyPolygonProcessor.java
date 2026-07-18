package com.namelessgod2008.core.programs.processing;

import com.namelessgod2008.core.programs.dispatchers.EmptyProgramDispatcher;
import com.namelessgod2008.core.programs.dispatchers.IPolygonProgramDispatcher;
import com.mojang.blaze3d.vertex.VertexFormat;

public class EmptyPolygonProcessor implements IPolygonProcessor {

	public static final EmptyPolygonProcessor INSTANCE = new EmptyPolygonProcessor();

	@Override
	public IPolygonProgramDispatcher select(VertexFormat.Mode mode) {
		return EmptyProgramDispatcher.INSTANCE;
	}
}
