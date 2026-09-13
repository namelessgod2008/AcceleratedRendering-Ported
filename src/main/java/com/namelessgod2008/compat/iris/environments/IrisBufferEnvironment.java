package com.namelessgod2008.compat.iris.environments;

import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.core.backends.buffers.IServerBuffer;
import com.namelessgod2008.core.buffers.accelerated.draw.IDrawMethod;
import com.namelessgod2008.core.buffers.environments.IBufferEnvironment;
import com.namelessgod2008.core.buffers.memory.VertexLayout;
import com.namelessgod2008.core.meshes.ServerMesh;
import com.namelessgod2008.core.programs.culling.ICullingProgramDispatcher;
import com.namelessgod2008.core.programs.culling.ICullingProgramSelector;
import com.namelessgod2008.core.programs.dispatchers.IPolygonProgramDispatcher;
import com.namelessgod2008.core.programs.dispatchers.meshes.MeshUploadingProgramDispatcher;
import com.namelessgod2008.core.programs.dispatchers.TransformProgramDispatcher;
import com.namelessgod2008.core.programs.overrides.*;
import com.namelessgod2008.core.programs.processing.IPolygonProcessor;
import com.namelessgod2008.core.programs.processing.LoadPolygonProcessorEvent;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModLoader;

import java.util.Set;

public class IrisBufferEnvironment implements IBufferEnvironment {

	private final IBufferEnvironment vanillaSubSet;
	private final IBufferEnvironment irisSubSet;

	public IrisBufferEnvironment(
			IBufferEnvironment	vanillaSubSet,
			VertexFormat		vanillaVertexFormat,
			VertexFormat		irisVertexFormat,
			Identifier	meshUploadingProgramKey,
			Identifier	transformProgramKey
	) {
		this.vanillaSubSet	= vanillaSubSet;
		this.irisSubSet		= new IrisSubSet(
				vanillaVertexFormat,
				irisVertexFormat,
				meshUploadingProgramKey,
				transformProgramKey
		);
	}

	private IBufferEnvironment getSubSet() {
		return useIris() ? irisSubSet : vanillaSubSet;
	}

	@Override
	public void setupBufferState() {
		getSubSet().setupBufferState();
	}

	@Override
	public void clear() {
		getSubSet().clear();
	}

	@Override
	public Set<VertexFormat> getVertexFormats() {
		return irisSubSet.getVertexFormats();
	}

	@Override
	public VertexLayout getLayout() {
		return getSubSet().getLayout();
	}

	@Override
	public ProgramOverride getProgramOverride(RenderType renderType) {
		return getSubSet().getProgramOverride(renderType);
	}

	@Override
	public ProgramOverride getProgramOverride(int overrideId) {
		return getSubSet().getProgramOverride(overrideId);
	}

	@Override
	public MeshUploadingProgramDispatcher selectMeshUploadingProgramDispatcher() {
		return getSubSet().selectMeshUploadingProgramDispatcher();
	}

	@Override
	public TransformProgramDispatcher selectTransformProgramDispatcher() {
		return getSubSet().selectTransformProgramDispatcher();
	}

	@Override
	public ICullingProgramDispatcher selectCullingProgramDispatcher(RenderType renderType) {
		return getSubSet().selectCullingProgramDispatcher(renderType);
	}

	@Override
	public IPolygonProgramDispatcher selectProcessingProgramDispatcher(VertexFormat.Mode mode) {
		return getSubSet().selectProcessingProgramDispatcher(mode);
	}

	@Override
	public boolean isAccelerated(VertexFormat vertexFormat) {
		return getSubSet().isAccelerated(vertexFormat);
	}

	@Override
	public IDrawMethod getDrawMethod() {
		return getSubSet().getDrawMethod();
	}

	@Override
	public int getVertexSize() {
		return getSubSet().getVertexSize();
	}

	@Override
	public int getOverrideCount() {
		return getSubSet().getOverrideCount();
	}

