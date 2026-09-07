package com.namelessgod2008.core.buffers.accelerated.draw.pools;

import com.namelessgod2008.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import com.namelessgod2008.core.buffers.accelerated.draw.pools.IElementPool.IElementSegment;
import com.namelessgod2008.core.backends.buffers.IServerBuffer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.renderer.RenderType;

public interface IDrawContextPool {

	void			reset	();
	void			delete	();
	void			setup	();
	IDrawContext	get		();

	interface IDrawContext extends Comparable<IDrawContext> {

		void		setupContext	(AcceleratedBufferBuilder	builder, IElementSegment elementSegment, IServerBuffer elementBuffer, RenderType renderType);
		void		drawElements	(Mode						mode);
		RenderType	getRenderType	();
	}
}
