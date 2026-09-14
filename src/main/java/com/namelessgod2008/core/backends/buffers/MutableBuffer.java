package com.namelessgod2008.core.backends.buffers;

import com.namelessgod2008.core.utils.MutableSize;

public class MutableBuffer extends MutableSize implements IServerBuffer {

	private final	int				bits;

	protected		ImmutableBuffer glBuffer;

	public MutableBuffer(long initialSize, int bits) {
		super(initialSize);

		this.bits		= bits;
		this.glBuffer	= new ImmutableBuffer(this.size, bits);
	}

	@Override
	public void doExpand(long size, long bytes) {
		// [临时探针] 统计 GL 缓冲重建（新建+全量拷贝+删旧+重建映射）
		long tProbe = System.nanoTime();

		var newSize		= size + bytes;
		var newBuffer	= new ImmutableBuffer(newSize, bits);

		glBuffer.copyTo(newBuffer, size);
		glBuffer.delete();
		glBuffer = newBuffer;

		com.namelessgod2008.core.AccelStats.EXPAND_CALLS	++;
		com.namelessgod2008.core.AccelStats.EXPAND_BYTES	+= size;
		com.namelessgod2008.core.AccelStats.EXPAND_NANOS	+= System.nanoTime() - tProbe;

		// [临时探针] 自动捕获故障：进世界 30 秒后仍在重建 GL 缓冲 = 异常
		// （正常情况扩容只在预热期发生，之后应恒为 0）
		long probeNow = System.nanoTime();
		if (		com.namelessgod2008.core.AccelStats.LEVEL_START != 0L
				&&	probeNow - com.namelessgod2008.core.AccelStats.FIRST_FRAME > 30_000_000_000L
				&&	probeNow - com.namelessgod2008.core.AccelStats.LAST_EXPAND_LOG > 2_000_000_000L
		) {
			com.namelessgod2008.core.AccelStats.LAST_EXPAND_LOG = probeNow;
			System.out.println(
					"[AR-ANOMALY] 进世界 30s 后仍在重建 GL 缓冲！"
					+ " oldSize=" + (size / 1024L) + "KB"
					+ " newSize=" + (newSize / 1024L) + "KB"
					+ " 累计次数=" + com.namelessgod2008.core.AccelStats.EXPAND_CALLS
					+ " 累计拷贝=" + (com.namelessgod2008.core.AccelStats.EXPAND_BYTES / 1024L / 1024L) + "MB"
					+ " resets=" + com.namelessgod2008.core.AccelStats.RESET_CALLS
					+ " resetMaxPos=" + (com.namelessgod2008.core.AccelStats.RESET_MAX_POS / 1024L) + "KB"
			);
		}
	}

	public long map(int flags) {
		return glBuffer.map(size, flags);
	}

	public void unmap() {
		glBuffer.unmap();
	}

	@Override
	public int getBufferHandle() {
		return glBuffer.getBufferHandle();
	}

	@Override
	public void delete() {
		glBuffer.delete();
	}

	@Override
	public void bind(int target) {
		glBuffer.bind(target);
	}

	@Override
	public void bindBase(int target, int index) {
		glBuffer.bindBase(target, index);
	}

	@Override
	public void bindRange(
			int		target,
			int		index,
			long	offset,
			long	size
	) {
		glBuffer.bindRange(
				target,
				index,
				offset,
				size
		);
	}

	@Override
	public void data(
			long offset,
			long size,
			long address
	) {
		glBuffer.data(
				offset,
				size,
				address
		);
	}
}
