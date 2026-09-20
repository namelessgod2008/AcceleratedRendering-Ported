package com.namelessgod2008.compat.iris;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.namelessgod2008.core.backends.GbufferBridge;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * 把加速绘制的输出重定向到 Iris 的 gbuffer。
 *
 * <p><b>为什么需要</b>（2026-09-20 RenderDoc 实证）：加速绘制走 26.1 的
 * {@code createRenderPass(color, depth)} 自建 pass。该 API 会用传入的纹理
 * （实测为 {@code MainTarget}，窗口尺寸）建并绑定 FBO，
 * **完全绕过 Iris 的 gbuffer 重定向**。
 *
 * <p>RenderDoc 抓帧对比（同一帧）：
 * <pre>
 * Iris 自己的绘制  : output = 2048x2048   R8G8B8A8    （gbuffer）
 * AR 的实体绘制    : output = 2560x1494   R16G16B16A16（主渲染目标＝窗口分辨率）
 * </pre>
 * Iris 的 composite 阶段采样 gbuffer 生成最终画面，故画在 gbuffer 之外的几何
 * **不会被合成** —— 表现为开光影后实体消失。
 *
 * <p><b>为什么在这里重定向能生效</b>：{@code GlRenderPass} 内部**不持有 FBO 字段**，
 * FBO 仅在 {@code GlCommandEncoder.createRenderPass} 里绑定一次，pass 存活期间
 * 不会再重绑。故在「pass 已打开、尚未绘制」的窗口期重新绑定 FBO 即可生效。
 *
 * <p><b>为什么还要重设视口</b>：{@code createRenderPass} 用**传入纹理的尺寸**设视口
 * （主 target = 窗口尺寸），而 gbuffer 尺寸不同（Iris 默认 2048）。
 * 不重设会导致画面只占一角或整体错位。
 *
 * <p>本类是 Iris 兼容代码，位于 {@code compat/iris}；由 {@code IrisCompatMixinPlugin}
 * 做存在性判定，未装 Iris 时不会被加载。
 */
public final class IrisGbufferBridge implements GbufferBridge.Redirector {

	public static final IrisGbufferBridge INSTANCE = new IrisGbufferBridge();

	private IrisGbufferBridge() {
	}

	@Override
	public boolean redirectToGbuffer() {
		// 阴影通道必须跳过：{@code IrisCompatBuffers.*_SHADOW.drawBuffers()} 与主通道
		// 共用本方法，而阴影渲染有自己的 FBO（{@code ShadowRenderTargets}）。
		// 阴影期切到 gbuffer 会让阴影几何写错目标 —— 实测表现为「阴影整个消失」。
		// 另外 Iris 在阴影期会把 {@code _glBindFramebuffer} 推迟（changeFramebuffer 的
		// ShadowRenderingState 分支），此时我们抢绑 FBO 与它冲突。
		if (net.irisshaders.iris.shadows.ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
			return false;
		}

		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

		if (!(pipeline instanceof IrisRenderingPipeline iris)) {
			return false;
		}

		// 用 bindDefault() 而非硬绑 defaultFB —— 它会按 isBeforeTranslucent 自动二选一，
		// 而这个选择恰好与调用时机一一对应（见 LevelRendererMixin 的拆分说明）：
		//
		//   OPAQUE 锚点（renderSolidFeatures 之后）  → isBeforeTranslucent=true  → defaultFB
		//   TRANSLUCENT 锚点（endOutlineBatch 之后） → isBeforeTranslucent=false → defaultFBAlt
		//
		// ⚠️ 不要"优化"成二选一里硬绑某一个：拆分成两个时机之后，两批几何本就该写进
		// 不同纹理。曾硬绑 defaultFB，导致半透明几何写错目标 —— 实测症状为
		// 「史莱姆外壳消失、内层小方块显示为不透明」。
		iris.bindDefault();

		// 视口用 gbuffer 尺寸重设（bindDefault 已把 FBO 切到 gbuffer）。
		// 尺寸直接从「当前绑定的 FBO 的 color attachment」查，不依赖 Iris 内部对象
		// （其 renderTargets 字段是 private 且无 getter）。
		int[] size = queryCurrentFboColorSize();

		if (size != null) {
			GlStateManager._viewport(0, 0, size[0], size[1]);
		}

		return true;
	}

	/**
	 * 查询当前绑定 FBO 的 color attachment 尺寸。
	 *
	 * <p>先取 attachment 的纹理名，再临时绑到 {@code GL_TEXTURE_2D} 查宽高，
	 * 最后恢复原绑定，避免污染调用方的纹理状态。
	 *
	 * @return {@code [width, height]}；查询失败时返回 null
	 */
	private static int[] queryCurrentFboColorSize() {
		if (GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING) == 0) {
			return null;
		}

		int texture = GL30.glGetFramebufferAttachmentParameteri(
				GL30.GL_FRAMEBUFFER,
				GL30.GL_COLOR_ATTACHMENT0,
				GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME
		);

		if (texture == 0) {
			return null;
		}

		int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);

		int width	= GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
		int height	= GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous);

		return (width > 0 && height > 0) ? new int[] { width, height } : null;
	}
}
