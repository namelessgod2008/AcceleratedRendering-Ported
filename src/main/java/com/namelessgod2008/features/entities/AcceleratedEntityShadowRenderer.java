package com.namelessgod2008.features.entities;

import com.namelessgod2008.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.namelessgod2008.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.experimental.ExtensionMethod;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 26.1 阴影加速渲染器。
 *
 * <p>与 1.21.4 的差别：1.21.4 由 {@code EntityRenderDispatcher.renderBlockShadow} 在绘制时
 * 现算「方块可见性、亮度、碰撞形状、alpha」，因此 1.21.4 的 Context 需要
 * {@code levelReader}/{@code chunkAccess}/{@code blockPos} 才能做这些判断。
 * 26.1 已把判断全部前移到 {@code EntityRenderer.extractShadowPiece}（提交段），
 * 产出 {@code EntityRenderState.ShadowPiece(relativeX, relativeY, relativeZ, shapeBelow, alpha)}，
 * 顶点写入阶段拿不到世界/区块，也不需要——数据已是判定后的结果。
 *
 * <p>故此处 Context 直接承载 ShadowPiece 的字段。逐顶点几何与 26.1
 * {@code ShadowFeatureRenderer.renderTranslucent} 逐行等价（形状包围盒 + 相对偏移 + 以半径
 * 归一化的 uv），仅换成走加速管线写入。
 */
@ExtensionMethod(VertexConsumerExtension.class)
public class AcceleratedEntityShadowRenderer implements IAcceleratedRenderer<AcceleratedEntityShadowRenderer.Context> {

	public static final AcceleratedEntityShadowRenderer INSTANCE = new AcceleratedEntityShadowRenderer();

	@Override
	public void render(
			VertexConsumer	vertexConsumer,
			Context			context,
			Matrix4f		transform,
			Matrix3f		normal,
			int				light,
			int				overlay,
			int				color
	) {
		var extension	= vertexConsumer	.getAccelerated	();
		var radius		= context			.radius			();
		var bounds		= context			.shapeBelow		().bounds		();
		var relativeX	= context			.relativeX		();
		var relativeY	= context			.relativeY		();
		var relativeZ	= context			.relativeZ		();

		var minX = relativeX + (float) bounds.minX;
		var maxX = relativeX + (float) bounds.maxX;
		var minY = relativeY + (float) bounds.minY;
		var minZ = relativeZ + (float) bounds.minZ;
		var maxZ = relativeZ + (float) bounds.maxZ;

		var u0 = -minX / 2.0f / radius + 0.5f;
		var u1 = -maxX / 2.0f / radius + 0.5f;
		var v0 = -minZ / 2.0f / radius + 0.5f;
		var v1 = -maxZ / 2.0f / radius + 0.5f;

		// 26.1 原版为 ARGB.white(piece.alpha())；addVertex 内部会转成 GPU 期望的 ABGR
		var shadowColor = ARGB.white(context.alpha());

		extension.beginTransform(transform, normal);

		vertexConsumer.addVertex(minX, minY, minZ, shadowColor, u0, v0, overlay, light, 0.0f, 1.0f, 0.0f);
		vertexConsumer.addVertex(minX, minY, maxZ, shadowColor, u0, v1, overlay, light, 0.0f, 1.0f, 0.0f);
		vertexConsumer.addVertex(maxX, minY, maxZ, shadowColor, u1, v1, overlay, light, 0.0f, 1.0f, 0.0f);
		vertexConsumer.addVertex(maxX, minY, minZ, shadowColor, u1, v0, overlay, light, 0.0f, 1.0f, 0.0f);

		extension.endTransform();
	}

	public static Context context(
			float		relativeX,
			float		relativeY,
			float		relativeZ,
			VoxelShape	shapeBelow,
			float		radius,
			float		alpha
	) {
		return new Context(
				relativeX,
				relativeY,
				relativeZ,
				shapeBelow,
				radius,
				alpha
		);
	}

	public record Context(
			float		relativeX,
			float		relativeY,
			float		relativeZ,
			VoxelShape	shapeBelow,
			float		radius,
			float		alpha
	) {

	}
}
