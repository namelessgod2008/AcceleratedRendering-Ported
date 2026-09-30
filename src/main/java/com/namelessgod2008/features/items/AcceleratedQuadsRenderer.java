package com.namelessgod2008.features.items;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.namelessgod2008.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.namelessgod2008.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import com.namelessgod2008.core.buffers.memory.VertexLayout;
import com.namelessgod2008.core.meshes.IMesh;
import com.namelessgod2008.core.meshes.data.MeshData;
import com.namelessgod2008.features.entities.AcceleratedEntityRenderingFeature;
import com.namelessgod2008.features.items.colors.ILayerColors;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.experimental.ExtensionMethod;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
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
 * （变换交由 GPU compute 完成，这正是加速的意义）。
 */
@ExtensionMethod(VertexConsumerExtension.class)
public class AcceleratedQuadsRenderer implements IAcceleratedRenderer<AcceleratedQuadsRenderer.Context> {

	public static final AcceleratedQuadsRenderer INSTANCE = new AcceleratedQuadsRenderer();

	/**
	 * 一级缓存：quad 实例序列 → 合并网格。
	 *
	 * <p>键按 quad **实例的身份序列**而非持有它的 builder 身份，因此可跨帧命中 ——
	 * 同一物品模型的 quad 在资源重载期烘焙一次，实例跨帧稳定。
	 * （对照实体侧 {@code ModelPartMixin}：其键 {@code IBufferGraph} 的 equals 是
	 * {@code (renderType, layout)}，天然跨帧命中。物品侧原先用
	 * {@code Reference2ObjectOpenHashMap} 按身份比较，而 builder 每帧重建
	 * （{@code AcceleratedBufferSource.clearBuffers}），故跨帧 100% miss，
	 * 且旧条目永不清理 —— 2026-09-30 修复。）
	 */
	private final Map<MergeKey, IMesh> merged = new Object2ObjectOpenHashMap<>();

	/** 二级缓存：网格数据 → 网格（几何相同的合并结果复用）。 */
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
		// [AR-ITEMAUDIT]
		final long tRender = System.nanoTime();

		var extension = vertexConsumer.getAccelerated();

		extension.beginTransform(transform, normal);

		renderQuads(extension, context.quads(), context.colors(), light, overlay);

		extension.endTransform();

		com.namelessgod2008.core.AccelStats.ITEM_RENDER_NANOS += System.nanoTime() - tRender;

