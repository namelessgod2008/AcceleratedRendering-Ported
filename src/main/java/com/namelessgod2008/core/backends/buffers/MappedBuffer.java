package com.namelessgod2008.core.backends.buffers;

import com.namelessgod2008.core.backends.GLConstants;
import lombok.Getter;

import static org.lwjgl.opengl.GL46.*;

@Getter
public class MappedBuffer extends MutableBuffer implements IClientBuffer {

	protected long address;
	protected long position;
	protected long current;

	public MappedBuffer(long initialSize) {
		super(initialSize,	GL_MAP_PERSISTENT_BIT
				| 			GL_MAP_WRITE_BIT
				|			GL_MAP_COHERENT_BIT
		);

		this.address	= map();
		this.position	= 0L;
		this.current	= 0L;
	}

	@Override
	public long reserve(long bytes, boolean occupied) {
		if (bytes <= 0) {
			return address + position;
		}

		var oldPosition = this.position;
		var newPosition = oldPosition + bytes;

		if (occupied) {
			this.current	= oldPosition;
			this.position	= newPosition;
		}

		if (newPosition <= size) {
			return address + oldPosition;
		}

		resize(newPosition);
		return address + oldPosition;
	}

	@Override
	public long reserve(long bytes) {
		return reserve(bytes, true);
	}

	@Override
	public long addressAt(long position) {
		return address + position;
	}

	@Override
	public void beforeExpand() {
		unmap();
	}

	@Override
	public void afterExpand() {
		address = map();
	}

	public void reset() {
		// [临时探针] 统计归零次数：若为 0 而 EXPAND_CALLS 增长，说明
		// clearBuffers 未执行导致 position 跨帧累积 → 每帧反复重建 GL 缓冲。
		com.namelessgod2008.core.AccelStats.RESET_CALLS ++;
		com.namelessgod2008.core.AccelStats.RESET_MAX_POS =
				Math.max(com.namelessgod2008.core.AccelStats.RESET_MAX_POS, position);

		position = 0;
	}

	public long getCurrent() {
		return address + current;
	}

	public boolean overflow(long bytes) {
		return position + bytes >= GLConstants.MAX_SHADER_STORAGE_BLOCK_SIZE;
	}

	public long map() {
		return map(	GL_MAP_WRITE_BIT
				|	GL_MAP_PERSISTENT_BIT
				|	GL_MAP_COHERENT_BIT
		);
	}
}
