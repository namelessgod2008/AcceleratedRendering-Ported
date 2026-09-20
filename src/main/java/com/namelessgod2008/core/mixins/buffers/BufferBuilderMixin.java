package com.namelessgod2008.core.mixins.buffers;

import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.core.buffers.EmptyAcceleratedBufferSources;
import com.namelessgod2008.core.buffers.accelerated.IAcceleratedBufferSource;
import com.namelessgod2008.core.buffers.accelerated.IAccelerationHolder;
import com.namelessgod2008.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import com.namelessgod2008.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.namelessgod2008.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.Supplier;

@Mixin(BufferBuilder.class)
public class BufferBuilderMixin implements IAccelerationHolder, IAcceleratedVertexConsumer {

	@Unique private IAcceleratedBufferSource	bufferSources = EmptyAcceleratedBufferSources.INSTANCE;
	@Unique private RenderType					renderType;
	@Unique private AcceleratedBufferBuilder	acceleration;
	@Unique private boolean						init	= false;
	@Unique private int							layer	= Integer.MIN_VALUE;

	@Unique
	@Override
	public VertexConsumer initAcceleration(RenderType renderType, Supplier<IAcceleratedBufferSource> bufferSource) {
		if (CoreFeature.isLoaded() && !init) {
			this.bufferSources	= bufferSource.get();
			this.renderType		= renderType;
			this.acceleration	= null;
			this.init			= true;
		}

		return (VertexConsumer) this;
	}

	@Unique
	@Override
	public boolean isAccelerated() {
		return bufferSources != EmptyAcceleratedBufferSources.INSTANCE && getAccelerated() != null;
	}

	/**
	 * 包装器（{@code SpriteCoordinateExpander} 等）经
	 * {@link com.namelessgod2008.core.buffers.accelerated.builders.IAcceleratedWrapperDelegation}
	 * 把元信息查询委托到这里 —— 这是唯一持有 {@code renderType} 的地方。
	 *
	 * <p><b>缺失它会实打实地中断渲染</b>：{@code IAcceleratedVertexConsumer} 的这些方法是
	 * 「默认抛 UnsupportedOperationException」，而 {@code CulledMeshCollector} 构造器会调用
	 * {@code downloadTexture() → getRenderType()}。方块实体（箱子/末影箱）走 sprite 包装路径，
	 * 是首个撞上此缺陷的场景 —— 表现为模型完全消失（碰撞箱仍在）。
	 */
	@Unique
	@Override
	public RenderType getRenderType() {
		return renderType;
	}

	@Unique
	@Override
	public com.namelessgod2008.core.buffers.memory.VertexLayout getLayout() {
		var accelerated = getAccelerated();

		return accelerated == null ? null : accelerated.getLayout();
	}

	@Unique
	@Override
	public int getPolygonSize() {
		var accelerated = getAccelerated();

		return accelerated == null ? 0 : accelerated.getPolygonSize();
	}

	@Unique
	@Override
	public <T> void doRender(
			IAcceleratedRenderer<T>	renderer,
			T						context,
			Matrix4f				transform,
			Matrix3f				normal,
			int						light,
			int						overlay,
			int						color
	) {
		getAccelerated().doRender(
				renderer,
				context,
				transform,
				normal,
				light,
				overlay,
				color
		);
	}

	@Unique
	@Override
	public AcceleratedBufferBuilder getAccelerated() {
		var layer = CoreFeature.getDefaultLayer();

		if (		this.layer			!= layer
				||	this.acceleration	== null
				||	this.acceleration.isOutdated()
		) {
			this.layer			= layer;
			this.acceleration	= bufferSources.getBuffer(
					renderType,
					CoreFeature.getDefaultLayerBeforeFunction	(),
					CoreFeature.getDefaultLayerAfterFunction	(),
					layer
			);
		}

		return this.acceleration;
	}

}
