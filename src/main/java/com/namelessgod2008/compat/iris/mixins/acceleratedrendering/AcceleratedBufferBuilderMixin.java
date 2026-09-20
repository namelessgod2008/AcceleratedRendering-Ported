package com.namelessgod2008.compat.iris.mixins.acceleratedrendering;

import com.namelessgod2008.compat.iris.interfaces.IIrisAcceleratedBufferBuilder;
import com.namelessgod2008.core.buffers.accelerated.AcceleratedRingBuffers;
import com.namelessgod2008.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import com.namelessgod2008.core.buffers.accelerated.draw.pools.IElementPool;
import com.namelessgod2008.core.buffers.accelerated.layers.LayerKey;
import com.namelessgod2008.core.buffers.accelerated.layers.functions.ILayerFunction;
import com.namelessgod2008.core.buffers.accelerated.pools.StagingBufferPool;
import com.namelessgod2008.core.buffers.memory.IMemoryInterface;
import com.namelessgod2008.core.buffers.memory.VertexLayout;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 填充 Iris 的实体标识扩展元素 {@code iris_Entity} 与 {@code mc_Entity}。
 *
 * <p>「每条实体几何写入路径都要补一份」是本兼容层的既定模式 —— 见
 * {@code SimpleMeshCollectorMixin}（{@code mc_midTexCoord}）与
 * {@code MeshUploadingProgramDispatcherMixin}（缓存网格上传）。三处各自覆盖一条路径，
 * 缺任何一处都会让对应路径的几何在光影下出错。
 *
 * <p>本类覆盖：
 * <ul>
 *   <li>11 参 {@code addVertex(FFFIFFIIFFF)} —— 直写顶点，{@code ModelPart} 走这条；</li>
 *   <li>{@code addVertex(FFF)} —— 链式写法的起点；</li>
 *   <li>{@code addServerMesh} / {@code addClientMesh} —— 缓存网格的整块拷贝路径。</li>
 * </ul>
 *
 * <p>元素偏移经 {@code layout.getElement(...)} 动态取得，不硬编码 ——
 * Iris 1.11.4 已把 {@code ENTITY_ID_ELEMENT} 从 USHORT×3 改为 USHORT×4。
 * 无 Iris 时 {@code layout} 查不到这些元素，返回 {@code NullMemoryInterface}（全 no-op）。
 */
@Mixin(AcceleratedBufferBuilder.class)
public class AcceleratedBufferBuilderMixin implements IIrisAcceleratedBufferBuilder {

	@Shadow @Final private	VertexLayout		layout;
	@Shadow private			long				vertexAddress;

	@Unique private			IMemoryInterface	entityIdOffset;
	@Unique private			IMemoryInterface	entityOffset;

	@Inject(
			method	= "<init>",
			at		= @At("TAIL")
	)
	public void constructor(
			StagingBufferPool			.StagingBuffer		vertexBuffer,
			StagingBufferPool			.StagingBuffer		varyingBuffer,
			IElementPool				.IElementSegment	elementSegment,
			AcceleratedRingBuffers		.Buffers			buffer,
			ILayerFunction									layerFunction,
			LayerKey										layerKey,
			CallbackInfo									ci
	) {
		entityIdOffset	= layout.getElement(IrisVertexFormats.ENTITY_ID_ELEMENT);
		entityOffset	= layout.getElement(IrisVertexFormats.ENTITY_ELEMENT);
	}

	@Inject(
			method	= "addVertex(FFFIFFIIFFF)V",
			at		= @At("TAIL")
	)
	public void addIrisVertex(
			float								pX,
			float								pY,
			float								pZ,
			int									pColor,
			float								pU,
			float								pV,
			int									pPackedOverlay,
			int									pPackedLight,
			float								pNormalX,
			float								pNormalY,
			float								pNormalZ,
			CallbackInfo						ci,
			@Local(name = "vertexAddress") long	vertexAddress
	) {
		addIrisData(vertexAddress);
	}

	@Inject(
			method	= "addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
			at		= @At("TAIL")
	)
	public void addIrisVertex(
			float									pX,
			float									pY,
			float 									pZ,
			CallbackInfoReturnable<VertexConsumer>	cir
	) {
		addIrisData(vertexAddress);
	}

	@Inject(
			method	= {
					"addServerMesh",
					"addClientMesh"
			},
			at		= @At(
					value	= "INVOKE",
					target	= "Lcom/namelessgod2008/core/buffers/memory/IMemoryInterface;putInt(JI)V",
					ordinal	= 2,
					shift	= At.Shift.AFTER
			),
			remap 	= false
	)
	public void addIrisMesh(CallbackInfo ci, @Local(name = "vertexAddress") long vertexAddress) {
		addIrisData(vertexAddress);
	}

	@Unique
	private void addIrisData(long vertexAddress) {
		entityOffset	.putShort(vertexAddress + 0L, (short) -1);
		entityOffset	.putShort(vertexAddress + 2L, (short) -1);
		entityIdOffset	.putShort(vertexAddress + 0L, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedEntity		());
		entityIdOffset	.putShort(vertexAddress + 2L, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity	());
		entityIdOffset	.putShort(vertexAddress + 4L, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedItem		());
	}

	@Unique
	@Override
	public IMemoryInterface getEntityIdOffset() {
		return entityIdOffset;
	}

	@Unique
	@Override
	public IMemoryInterface getEntityOffset() {
		return entityOffset;
	}
}
