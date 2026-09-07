package com.namelessgod2008.core.meshes.collectors;

import com.namelessgod2008.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;

public enum MeshCollectorType {

	CULLED,
	SIMPLE;

	public IMeshCollector create(IAcceleratedVertexConsumer consumer) {
		return create(this, consumer);
	}

	public static IMeshCollector create(MeshCollectorType collectorType, IAcceleratedVertexConsumer consumer) {
		return switch (collectorType) {
			case CULLED -> new CulledMeshCollector(consumer);
			case SIMPLE -> new SimpleMeshCollector(consumer.getLayout());
		};
	}
}
