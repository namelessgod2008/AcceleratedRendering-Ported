package com.namelessgod2008.core.programs.overrides;

public non-sealed interface ITransformOverride extends IProgramOverride {

	void	uploadVarying		(long	varyingAddress,	int offset);
	int		dispatchTransform	(int	vertexCount,	int vertexOffset, int varyingOffset);
	long	getVaryingSize		();
}
