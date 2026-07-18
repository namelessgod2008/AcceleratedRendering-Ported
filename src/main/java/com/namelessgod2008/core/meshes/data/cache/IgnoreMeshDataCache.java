package com.namelessgod2008.core.meshes.data.cache;

import com.namelessgod2008.core.buffers.memory.VertexLayout;
import com.namelessgod2008.core.meshes.IMesh;
import com.namelessgod2008.core.meshes.data.MeshData;

public class IgnoreMeshDataCache implements IMeshDataCache {

	@Override
	public void set(
			VertexLayout	layout,
			MeshData		data,
			IMesh			mesh
	) {

	}

	@Override
	public IMesh get(VertexLayout layout, MeshData meshData) {
		return null;
	}

	@Override
	public int count(VertexLayout layout, MeshData data) {
		return 0;
	}

	@Override
	public void reload() {

	}
}
