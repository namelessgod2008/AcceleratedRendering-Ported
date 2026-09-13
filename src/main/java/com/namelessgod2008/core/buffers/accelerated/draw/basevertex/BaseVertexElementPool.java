package com.namelessgod2008.core.buffers.accelerated.draw.basevertex;

import com.namelessgod2008.core.backends.buffers.IServerBuffer;
import com.namelessgod2008.core.backends.buffers.MutableBuffer;
import com.namelessgod2008.core.buffers.accelerated.draw.pools.IElementPool;
import com.namelessgod2008.core.utils.SimpleResetPool;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.Getter;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL46.*;

/**
 * 26.1: 原版 AutoStorageIndexBuffer 的顺序索引已抽象为 GpuBuffer（无裸 GL handle），
 * 无法用于 mod 的索引绘制，故此处自建。
 *
 * 索引语义必须与 MC 一致：26.1 的 VertexFormat.Mode.QUADS 以 GL_TRIANGLES 图元绘制
 * （见 GlConst.toGl(Mode)：QUADS → 4 = GL_TRIANGLES），因此每个 quad（4 顶点）需要
 * 6 个三角化索引 [v, v+1, v+2, v, v+2, v+3]。若按线性索引提交，每 3 个跨面顶点会被
 * 凑成一个三角形，表现为模型碎裂成杂乱三角形。
 *
 * 所有 draw 都从索引偏移 0 读取自身 count 个索引，配合 drawIndexed 的 baseVertex
 * （顶点全局偏移）引用顶点，因此索引缓冲只需保存一份规范前缀模式。
 */
@Getter
public class BaseVertexElementPool extends SimpleResetPool<BaseVertexElementPool.ElementSegment, Void> implements IElementPool {

	/** 一个 quad 的顶点数 */
	private static final int QUAD_VERTEX_COUNT	= 4;
	/** 一个 quad 的三角化索引数 */
	private static final int QUAD_INDEX_COUNT	= 6;

	private			final	MutableBuffer	indexBuffer;
	/** 已物化的索引数（单调增长；每帧各 draw 复用同一份前缀） */
	@Getter		private			long			totalIndexCount;

	public BaseVertexElementPool(int size) {
		super(size, null);

		this.indexBuffer		= new MutableBuffer(Math.max(size * 6L * Integer.BYTES, 256), GL_DYNAMIC_STORAGE_BIT);
		this.totalIndexCount	= 0L;
	}

	@Override
	public void bindBuffer() {
		indexBuffer.bind(GL_ELEMENT_ARRAY_BUFFER);
	}

	@Override
	public void prepare() {
		// 26.1: 规范索引前缀跨帧复用，无需每帧重置
	}

	@Override
	public void delete() {
		indexBuffer.delete();
	}

	@Override
	protected ElementSegment create(Void value, int i) {
		return new ElementSegment(this);
	}

	@Override
	protected void reset(ElementSegment elementSegment) {
		elementSegment.reset();
	}

	@Override
	protected void delete(ElementSegment elementSegment) {

	}

	@Override
	public IServerBuffer getBuffer() {
		return indexBuffer;
	}

	@Override
	public boolean isResized() {
		return false;
	}

	private VertexFormat.Mode mode = VertexFormat.Mode.TRIANGLES;

	/** 26.1: 由绘制侧注入图元类型，用于选择索引模式 */
	public void setMode(VertexFormat.Mode mode) {
		if (mode != null) {
			this.mode = mode;
		}
	}

	/**
	 * 物化索引模式到 target 个索引。
	 *
	 * 模式与 MC 原版一致（对照 RenderSystem 的 sharedSequentialQuad / sharedSequentialLines）：
	 * - QUADS：每 4 顶点 → 6 索引 [v, v+1, v+2, v+2, v+3, v]（vertexStride=4, indexStride=6）
	 * - LINES：每 4 顶点 → 6 索引 [v, v+1, v+2, v+3, v+2, v+1]
	 * - 其余：1:1 线性
	 *
	 * @param target    需要的索引总数（该 draw 的累计索引数）
	 * @param increment 本次新增的索引数
	 */
	protected void ensureIndices(long target, int increment) {
		if (target <= totalIndexCount) {
			return;
		}

		long neededBytes = target * Integer.BYTES;

		if (neededBytes > indexBuffer.getSize()) {
			indexBuffer.resize(neededBytes);
		}

		int vertexStride;
		int indexStride;

		switch (mode) {
			case QUADS -> {
				vertexStride	= 4;
				indexStride		= 6;
			}
			case LINES -> {
				vertexStride	= 4;
				indexStride		= 6;
			}
			default -> {
				vertexStride	= 1;
				indexStride		= 1;
			}
		}

		boolean strided = indexStride > 1;

		long from	= totalIndexCount;
		int  count	= (int) (target - from);
		java.nio.ByteBuffer memory = MemoryUtil.memAlloc(count * Integer.BYTES);

		try {
			// 相对写入（putInt(value)），避免绝对索引在字节/元素单位之间混淆
			long index	= from;
			// 顶点游标：strided 模式下前 from 个索引引用 from/indexStride*vertexStride 个顶点
			long vertex	= strided
					? from / indexStride * vertexStride
					: from;

			if (strided) {
				boolean quad = mode == VertexFormat.Mode.QUADS;

				while (index + indexStride <= target) {
					if (quad) {
						memory.putInt((int) (vertex + 0));
						memory.putInt((int) (vertex + 1));
						memory.putInt((int) (vertex + 2));
						memory.putInt((int) (vertex + 2));
						memory.putInt((int) (vertex + 3));
						memory.putInt((int) (vertex + 0));
					} else {
						memory.putInt((int) (vertex + 0));
						memory.putInt((int) (vertex + 1));
						memory.putInt((int) (vertex + 2));
						memory.putInt((int) (vertex + 3));
						memory.putInt((int) (vertex + 2));
						memory.putInt((int) (vertex + 1));
					}

					index	+= indexStride;
					vertex	+= vertexStride;
				}
			}

			// 余数按线性补齐
			while (index < target) {
				memory.putInt((int) vertex);

				index	++;
				vertex	++;
			}

			memory.flip();
			indexBuffer.data(from * Integer.BYTES, memory.remaining(), MemoryUtil.memAddress(memory));
		} finally {
			MemoryUtil.memFree(memory);
		}

		totalIndexCount = target;
	}

	public static class ElementSegment implements IElementSegment {

		private final BaseVertexElementPool	pool;
		private			long					count;

		public ElementSegment(BaseVertexElementPool pool) {
			this.pool	= pool;
			this.count	= 0L;
		}

		private void reset() {
			count = 0L;
		}

		@Override
		public void setup() {

		}

		@Override
		public long getCount() {
			return count;
		}

		@Override
		public void setMode(VertexFormat.Mode mode) {
			this.pool.setMode(mode);
		}

		@Override
		public void count(int count) {
			this.count += count;

			this.pool.ensureIndices(this.count, count);
		}
	}
}