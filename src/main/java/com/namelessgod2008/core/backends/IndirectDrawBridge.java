package com.namelessgod2008.core.backends;

import com.mojang.blaze3d.opengl.GlRenderPass;

/**
 * indirect 绘制入口的静态桥接表。
 *
 * <p>为什么不直接用「接口 mixin + instanceof」：Mixin 对 interface 形式的 mixin 有额外校验
 * （{@code @Mixin target type mismatch: ... is not an interface}），把接口注入普通类
 * 需要特定版本的宽松行为，实测在本项目的 Mixin 0.8.7 下无法通过。
 * 故改用最朴素的静态注册：{@code GlCommandEncoderMixin} 在类初始化时把自身登记进来，
 * 调用方按实例查表即可，无反射、无接口依赖。
 */
public final class IndirectDrawBridge {

	/** indirect 绘制执行器。 */
	public interface Executor {

		/**
		 * 在给定 pass 内执行一次 indirect 绘制。
		 *
		 * @param commandOffset indirect 命令在 {@code GL_DRAW_INDIRECT_BUFFER} 中的字节偏移
		 */
		void drawElementsIndirect(GlRenderPass pass, long commandOffset);
	}

	private static volatile Executor executor;

	private IndirectDrawBridge() {
	}

	/** 由 {@code GlCommandEncoderMixin} 的静态初始化块登记。 */
	public static void register(Executor newExecutor) {
		executor = newExecutor;
	}

	/** 取执行器；mixin 未生效时为 null。 */
	public static Executor get() {
		return executor;
	}
}
