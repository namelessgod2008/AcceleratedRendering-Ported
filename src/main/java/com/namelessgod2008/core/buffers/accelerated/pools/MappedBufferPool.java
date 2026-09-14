package com.namelessgod2008.core.buffers.accelerated.pools;

import com.namelessgod2008.core.backends.buffers.MappedBuffer;
import com.namelessgod2008.core.utils.SimpleResetPool;

public class MappedBufferPool extends SimpleResetPool<MappedBufferPool.Pooled, Void> {

	public MappedBufferPool() {
		super(16, null);
	}

	@Override
	protected Pooled create(Void context, int i) {
		return new Pooled();
	}

	@Override
	protected void reset(Pooled pooled) {
		pooled.poolReset();
	}

	@Override
	protected void delete(Pooled pooled) {
		pooled.poolDelete();
	}

	@Override
	protected Pooled fail(boolean force) {
		expand();
		return get();
	}

	public static class Pooled extends MappedBuffer {

		/**
		 * 26.1 性能修复：初始容量由 64 字节提高到 64KB。
		 *
		 * 该池用于 meshInfo 缓冲，单次 upload 的数据量为「实例数 × 7 × 4 字节」——
		 * 实测大场景下单次约 46KB。原初始值 64B 会让每个池对象首次使用时触发 resize，
		 * 而 MappedBuffer.resize 的实现是「新建 ImmutableBuffer + 拷贝 + 删除旧 buffer
		 * + 重建持久映射」，即每次 resize 都要重建整块 GL 资源（实测 11 个对象累计 27ms/帧）。
		 * 提高初始容量后，绝大多数情况不再触发 resize。
		 */
		public Pooled() {
			super(65536L);
		}

		@Override
		public void delete() {
			throw new IllegalStateException("Pooled buffers cannot be deleted directly.");
		}

		@Override
		public void reset() {
			throw new IllegalStateException("Pooled buffers cannot be reset directly.");
		}

		private void poolDelete() {
			super.delete();
		}

		private void poolReset() {
			super.reset();
		}
	}
}
