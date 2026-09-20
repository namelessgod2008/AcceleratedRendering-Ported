package com.namelessgod2008.core.backends;

/**
 * 「把绘制输出重定向到光影 gbuffer」的静态桥接表。
 *
 * <p><b>为什么需要桥接</b>：架构约束是 {@code core} 不依赖 {@code compat}
 * （见 CLAUDE.md：mod 兼容代码一律放 {@code compat/<mod名>/}）。但重定向的调用点在
 * {@code core} 的 {@code AcceleratedBufferSource.drawBuffers()} 里，而实现依赖
 * Iris 的 {@code IrisRenderingPipeline.bindDefault()}（外部 mod 类）。
 * 故用与 {@link IndirectDrawBridge} 相同的模式：core 定义接口，
 * {@code compat/iris} 在初始化时把实现登记进来。
 *
 * <p>未装光影时执行器为 null，调用方按「无需重定向」处理即可，零开销。
 *
 * <p>背景见 {@code compat/iris/IrisGbufferBridge}：加速绘制走 26.1 的
 * {@code createRenderPass} 自建 pass，会绕过 Iris 的 gbuffer 重定向，
 * 导致几何画到主渲染目标、不被 composite 采样（表现为开光影后实体消失）。
 */
public final class GbufferBridge {

	/** gbuffer 重定向执行器。返回 true 表示已重定向。 */
	public interface Redirector {

		/**
		 * 把当前 FBO 切到光影的 gbuffer 并按 gbuffer 尺寸重设视口。
		 *
		 * <p>必须在 RenderPass 已打开、尚未执行任何绘制时调用。
		 *
		 * @return true 表示已重定向；false 表示当前无光影，无需处理
		 */
		boolean redirectToGbuffer();
	}

	private static volatile Redirector redirector;

	private GbufferBridge() {
	}

	/** 由 {@code compat/iris} 的 mixin 静态初始化块登记。 */
	public static void register(Redirector newRedirector) {
		redirector = newRedirector;
	}

	/** 取执行器；未装光影或 mixin 未生效时为 null。 */
	public static Redirector get() {
		return redirector;
	}
}