	public boolean useIris() {
		return IrisApi.getInstance().isShaderPackInUse() && ImmediateState.isRenderingLevel;
	}

	public static class IrisSubSet implements IBufferEnvironment {

		private final VertexFormat						vanillaVertexFormat;
		private final VertexFormat						irisVertexFormat;
		private final VertexLayout						layout;
		private final IDrawMethod						method;

		private final IShaderProgramOverrides			shaderProgramOverrides;
		private final MeshUploadingProgramDispatcher	meshUploadingProgramDispatcher;
		private final TransformProgramDispatcher		transformProgramDispatcher;
		private final ICullingProgramSelector			cullingProgramSelector;
		private final IPolygonProcessor					polygonProcessor;

		public IrisSubSet(
				VertexFormat		vanillaVertexFormat,
				VertexFormat		irisVertexFormat,
				Identifier	uploadingProgramKey,
				Identifier	transformProgramKey
		) {
			var defaultTransformOverride		= new TransformProgramDispatcher	.Default(transformProgramKey, 4L * 4L);
			var defaultUploadingOverride		= new MeshUploadingProgramDispatcher.Default(uploadingProgramKey, 9L * 4L);

			this.vanillaVertexFormat			= vanillaVertexFormat;
			this.irisVertexFormat				= irisVertexFormat;
			this.layout							= new VertexLayout(this.irisVertexFormat);
			this.method							= CoreFeature.getDrawMethod();

			this.shaderProgramOverrides			= ModLoader		.postEventWithReturn(new LoadShaderProgramOverridesEvent(this.irisVertexFormat)).getOverrides(defaultTransformOverride, defaultUploadingOverride);
			this.polygonProcessor				= ModLoader		.postEventWithReturn(new LoadPolygonProcessorEvent		(this.irisVertexFormat)).getProcessor();
			this.cullingProgramSelector			= this.method	.getCullingProgramSelector								(this.irisVertexFormat);

			this.meshUploadingProgramDispatcher	= new MeshUploadingProgramDispatcher();
			this.transformProgramDispatcher		= new TransformProgramDispatcher	();
		}

		@Override
		public void setupBufferState() {
			irisVertexFormat.setupBufferState();
		}

		@Override
		public void clear() {
			meshUploadingProgramDispatcher.clear();
		}

		@Override
		public boolean isAccelerated(VertexFormat vertexFormat) {
			return this.vanillaVertexFormat == vertexFormat || this.irisVertexFormat == vertexFormat;
		}

		@Override
		public Set<VertexFormat> getVertexFormats() {
			return Set.of(vanillaVertexFormat, irisVertexFormat);
		}

		@Override
		public VertexLayout getLayout() {
			return layout;
		}

		@Override
		public ProgramOverride getProgramOverride(RenderType renderType) {
			return shaderProgramOverrides.getOverride(renderType);
		}

		@Override
		public ProgramOverride getProgramOverride(int overrideId) {
			return shaderProgramOverrides.getOverride(overrideId);
		}

		@Override
		public MeshUploadingProgramDispatcher selectMeshUploadingProgramDispatcher() {
			return meshUploadingProgramDispatcher;
		}

		@Override
		public TransformProgramDispatcher selectTransformProgramDispatcher() {
			return transformProgramDispatcher;
		}

		@Override
		public ICullingProgramDispatcher selectCullingProgramDispatcher(RenderType renderType) {
			return cullingProgramSelector.select(renderType);
		}

		@Override
		public IPolygonProgramDispatcher selectProcessingProgramDispatcher(VertexFormat.Mode mode) {
			return polygonProcessor.select(mode);
		}

		@Override
		public IDrawMethod getDrawMethod() {
			return method;
		}

		@Override
		public int getVertexSize() {
			return irisVertexFormat.getVertexSize();
		}

		@Override
		public int getOverrideCount() {
			return shaderProgramOverrides.getCount();
		}
	}
}
