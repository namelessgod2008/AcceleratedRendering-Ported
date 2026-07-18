package com.namelessgod2008.core.meshes;

import com.namelessgod2008.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;

public class EmptyMesh implements IMesh {

	public static final EmptyMesh INSTANCE = new EmptyMesh();

	@Override
	public void write(
			IAcceleratedVertexConsumer	extension,
			int							color,
			int							light,
			int							overlay
	) {

	}
}
