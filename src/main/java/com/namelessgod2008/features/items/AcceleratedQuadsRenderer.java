package com.namelessgod2008.features.items;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.namelessgod2008.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.namelessgod2008.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import com.namelessgod2008.core.meshes.IMesh;
import com.namelessgod2008.core.meshes.data.MeshData;
import com.namelessgod2008.features.entities.AcceleratedEntityRenderingFeature;
import com.namelessgod2008.features.items.colors.ILayerColors;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.experimental.ExtensionMethod;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Map;

/**
 * 26.1 物品（BakedQuad）加速渲染器。
 *
 * <p>与 1.21.4 的差别：26.1 的 {@link BakedQuad} 是**自包含 record**，暴露
 * {@code position(vertex)} / {@code packedUV(vertex)} / {@code materialInfo()} 等公开访问器。
 * 1.21.4 那套「{@code int[] vertices} + access widener + {@code IQuadTransformer} 常量 + 逐字节
 * 解析法线」的读取方式在 26.1 完全不需要，故 1.21.4 的 {@code IAcceleratedBakedQuad} 接口与
 * {@code BakedQuadMixin} 在本版本**不需要移植**。
 *
 * <p>顶点数据与 26.1 {@code VertexConsumer.putBakedQuad} 逐项对齐：位置用模型空间坐标
 * （变换交由 GPU compute 完成，这正是加速的意义），光照用
 * {@code LightCoordsUtil.lightCoordsWithEmission(light, lightEmission)}，颜色用该 quad 的
 * tint 色（{@code tintIndex} 越界时为 -1，表示不着色）。
 *
 * <p>网格缓存键用 {@code BakedQuad} 实例本身（record 的 equals 按字段值比较，同一模型烘焙出的
 * quad 为同一实例），二级缓存用 {@link MeshData} 做跨 quad 合并。
 */
@ExtensionMethod(VertexConsumerExtension.class)
public class AcceleratedQuadsRenderer implements IAcceleratedRenderer<AcceleratedQuadsRenderer.Context> {

	public static final AcceleratedQuadsRenderer INSTANCE = new AcceleratedQuadsRenderer();

	/** 一级缓存：quad → （加速 builder → 网格）。 */
	private final Map<BakedQuad, Map<Object, IMesh>> meshes = new Reference2ObjectOpenHashMap<>();

	/** 二级缓存：网格数据 → 网格（几何相同的 quad 合并复用）。 */
	private final Map<MeshData, IMesh> merges = new Object2ObjectOpenHashMap<>();

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
		var extension = vertexConsumer.getAccelerated();

		extension.beginTransform(transform, normal);

		var quads	= context.quads	();
		var colors	= context.colors();

		for (var quad : quads) {
			renderQuad(extension, quad, colors, light, overlay);
		}

		extension.endTransform();
	}

	/** 单个 quad：命中缓存则直接写入，否则构建网格后缓存。 */
	private void renderQuad(
			IAcceleratedVertexConsumer	extension,
			BakedQuad					quad,
			ILayerColors				colors,
			int							light,
			int							overlay
	) {
		var byBuilder	= meshes.computeIfAbsent(quad, ignored -> new Reference2ObjectOpenHashMap<>());
		var mesh		= byBuilder.get(extension);
		var color		= colors.getColor(quad.materialInfo().tintIndex());

		if (mesh != null) {
			mesh.write(extension, color, light, overlay);
			return;
		}

		var meshCollector	= CoreFeature.createMeshCollector(extension);
		var meshBuilder		= extension.decorate(meshCollector);
		var lightEmission	= quad.materialInfo().lightEmission();

		for (var vertex = 0; vertex < BakedQuad.VERTEX_COUNT; vertex++) {
			var position = quad.position(vertex);
			var packedUv = quad.packedUV(vertex);

			meshBuilder.addVertex(
					position.x(),
					position.y(),
					position.z(),
					-1,
					UVPair.unpackU(packedUv),
					UVPair.unpackV(packedUv),
					overlay,
					LightCoordsUtil.lightCoordsWithEmission(light, lightEmission),
					0.0F,
					0.0F,
					0.0F
			);
		}

		meshCollector.flush();

		var data	= meshCollector.getData();
		var buffer	= meshCollector.getBuffer();
		mesh		= merges.get(data);

		if (mesh != null) {
			buffer.discard();
			buffer.close();
		} else {
			mesh = AcceleratedEntityRenderingFeature
					.getMeshType()
					.getBuilder()
					.build(meshCollector);
		}

		byBuilder.put(extension, mesh);
		merges.put(data, mesh);

		mesh.write(extension, color, light, overlay);
	}

	public static Context context(List<BakedQuad> quads, ILayerColors colors) {
		return new Context(quads, colors);
	}

	public record Context(List<BakedQuad> quads, ILayerColors colors) {

	}
}
