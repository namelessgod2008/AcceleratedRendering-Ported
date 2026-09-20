package com.namelessgod2008.core.buffers.accelerated;

import com.namelessgod2008.core.AccelStats;
import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import com.namelessgod2008.core.buffers.accelerated.layers.LayerDrawType;
import com.namelessgod2008.core.buffers.accelerated.layers.LayerKey;
import com.namelessgod2008.core.buffers.accelerated.layers.functions.CustomLayerFunction;
import com.namelessgod2008.core.buffers.accelerated.layers.functions.EmptyLayerFunction;
import com.namelessgod2008.core.buffers.accelerated.layers.storage.empty.EmptyLayerStorage;
import com.namelessgod2008.core.buffers.environments.IBufferEnvironment;
import com.namelessgod2008.core.programs.dispatchers.meshes.MeshUploadingProgramDispatcher;
import com.namelessgod2008.core.utils.RenderTypeUtils;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import lombok.Getter;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.Map;
import java.util.Set;

import static org.lwjgl.opengl.GL46.*;

public class AcceleratedBufferSource implements IAcceleratedBufferSource {

	@Getter private	final	IBufferEnvironment						environment;
	private			final	AcceleratedRingBuffers					ringBuffers;
	private			final	Set<AcceleratedRingBuffers.Buffers>		buffers;
	private			final	Map<LayerKey, AcceleratedBufferBuilder> activeBuilders;
	private			final	IntSet									activeLayers;

	private					AcceleratedRingBuffers.Buffers			currentBuffer;
	private 				boolean									used;
	/** prepareBuffers 的幂等守卫（光影下绘制分两个时机，会调用两次） */
	private 				boolean									prepared;
	private					int										barriers;

	public AcceleratedBufferSource(IBufferEnvironment bufferEnvironment) {
		this.environment	= bufferEnvironment;
		this.ringBuffers	= new AcceleratedRingBuffers		(this.environment);
		this.buffers		= new ObjectLinkedOpenHashSet	<>	();
		this.activeBuilders	= new Object2ObjectOpenHashMap	<>	();
		this.activeLayers	= new IntAVLTreeSet					();

		this.currentBuffer	= this.ringBuffers.get(false);
		this.used			= false;
		this.barriers		= GL_SHADER_STORAGE_BARRIER_BIT;

		this.buffers.add(this.currentBuffer);
	}

	public void delete() {
		ringBuffers.delete();
	}

	@Override
	public AcceleratedBufferBuilder getBuffer(
			RenderType	renderType,
			Runnable	before,
			Runnable	after,
			int			layerIndex
	) {
		var layerKey	= new LayerKey					(layerIndex, renderType);
		var builder		= activeBuilders.get			(layerKey);
		var builders	= currentBuffer	.getBuilders	();
		var functions	= currentBuffer	.getFunctions	();
		var layers		= currentBuffer	.getLayers		();
		var layer		= layers		.get			(layerIndex);

		if (builder != null) {
			AccelStats.REUSE_HITS ++;

			var function = builder	.getFunction();
				function			.addBefore	(before);
				function			.addAfter	(after);

			return builder;
		}

		var function		= functions		.get				(layerIndex);
		var vertexBuffer	= currentBuffer	.getVertexBuffer	();
		var varyingBuffer	= currentBuffer	.getVaryingBuffer	();
		var elementSegment	= currentBuffer	.getElementSegment	();

		if (vertexBuffer == null) {
			currentBuffer	= ringBuffers	.get				(true);
			builders		= currentBuffer	.getBuilders		();
			functions		= currentBuffer	.getFunctions		();
			layers			= currentBuffer	.getLayers			();
			function		= functions		.get				(layerIndex);
			layer			= layers		.get				(layerIndex);

			vertexBuffer	= currentBuffer	.getVertexBuffer	();
			varyingBuffer	= currentBuffer	.getVaryingBuffer	();
			elementSegment	= currentBuffer	.getElementSegment	();

			buffers.add(currentBuffer);
		}

		if (layer == null) {
			function	= new CustomLayerFunction			();
			layer 		= CoreFeature	.createLayerStorage	();
			layers						.put				(layerIndex, layer);
			functions					.put				(layerIndex, function);
		}

		builder = new AcceleratedBufferBuilder(
				vertexBuffer,
				varyingBuffer,
				elementSegment,
				currentBuffer,
				function,
				layerKey
		);

		used = true;

		builders		.put		(layerKey, builder);
		function		.addBefore	(before);
		function		.addAfter	(after);
		activeBuilders	.put		(layerKey, builder);
		activeLayers	.add		(layerIndex);

		return builder;
	}

