package com.namelessgod2008.core.programs.overrides;

import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.Map;

public interface IShaderProgramOverrides {

	ProgramOverride	getOverride	(RenderType	renderType);
	ProgramOverride	getOverride	(int		overrideId);
	int				getCount	();
}
