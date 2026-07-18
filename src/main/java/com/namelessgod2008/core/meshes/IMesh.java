package com.namelessgod2008.core.meshes;

import com.namelessgod2008.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.namelessgod2008.core.meshes.collectors.IMeshCollector;

public interface IMesh {

	void write(IAcceleratedVertexConsumer extension, int color, int light, int overlay);

	interface Builder {

		IMesh	build	(IMeshCollector collector);
		IMesh	build	(IMeshCollector collector, boolean forceDense);
		IMesh	build	(IMeshCollector collector, boolean forceDense, int		meshLayer);
		IMesh	build	(IMeshCollector collector, boolean forceDense, boolean	reloadSensitive, int meshLayer);
		void	delete	();
		void	reload	();
	}
}