	public void prepareBuffers() {
		if (!used || prepared) {
			return;
		}

		// 光影下绘制被拆成两个时机（不透明 / 半透明，见 LevelRendererMixin），
		// prepareBuffers 会被调用两次。上传+变换只需做一次，否则同一批 builder
		// 会被重复 dispatch、drawContext 也会被重复投进 OPAQUE 桶导致重绘。
		prepared = true;

		AccelStats.PREPARE_CALLS ++;

		for (var buffer : buffers) {
			var elementBuffer	= buffer.getElementBuffer	();
			var builders		= buffer.getBuilders		();

			long tQuery = System.nanoTime();
			var program			= glGetInteger				(GL_CURRENT_PROGRAM);
			AccelStats.GL_QUERY_NANOS += System.nanoTime() - tQuery;

			if (builders.isEmpty()) {
				continue;
			}

			long tUpload = System.nanoTime();
			environment.selectMeshUploadingProgramDispatcher().dispatch	(builders.values(), buffer);
			long tTransform = System.nanoTime();
			environment.selectTransformProgramDispatcher	().dispatch	(builders.values());
			long tDone = System.nanoTime();

			AccelStats.UPLOAD_NANOS		+= tTransform - tUpload;
			AccelStats.TRANSFORM_NANOS	+= tDone - tTransform;
			AccelStats.DISPATCH_NANOS	+= tDone - tUpload;

			glMemoryBarrier(barriers);

			long tBuilder = System.nanoTime();

			for (var layerKey : builders.keySet()) {
				var builder = builders.get(layerKey);

				if (builder.isEmpty()) {
					continue;
				}

				AccelStats.BUILDER_COUNT ++;

				var drawContext		= buffer			.getDrawContext		();
				var elementSegment	= builder			.getElementSegment	();
				var renderType		= layerKey			.renderType			();
				var layer			= layerKey			.layer				();
				var drawType		= RenderTypeUtils	.getDrawType		(renderType);

				builder			.setOutdated();
				elementSegment	.setup		();

				drawContext.setupContext(
						builder,
						elementSegment,
						elementBuffer,
						renderType
				);

				buffer
						.getLayers	()
						.get		(layer)
						.get		(drawType)
						.add		(drawContext);

				barriers |= builder.getPolygonProgramDispatcher().dispatch(builder);
				barriers |= builder.getCullingProgramDispatcher().dispatch(builder);
			}

			AccelStats.BUILDER_NANOS += System.nanoTime() - tBuilder;

			long tUse = System.nanoTime();
			glUseProgram(program);
			AccelStats.USE_PROGRAM_NANOS += System.nanoTime() - tUse;
		}
	}

