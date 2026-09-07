package com.namelessgod2008.core.programs.culling;

import net.minecraft.client.renderer.RenderType;

public interface ICullingProgramSelector {

	ICullingProgramDispatcher select(RenderType renderType);
}
