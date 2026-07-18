package com.namelessgod2008.core.programs.overrides;

public non-sealed interface IUploadingOverride extends IProgramOverride {

	void	uploadMeshInfo		(long	meshInfoAddress,	int	meshInfoIndex);
	int		dispatchUploading	(int	meshCount,			int	meshSize, int meshOffset);
	long	getMeshInfoSize		();
}
