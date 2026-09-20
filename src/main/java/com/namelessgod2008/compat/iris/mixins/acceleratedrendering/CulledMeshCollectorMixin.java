package com.namelessgod2008.compat.iris.mixins.acceleratedrendering;

import com.namelessgod2008.compat.iris.interfaces.IIrisMeshCollector;
import com.namelessgod2008.core.meshes.collectors.CulledMeshCollector;
import com.namelessgod2008.core.meshes.collectors.SimpleMeshCollector;
import com.namelessgod2008.core.utils.Vertex;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在面剔除后、顶点转发给 {@code SimpleMeshCollector} 之前，
 * 算好该多边形的 {@code mc_midTexCoord} 平均值并交给它回填。
 *
 * <p>选 {@code flush()} 而非逐顶点注入，是因为只有这里同时具备两个条件：
 * <ol>
 *   <li>{@code polygon[]} 持有完整多边形的全部顶点（能算 UV 平均）；</li>
 *   <li>此时才决定该多边形**是否被剔除** —— 被剔除的多边形不会调
 *       {@code meshCollector.addVertex}，也就不该写 {@code mc_midTexCoord}。</li>
 * </ol>
 *
 * <p>注入点取方法 TAIL：此时转发循环已把本多边形各顶点的地址登记进
 * {@code SimpleMeshCollector}，正好可以回填。
 *
 * <p>算法与 Iris {@code MixinBufferBuilder.fillExtendedData} 一致：对多边形所有顶点的
 * UV 求算术平均。多边形被剔除时不调用（顶点未转发，回填会写错对象）。
 *
 * <p>注意：{@code flush()} 在 {@code vertexIndex < polygonSize - 1} 时直接返回，
 * 故 TAIL 处需自行判断本次是否真的转发了一个多边形 —— 用 {@code meshCollector}
 * 的顶点数是否增长来判断会引入额外读取，这里改为复算一遍剔除判据的等价条件：
 * 即 {@code vertexIndex} 是否已被重置为 -1（{@code flush} 的入口分支）。
 */
@Mixin(CulledMeshCollector.class)
public class CulledMeshCollectorMixin {

	@Shadow @Final private	SimpleMeshCollector	meshCollector;
	@Shadow @Final private	Vertex[]			polygon;
	@Shadow private			int					vertexIndex;

	/** 进入本次 flush 时的顶点数，用于判断本次是否真的转发了一个多边形 */
	@Unique private	long	iris$vertexCountBefore;

	@Inject(method = "flush", at = @At("HEAD"))
	private void iris$beforeFlush(CallbackInfo ci) {
		iris$vertexCountBefore = meshCollector.getVertexCount();
	}

	@Inject(method = "flush", at = @At("TAIL"))
	private void iris$fillMidTexCoord(CallbackInfo ci) {
		if (!(meshCollector instanceof IIrisMeshCollector irisCollector)) {
			return;
		}

		// 只有真正转发了顶点（未被剔除、且凑齐了一个多边形）才回填
		if (meshCollector.getVertexCount() == iris$vertexCountBefore) {
			return;
		}

		var midU = 0.0F;
		var midV = 0.0F;

		for (var vertex : polygon) {
			if (vertex == null) {
				return;
			}

			midU += vertex.getUv().x;
			midV += vertex.getUv().y;
		}

		var size = polygon.length;

		irisCollector.setIrisMidTexCoord(midU / size, midV / size);
	}
}
