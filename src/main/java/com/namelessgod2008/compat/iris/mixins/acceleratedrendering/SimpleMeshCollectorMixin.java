package com.namelessgod2008.compat.iris.mixins.acceleratedrendering;

import com.namelessgod2008.compat.iris.interfaces.IIrisMeshCollector;
import com.namelessgod2008.core.buffers.memory.IMemoryInterface;
import com.namelessgod2008.core.buffers.memory.VertexLayout;
import com.namelessgod2008.core.meshes.collectors.SimpleMeshCollector;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在网格收集阶段填充 Iris 的 {@code mc_midTexCoord} 扩展元素。
 *
 * <p><b>为什么是这里</b>：实体几何的构建**不经过 {@code AcceleratedBufferBuilder}**。
 * 实际链路是
 * <pre>
 * ModelPartMixin.render()
 *   → CulledMeshCollector（凑齐一个多边形、做面剔除）
 *     → SimpleMeshCollector.addVertex()   ← 写入自己的 ByteBufferBuilder + MeshData.Builder
 *       → ClientMesh.Builder.build()      ← 产出按 56 字节布局排好的顶点流
 *         → ClientMesh.write()
 *           → AcceleratedBufferBuilder.addClientMesh()  ← 只做一次 memCopy
 * </pre>
 * {@code addClientMesh} 是整块内存拷贝，**不会逐顶点补字段**，所以只在那时注入已经太晚；
 * 而 {@code AcceleratedBufferBuilder.addVertex/setUv} 在这条链上一次都不会被调用
 * （实测探针从未触发，构造器探针却正常打印）。
 *
 * <p><b>为什么必须填</b>（2026-09-16 定位的「开光影后不透明实体消失」根因）：
 * Iris 在 {@code MixinBufferBuilder.fillExtendedData(int)} 里为 vanilla 的 {@code BufferBuilder}
 * 填这个元素（取多边形顶点 UV 的算术平均），但上述加速链路完全不经过那个 mixin。
 * Photon 的 {@code gbuffer/solid.glsl}（{@code entity_cutout} 等不透明类型）与
 * {@code gbuffer/translucent.glsl} 都用了 {@code mc_midTexCoord}：
 * <pre>
 * solid.glsl:       124: bool is_top_vertex = uv.y &lt; mc_midTexCoord.y;   // 无条件使用
 *                   433: if (base_color.a &lt; 0.99) discard;                 // 严苛
 * translucent.glsl: 109: bool is_top_vertex = uv.y &lt; mc_midTexCoord.y;
 *                   477: if (base_color.a &lt; 0.1) discard;                  // 宽松
 * </pre>
 * 未初始化的值令 UV / 顶点动画错乱，不透明几何被大面积 {@code discard} ——
 * 表现为「羊/箱子/史莱姆眼睛消失，史莱姆身体（半透明）正常」。
 *
 * <p>元素偏移经 {@code layout.getElement(MID_TEXTURE_ELEMENT)} 动态取得，不硬编码
 * （Iris 1.11.4 已把 {@code ENTITY_ID_ELEMENT} 从 USHORT×3 改为 USHORT×4）。
 */
@Mixin(SimpleMeshCollector.class)
public class SimpleMeshCollectorMixin implements IIrisMeshCollector {

	@Shadow @Final private	VertexLayout		layout;
	@Shadow private			long				vertexAddress;

	@Unique private			IMemoryInterface	iris$midTexCoordOffset;
	@Unique private			boolean				iris$initialized;

	@Unique private final	long[]				iris$polygonVertices	= new long[4];
	@Unique private			int					iris$vertexInPolygon;

	/**
	 * 登记顶点地址，供 {@link #setIrisMidTexCoord} 回填。
	 *
	 * <p>只注入 11 参重载（返回 {@code void}）：它是 {@code CulledMeshCollector.flush()}
	 * 转发多边形顶点时实际调用的那个。3 参 {@code addVertex(FFF)} 在本链路上不被调用，
	 * 注入它不会有任何效果（曾因此让修复静默失效）。
	 */
	@Inject(method = "addVertex(FFFIFFIIFFF)V", at = @At("TAIL"))
	private void iris$trackVertex(CallbackInfo ci) {
		if (iris$vertexInPolygon < iris$polygonVertices.length) {
			iris$polygonVertices[iris$vertexInPolygon++] = vertexAddress;
		}
	}

	@Unique
	@Override
	public void setIrisMidTexCoord(float midU, float midV) {
		if (!iris$initialized) {
			iris$initialized			= true;
			iris$midTexCoordOffset		= layout.getElement(IrisVertexFormats.MID_TEXTURE_ELEMENT);
		}

		for (var address : iris$polygonVertices) {
			if (address != 0L) {
				iris$midTexCoordOffset.putFloat(address + 0L, midU);
				iris$midTexCoordOffset.putFloat(address + 4L, midV);
			}
		}

		iris$reset();
	}

	@Unique
	private void iris$reset() {
		java.util.Arrays.fill(iris$polygonVertices, 0L);
		iris$vertexInPolygon = 0;
	}
}