		// [AR-ITEMAUDIT] 缓存结构快照
		com.namelessgod2008.core.AccelStats.ITEM_MESHES_SIZE = merged.size();
		com.namelessgod2008.core.AccelStats.ITEM_MERGES_SIZE = merges.size();
	}

	/**
	 * 把一批 quad 尽可能合并成少数网格。
	 *
	 * <p><b>为什么能合并</b>：{@code mesh.write(color, light, overlay)} 只为整个网格提供
	 * **一份**逐实例数据，故同一网格内的 quad 必须共享这三者。{@code light}/{@code overlay}
	 * 在一次 {@code render} 调用内恒定；{@code color} 由 {@code tintIndex} 决定。
	 * 唯一的其它约束是 {@code lightEmission} —— 它被烘焙进网格顶点的 uv2。
	 * 故分组键是 {@code (tintIndex, lightEmission)}。
	 *
	 * <p><b>为什么 light/overlay 不必进键</b>（见 {@code entity_vertex_transform_shader.compute}）：
	 * <pre>
	 *   verticesOut.uv2 = max(uv2In, uv2Mesh);          // 网格烘焙值只是「亮度下限」
	 *   verticesOut.uv1 = verticesIn[reference].uv1;    // overlay 取逐实例值，烘焙值未被读取
	 * </pre>
	 * 因此把网格的 uv2 烘焙成**仅含发光**（{@code light = 0}），真实光照由逐实例值提供。
	 * 这比合并前更正确：合并前同一网格被所有物品共享，烘焙的是「本帧第一个物品」的光照，
	 * 会让阴影中的物品被提亮。
	 *
	 * <p><b>收益</b>：把「每 quad 一次」的簿记（两次 map 查找 + {@code addServerMesh}
	 * + 7 个 meshInfo int 写）降到「每提交一次」。1400 重锤实测：96,710 次/帧 → 1,458 次/帧。
	 */
	private void renderQuads(
			IAcceleratedVertexConsumer	extension,
			List<BakedQuad>				quads,
			ILayerColors				colors,
			int							light,
			int							overlay
	) {
		int size = quads.size();

		if (size == 0) {
			return;
		}

		var first		= quads.get(0).materialInfo();
		int tint		= first.tintIndex		();
		int emission	= first.lightEmission	();

		// 常见情形：整个列表同 tintIndex + lightEmission（普通物品模型），免去分组
		boolean uniform = true;

		for (int i = 1; i < size; i ++) {
			var info = quads.get(i).materialInfo();

			if (info.tintIndex() != tint || info.lightEmission() != emission) {
				uniform = false;
				break;
			}
		}

		if (uniform) {
			writeMerged(extension, quads, tint, emission, colors, light, overlay);
			return;
		}

		// 慢路径：分组数通常 ≤3（带染色叠加层的模型），线性扫描比建 map 快
		var groups = new ArrayList<TintGroup>(4);

		for (var quad : quads) {
			var info	= quad.materialInfo();
			TintGroup group = null;

			for (var candidate : groups) {
				if (candidate.tint == info.tintIndex() && candidate.emission == info.lightEmission()) {
					group = candidate;
					break;
				}
			}

			if (group == null) {
				group = new TintGroup(info.tintIndex(), info.lightEmission());
				groups.add(group);
			}

			group.quads.add(quad);
		}

		for (var group : groups) {
			writeMerged(extension, group.quads, group.tint, group.emission, colors, light, overlay);
		}
	}

	/** 取（或构建）合并网格，然后写入一次逐实例数据。 */
	private void writeMerged(
			IAcceleratedVertexConsumer	extension,
			List<BakedQuad>				quads,
			int							tint,
			int							emission,
			ILayerColors				colors,
			int							light,
			int							overlay
	) {
		var key		= new MergeKey(quads, tint, emission, extension.getLayout());
		var mesh	= merged.get(key);

		// [AR-PROBE-ITEM2] 记录合并网格的命中情况
		com.namelessgod2008.core.AccelStats.itemProbe2(mesh != null);

		if (mesh == null) {
			mesh = buildMesh(extension, quads, emission, overlay);

			merged.put(key, mesh);
		}

		mesh.write(extension, colors.getColor(tint), light, overlay);
	}

	/** 把一个 quad 序列烘焙成一个网格。 */
	private IMesh buildMesh(
			IAcceleratedVertexConsumer	extension,
			List<BakedQuad>				quads,
			int							emission,
			int							overlay
	) {
		// [AR-ITEMAUDIT]
		final long tMissTot = System.nanoTime	();
		final long tGather	= tMissTot;

		var meshCollector	= CoreFeature.createMeshCollector(extension);
		var meshBuilder		= extension.decorate(meshCollector);

		// 只烘焙发光；真实光照由 write() 的逐实例值提供（见 renderQuads 的说明）
		var bakedLight		= LightCoordsUtil.lightCoordsWithEmission(0, emission);

		for (var quad : quads) {
			// 法线必须烘焙「模型空间单位法线」：GPU 的变换着色器会用 sharingData.normal
			// （即 pose 的法线矩阵）再变换一次，这与 vanilla 的
			// VertexConsumer.putBakedQuad:83 `pose.transformNormal(quad.direction().getUnitVec3f())`
			// 等价 —— 只是把那次变换从 CPU 挪到了 GPU。
			//
			// ⚠️ 传 (0,0,0) 会让 minecraft_mix_light 的分量
			// `dot(Light0_Direction, Normal)` / `dot(Light1_Direction, Normal)` 恒为 0，
			// 方向光照退化为常量 0.4（见 light.glsl 的 MINECRAFT_AMBIENT_LIGHT），
			// 表现为「物品旋转时明暗不再变化」。2026-09-30 修复。
			var normal = quad.direction().getUnitVec3f();

			for (var vertex = 0; vertex < BakedQuad.VERTEX_COUNT; vertex ++) {
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
						bakedLight,
						normal.x(),
						normal.y(),
						normal.z()
				);
			}
		}

		meshCollector.flush();

		var data	= meshCollector.getData();
		var buffer	= meshCollector.getBuffer();

		com.namelessgod2008.core.AccelStats.ITEM_MISS_GATHER_NANOS += System.nanoTime() - tGather;

		final long tGet = System.nanoTime();

		var mesh = merges.get(data);

		com.namelessgod2008.core.AccelStats.ITEM_MISS_GET_NANOS += System.nanoTime() - tGet;

		if (mesh != null) {
			buffer.discard();
			buffer.close();
		} else {
			final long tBld = System.nanoTime();

			mesh = AcceleratedEntityRenderingFeature
					.getMeshType()
					.getBuilder()
					.build(meshCollector);

			com.namelessgod2008.core.AccelStats.ITEM_MISS_BLD_NANOS += System.nanoTime() - tBld;
			com.namelessgod2008.core.AccelStats.ITEM_MISS_BUILDS ++;

			merges.put(data, mesh);
		}

		com.namelessgod2008.core.AccelStats.ITEM_MISS_TOT_NANOS += System.nanoTime() - tMissTot;

		return mesh;
	}

	public static Context context(List<BakedQuad> quads, ILayerColors colors) {
		return new Context(quads, colors);
	}

	public record Context(List<BakedQuad> quads, ILayerColors colors) {

	}

	/** 同一 {@code (tintIndex, lightEmission)} 的一组 quad。 */
	private static final class TintGroup {

		final int				tint;
		final int				emission;
		final List<BakedQuad>	quads = new ArrayList<>();

		TintGroup(int tint, int emission) {
			this.tint		= tint;
			this.emission	= emission;
		}
	}

	/**
	 * 合并网格的缓存键：quad 实例序列 + 分组属性 + **顶点布局**。
	 *
	 * <p>用**身份**比较而非 {@link BakedQuad#equals}：BakedQuad 是 record，其 equals/hashCode
	 * 会逐字段计算 4 个 {@link org.joml.Vector3fc} 与 {@code MaterialInfo}，代价远高于身份比较；
	 * 而同一模型烘焙出的 quad 实例跨帧稳定，身份比较已足够。
	 *
	 * <p>{@code quads} 由调用方在构造后不再修改（{@code ItemFeatureRendererMixin} 每提交新建的
	 * 分组列表，或 {@code context.quads()}），故可直接持有引用。
	 *
	 * <p><b>layout 为什么必须在键里</b>（2026-09-30 定位的回归）：
	 * 网格顶点是按某个 {@link VertexLayout} 的 stride/偏移**打包**的，换布局后同一份字节会被
	 * 按错误的 stride 解读 → 几何变成乱码（实测：黑色三角形碎片）。
	 * Iris 用的顶点格式与原版不同（{@code IrisBufferEnvironment} 构造自己的
	 * {@code new VertexLayout(irisVertexFormat)}），因此**切换光影包时布局会变**。
	 * 键里若不含 layout，切到原版后会命中「用 Iris 布局建的网格」→ 乱码；
	 * 切回光影又因布局重新匹配而恢复 —— 精确对应实测的「不切换正常、一切换就坏、切回又恢复」。
	 *
	 * <p>实体侧的 {@code AcceleratedBufferBuilder} 从一开始就把 {@code layout} 放进
	 * {@code @EqualsAndHashCode.Include}，且 {@code MeshData} 的 equals 也含 layout —— 二者都
	 * 印证了「layout 是网格身份的一维」。{@link VertexLayout} 无 equals/hashCode，故这里是**身份**比较。
	 */
	private static final class MergeKey {

		private final List<BakedQuad>	quads;
		private final int				tint;
		private final int				emission;
		private final VertexLayout		layout;
		private final int				hash;

		MergeKey(List<BakedQuad> quads, int tint, int emission, VertexLayout layout) {
			this.quads		= quads;
			this.tint		= tint;
			this.emission	= emission;
			this.layout		= layout;

			int h	= tint;
			int n	= quads.size();

			h = h * 31 + emission;
			h = h * 31 + n;
			h = h * 31 + System.identityHashCode(layout);

			for (int i = 0; i < n; i ++) {
				h = h * 31 + System.identityHashCode(quads.get(i));
			}

			this.hash = h;
		}

		@Override
		public int hashCode() {
			return hash;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}

			if (!(o instanceof MergeKey other)) {
				return false;
			}

			if (	hash		!= other.hash
				||	tint		!= other.tint
				||	emission	!= other.emission
				||	layout		!= other.layout
			) {
				return false;
			}

			int n = quads.size();

			if (n != other.quads.size()) {
				return false;
			}

			for (int i = 0; i < n; i ++) {
				if (quads.get(i) != other.quads.get(i)) {
					return false;
				}
			}

			return true;
		}
	}
}
