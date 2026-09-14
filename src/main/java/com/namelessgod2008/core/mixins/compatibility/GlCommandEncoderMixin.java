package com.namelessgod2008.core.mixins.compatibility;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlBuffer;
import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlRenderPass;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.namelessgod2008.core.backends.IndirectDrawBridge;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL40;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Collections;

import static org.lwjgl.opengl.GL30.GL_ELEMENT_ARRAY_BUFFER;

/**
 * 26.1 的 {@code RenderPass} 体系没有 indirect draw 入口
 * （{@code RenderPassBackend} 只有 {@code draw} / {@code drawIndexed} / {@code drawMultipleIndexed}），
 * vanilla 自身也完全不用 {@code glDrawElementsIndirect}。
 *
 * <p>本 mod 的 INDIRECT 路径依赖 GPU 侧生成的索引与 draw count（compute shader 经 atomic counter
 * 写入），必须走真正的 indirect draw；{@code drawMultipleIndexed} 无法替代
 * ——它只是 CPU 侧逐个 {@code drawFromBuffers}，draw count 仍由 CPU 决定。
 *
 * <p>本 mixin 向 {@link IndirectDrawBridge} 登记一个执行器（而非用接口 mixin：
 * Mixin 对 interface 形式有 {@code target type mismatch: ... is not an interface} 校验，
 * 无法把接口注入普通类），执行流程为：
 * <ol>
 *   <li>调用原版私有 {@code trySetup}，把 pipeline / uniform / 纹理 / scissor 真正刷到 GL
 *       ——【不可省略】：{@code trySetup} 只在 {@code executeDraw} / {@code executeDrawMultiple}
 *       内部被调用，直接调裸 GL 会导致 shader、纹理全未绑定；</li>
 *   <li>按 {@code drawFromBuffers} 的方式绑定 VAO 与索引缓冲；</li>
 *   <li>调用 {@code glDrawElementsIndirect}（命令缓冲由调用方在此之前绑定）。</li>
 * </ol>
 *
 * <p>裸 GL 与命令编码器可共存：{@code GlCommandEncoder} 内部没有「pass 已打开则禁止其它操作」
 * 的守卫（已核对全部 throw 点），该限制存在于 {@code mapBuffer} / {@code writeToTexture} 等
 * 需要另起命令的函数。indirect draw 只是提交绘制命令，与当前 pass 目标一致。
 *
 * <p>所需的 {@code GlRenderPass} 字段访问经 access widener 开放（见
 * {@code acceleratedrendering.accesswidener}）。
 */
@Mixin(GlCommandEncoder.class)
public abstract class GlCommandEncoderMixin {

	@Shadow private com.mojang.blaze3d.opengl.GlDevice device;

	/**
	 * 原版私有方法：把 pass 上累积的状态刷到 GL，返回是否可绘制。
	 *
	 * <p>注意：{@code @Invoker} 代理方法必须声明为【实例方法且 abstract】，被调用时 {@code this}
	 * 即目标类实例。写成 static 并把 {@code GlCommandEncoder} 作为首个参数，会被 Mixin 当作
	 * 目标方法签名的一部分去匹配，从而报
	 * {@code No candidates were found matching trySetup(...)}；
	 * 带方法体则会报 {@code @Invoker method ... is not abstract}。
	 */
	@Invoker("trySetup")
	abstract boolean invokeTrySetup(GlRenderPass renderPass, Collection<String> dynamicUniforms);

	/** 登记 indirect 绘制执行器（encoder 实例本身即执行体）。 */
	@Inject(method = "<init>", at = @At("TAIL"))
	private void registerIndirectExecutor(CallbackInfo ci) {
		// 不用方法引用：mixin 会重命名方法，引用会失效；用 lambda 直接捕获 this
		IndirectDrawBridge.register((pass, commandOffset) ->
				this.acceleratedrendering$drawElementsIndirect(pass, commandOffset));
	}

	private void acceleratedrendering$drawElementsIndirect(GlRenderPass pass, long commandOffset) {
		if (pass == null || pass.isClosed()) {
			return;
		}

		if (!this.invokeTrySetup(pass, Collections.emptyList())) {
			return;
		}

		var pipeline		= pass.pipeline;
		var vertexBuffers	= pass.vertexBuffers;
		var index			= pass.indexBuffer;

		GpuBuffer vertex = vertexBuffers == null ? null : vertexBuffers[0];

		if (pipeline == null || vertex == null || index == null) {
			return;
		}

		// 与 GlCommandEncoder.drawFromBuffers 一致：绑定 VAO（顶点缓冲 + 顶点属性）
		this.device
				.vertexArrayCache()
				.bindVertexArray(pipeline.info().getVertexFormat(), (GlBuffer) vertex);

		GlStateManager._glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ((GlBuffer) index).handle);

		VertexFormat.Mode mode = pipeline.info().getVertexFormatMode();

		GL40.glDrawElementsIndirect(
				GlConst.toGl(mode),
				GL11.GL_UNSIGNED_INT,
				commandOffset
		);
	}
}