	public void drawBuffers(LayerDrawType drawType) {
		if (!used) {
			return;
		}

		// [临时探针] 实际绘制规模：层数 / buffer 数 / 最终 draw 数
		long probeStart = System.nanoTime();
		int  probeLayers	= 0;
		int  probeContexts	= 0;

		glMemoryBarrier(GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT
				|		GL_ELEMENT_ARRAY_BARRIER_BIT
				|		GL_COMMAND_BARRIER_BIT
		);

		for (		int layerIndex	: activeLayers) {
			probeLayers ++;
			for (	var buffer		: buffers) {
				var function = buffer.getFunctions	().getOrDefault(layerIndex, EmptyLayerFunction	.INSTANCE);
				var contexts = buffer.getLayers		().getOrDefault(layerIndex, EmptyLayerStorage	.INSTANCE).get(drawType);

				if (contexts.isEmpty()) {
					continue;
				}

				// 26.1: BufferUploader 已移除（GPU 状态由 GpuDevice 管理），bindDrawBuffers 前无需显式 invalidate
				buffer			.bindDrawBuffers();
				contexts		.prepare		();
				function		.runBefore		();

				// 26.1: 按输出目标分组共享 RenderPass（每个 draw 新建 pass 会导致极低帧率）
				var byTarget = new java.util.LinkedHashMap<com.mojang.blaze3d.pipeline.RenderTarget, java.util.List<com.namelessgod2008.core.buffers.accelerated.draw.pools.IDrawContextPool.IDrawContext>>();

				for (var drawContext : contexts) {
					var target = drawContext.getRenderTarget();

					if (target != null) {
						byTarget
								.computeIfAbsent	(target, t -> new java.util.ArrayList<>())
								.add				(drawContext);
					}
				}

				// 26.1: 准备阶段必须在【任何 RenderPass 打开之前】完成。
				// 解析纹理可能触发懒加载上传（writeToTexture），写入 DynamicTransforms 会 mapBuffer；
				// 二者都是命令编码，在打开的 pass 内执行会抛
				// "Close the existing render pass before performing additional commands"。
				for (var entry : byTarget.entrySet()) {
					for (var drawContext : entry.getValue()) {
						drawContext.prepareDraw();
					}
				}

				for (var entry : byTarget.entrySet()) {
					var target	= entry.getKey();
					var color	= com.mojang.blaze3d.systems.RenderSystem.outputColorTextureOverride != null
							? com.mojang.blaze3d.systems.RenderSystem.outputColorTextureOverride
							: target.getColorTextureView();
					var depth	= target.useDepth
							? (com.mojang.blaze3d.systems.RenderSystem.outputDepthTextureOverride != null
								? com.mojang.blaze3d.systems.RenderSystem.outputDepthTextureOverride
								: target.getDepthTextureView())
							: null;

					try (var pass = com.mojang.blaze3d.systems.RenderSystem
							.getDevice			()
							.createCommandEncoder()
							.createRenderPass	(
									() -> "acceleratedrendering",
									color,
									java.util.OptionalInt.empty(),
									depth,
									java.util.OptionalDouble.empty()
							)
					) {
						// 光影下把输出重定向到 Iris 的 gbuffer。
						//
						// createRenderPass 用传入纹理（= 主渲染目标）建并绑 FBO，绕过了 Iris
						// 的重定向；而 GlRenderPass 内部不持有 FBO 字段、存活期不再重绑，
						// 故此处重新绑定即对后续 drawIndexed 生效。
						// 未装光影时 redirector 为 null（或返回 false），不做任何事。
						var redirector = com.namelessgod2008.core.backends.GbufferBridge.get();

						if (redirector != null) {
							redirector.redirectToGbuffer();
						}

						com.mojang.blaze3d.systems.RenderSystem.bindDefaultUniforms(pass);

						for (var drawContext : entry.getValue()) {
							probeContexts ++;
							drawContext.drawElements(pass, drawContext.getRenderType().mode());
						}
					}
				}

				function.runAfter			();
				contexts.reset				();
				buffer	.unbindVertexArray	();
			}
		}

		// [临时探针] 累加实际绘制规模
		AccelStats.DRAW_LAYERS		+= probeLayers;
		AccelStats.DRAW_CONTEXTS	+= probeContexts;
		AccelStats.DRAW_WALK_NANOS	+= System.nanoTime() - probeStart;
	}

	public void clearBuffers() {
		if (!used) {
			return;
		}

		for (var buffer : buffers) {
			buffer.reset		();
			buffer.setInFlight	();
		}

		used			= false;
		prepared		= false;
		currentBuffer	= ringBuffers.get(false);

		environment		.clear	();
		activeBuilders	.clear	();
		activeLayers	.clear	();
		buffers			.clear	();
		buffers			.add	(currentBuffer);
	}
}
