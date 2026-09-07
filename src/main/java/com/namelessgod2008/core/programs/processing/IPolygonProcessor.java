package com.namelessgod2008.core.programs.processing;

import com.namelessgod2008.core.programs.dispatchers.IPolygonProgramDispatcher;
import com.mojang.blaze3d.vertex.VertexFormat;

public interface IPolygonProcessor {

	IPolygonProgramDispatcher select(VertexFormat.Mode mode);
}
