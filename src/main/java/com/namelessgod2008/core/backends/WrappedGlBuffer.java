package com.namelessgod2008.core.backends;

import com.mojang.blaze3d.opengl.GlBuffer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 26.1: 把 mod 已持有的 GL buffer（handle）包装为 26.1 的 GpuBuffer，供 RenderPass 绘制使用。
 *
 * 采用「继承 GlBuffer + 覆盖 close()」的硬编码方式（不使用反射）：
 * - isClosed() 保持 false —— 满足 26.1 绘制前的校验。若为 true，26.1 会以
 *   "buffer has been closed" 拒绝绘制；而回退方案（反射置 closed=true）正是因此导致
 *   实体不显示且每帧抛异常、帧率骤降。
 * - close() 覆盖为空实现 —— 该 GL buffer 的生命周期由 mod 自己管理，不能被 26.1 释放。
 *
 * ⚠️ 必须按 handle 复用实例（见 {@link #of}）：26.1 的 VertexArrayCache 用
 * `lastVertexBuffer != vertexBuffer` 判断是否需要重新绑定顶点缓冲。若每次绘制都新建
 * 包装对象，该判断恒为真 → 每次 draw 都重新绑定缓冲/重设顶点属性，帧率骤降。
 */
public class WrappedGlBuffer extends GlBuffer {

	private static final Map<Integer, WrappedGlBuffer> CACHE = new ConcurrentHashMap<>();

	public WrappedGlBuffer(int handle, int usage, long size) {
		super(
				() -> "acceleratedrendering_wrapped",
				null,
				usage,
				size,
				handle,
				null
		);
	}

	/**
	 * 按 GL handle 复用包装对象。同一 handle 始终返回同一实例，使 26.1 的 VAO 缓存得以命中。
	 *
	 * @param handle GL buffer handle；&lt;= 0 时返回 null
	 * @param usage  GpuBuffer.USAGE_*（mod 中同一 buffer 用途固定）
	 * @param size   缓冲字节数
	 */
	public static WrappedGlBuffer of(int handle, int usage, long size) {
		if (handle <= 0) {
			return null;
		}

		return CACHE.computeIfAbsent(handle, h -> new WrappedGlBuffer(h, usage, size));
	}

	/** 资源重载/缓冲重建后清理缓存（避免废弃 handle 条目长期驻留）。 */
	public static void clearCache() {
		CACHE.clear();
	}

	@Override
	public void close() {
		// 由 mod 拥有该 GL buffer 的生命周期，此处不释放
	}
}