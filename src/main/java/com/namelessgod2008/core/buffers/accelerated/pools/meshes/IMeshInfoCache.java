package com.namelessgod2008.core.buffers.accelerated.pools.meshes;

public interface IMeshInfoCache {

	void	reset			();
	void	delete			();
	void	setup			(int color, int light, int overlay, int sharing, int shouldCull);
	int		getMeshCount	();
	int		getSharing		(int i);
	int		getShouldCull	(int i);
	int		getColor		(int i);
	int		getLight		(int i);
	int		getOverlay		(int i);
}
