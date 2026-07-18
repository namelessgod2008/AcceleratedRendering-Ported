package com.namelessgod2008.core.meshes.collectors;

import com.namelessgod2008.core.buffers.memory.VertexLayout;
import com.namelessgod2008.core.meshes.data.MeshData;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;

public interface IMeshCollector extends VertexConsumer {

	MeshData			getData			();
	ByteBufferBuilder	getBuffer		();
	VertexLayout		getLayout		();
	long				getVertexCount	();
	void				flush			();
}
