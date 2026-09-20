package com.namelessgod2008.compat.iris.interfaces;

/**
 * Iris 扩展顶点数据的写入接口。
 *
 * <p>见 {@code com.namelessgod2008.compat.iris.mixins.acceleratedrendering.SimpleMeshCollectorMixin}
 * 的类注释 —— 说明为什么 {@code mc_midTexCoord} 必须在网格收集阶段填充。
 */
public interface IIrisMeshCollector {

	/**
	 * 为「刚刚写入的最后一个多边形」回填 {@code mc_midTexCoord}。
	 *
	 * <p>调用者必须保证：自上一次调用本方法（或自第一次 {@code addVertex}）以来，
	 * 恰好写入了完整的一个多边形（{@code polygonSize} 个顶点）。
	 *
	 * @param midU 该多边形所有顶点 UV 的 u 分量算术平均
	 * @param midV 该多边形所有顶点 UV 的 v 分量算术平均
	 */
	void setIrisMidTexCoord(float midU, float midV);
}
