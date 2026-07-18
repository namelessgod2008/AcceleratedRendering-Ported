package com.namelessgod2008.core.meshes.data.cache;

import com.namelessgod2008.core.buffers.memory.VertexLayout;
import com.namelessgod2008.core.meshes.IMesh;
import com.namelessgod2008.core.meshes.data.MeshData;

public interface IMeshDataCache {

	void	reload	();
	void	set		(VertexLayout layout, MeshData data, IMesh mesh);
	IMesh	get		(VertexLayout layout, MeshData data);
	int		count	(VertexLayout layout, MeshData data);
}
