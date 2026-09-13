package com.namelessgod2008.core.buffers.accelerated.draw.pools;

import com.namelessgod2008.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import com.namelessgod2008.core.buffers.accelerated.draw.pools.IElementPool.IElementSegment;
import com.namelessgod2008.core.backends.buffers.IServerBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.renderer.rendertype.RenderType;

public interface IDrawContextPool {

	void			reset	();
	void			delete	();
	void			setup	();
	IDrawContext	get		();

	interface IDrawContext extends Comparable<IDrawContext> {

		void		setupContext	(AcceleratedBufferBuilder	builder, IElementSegment elementSegment, IServerBuffer elementBuffer, RenderType renderType);
		void		drawElements	(Mode						mode);
		RenderType	getRenderType	();

		/**
		 * 26.1: 绘制前的准备阶段，【必须在任何 RenderPass 打开之前】调用。
		 *
		 * 26.1 的 CommandEncoder 规定：存在打开的 render pass 时不得执行其它命令
		 * （否则抛 "Close the existing render pass before performing additional commands"）。
		 * 而解析纹理（可能触发懒加载上传 → writeToTexture）与写入 DynamicTransforms
		 * （mapBuffer）都属于此类命令，故二者必须在开 pass 前完成。
		 */
		default void prepareDraw() {

		}

		/**
		 * 26.1: 在外层按输出目标共享的 RenderPass 内绘制（prepareDraw 之后调用）。
		 * 实现应只做不触发命令编码的状态绑定与 draw 调用。
		 */
		default void drawElements(RenderPass pass, Mode mode) {
			drawElements(mode);
		}

		/** 26.1: 该 draw 的输出目标，用于把 draw 分组到同一个 RenderPass */
		default RenderTarget getRenderTarget() {
			return getRenderType().outputTarget().getRenderTarget();
		}
	}
}