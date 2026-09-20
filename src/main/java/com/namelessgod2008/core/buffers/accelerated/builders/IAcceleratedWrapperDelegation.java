package com.namelessgod2008.core.buffers.accelerated.builders;

import com.namelessgod2008.core.buffers.memory.VertexLayout;
import com.namelessgod2008.core.meshes.ServerMesh;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.nio.ByteBuffer;

/**
 * 给「包装其它 VertexConsumer 的 mixin」提供缺失方法的一站式委托实现。
 *
 * <p><b>为什么需要它</b>：{@link IAcceleratedVertexConsumer} 的多数方法是
 * 「默认抛 {@code UnsupportedOperationException}」，实现类必须逐个覆盖。
 * 各处包装器 mixin（{@code SpriteCoordinateExpanderMixin} /
 * {@code EntityOutlineGeneratorMixin} / …）只覆盖了实际用到的几个，
 * 其余保持默认抛异常状态 —— 平时不出问题，一旦被调用就直接中断渲染。
 *
 * <p><b>真实故障（2026-09-14）</b>：方块实体（箱子/末影箱）模型完全消失，碰撞箱仍在。
 * 根因链：
 * <pre>
 * 箱子走 SpriteId 路径 → buffer 被 TextureAtlasSprite.wrap() 包成 SpriteCoordinateExpander
 *   → CulledMeshCollector 构造器调用 vertexConsumer.downloadTexture()
 *   → IAcceleratedVertexConsumer.downloadTexture() 默认实现调用 getRenderType()
 *   → SpriteCoordinateExpanderMixin 未实现 getRenderType() → 抛 UnsupportedOperationException
 *   → 渲染中断 → 模型消失
 * </pre>
 * 羊等普通实体走 {@link AcceleratedBufferBuilder}（实现完整），故不受影响 ——
 * 这解释了「只有部分实体消失」的现象。
 *
 * <p>各 wrapper mixin 只需 {@code implements IAcceleratedWrapperDelegation} 并实现
 * {@link #acceleratedDelegate()}（返回被包装的消费者的加速视图的源头）即可。
 */
public interface IAcceleratedWrapperDelegation extends IAcceleratedVertexConsumer {

	/**
	 * 返回真正持有加速缓冲的 {@code VertexConsumer}（包装链的下层）。
	 * 多路包装（如 {@code VertexDoubleConsumer}）返回任意一路即可 ——
	 * 它们共享同一个加速缓冲，元信息一致。
	 */
	VertexConsumer acceleratedDelegate();

	@Override
	default RenderType getRenderType() {
		return VertexConsumerExtension.getAccelerated(acceleratedDelegate()).getRenderType();
	}

	@Override
	default VertexLayout getLayout() {
		return VertexConsumerExtension.getAccelerated(acceleratedDelegate()).getLayout();
	}

	@Override
	default int getPolygonSize() {
		return VertexConsumerExtension.getAccelerated(acceleratedDelegate()).getPolygonSize();
	}

	@Override
	default void addClientMesh(
			ByteBuffer	meshBuffer,
			int			size,
			int			color,
			int			light,
			int			overlay
	) {
		VertexConsumerExtension
				.getAccelerated	(acceleratedDelegate())
				.addClientMesh	(meshBuffer, size, color, light, overlay);
	}

	@Override
	default void addServerMesh(
			ServerMesh	serverMesh,
			int			color,
			int			light,
			int			overlay
	) {
		VertexConsumerExtension
				.getAccelerated	(acceleratedDelegate())
				.addServerMesh	(serverMesh, color, light, overlay);
	}
}
