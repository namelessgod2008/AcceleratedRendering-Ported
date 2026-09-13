package com.namelessgod2008.core.buffers.accelerated.draw.pools;

import com.mojang.blaze3d.vertex.VertexFormat;

import com.namelessgod2008.core.backends.buffers.IServerBuffer;

public interface IElementPool {

	void			reset		();
	void			delete		();
	void			prepare		();
	void			bindBuffer	();
	boolean			isResized	();
	IServerBuffer	getBuffer	();
	IElementSegment get			();

	interface IElementSegment {

		long	getCount	();
		void	setup		();
		void	count		(int count);

		/**
		 * 26.1: 索引模式与图元类型相关（QUADS 需三角化索引，见 RenderSystem 的
		 * sharedSequentialQuad），而 IElementSegment 自身无从得知 mode，故由绘制侧注入。
		 */
		default void setMode(VertexFormat.Mode mode) {

		}
	}
}
