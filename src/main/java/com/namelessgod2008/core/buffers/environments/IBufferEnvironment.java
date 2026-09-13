package com.namelessgod2008.core.buffers.environments;

import com.namelessgod2008.core.buffers.accelerated.draw.IDrawMethod;
import com.namelessgod2008.core.buffers.memory.VertexLayout;
import com.namelessgod2008.core.programs.ComputeShaderPrograms;
import com.namelessgod2008.core.programs.culling.ICullingProgramDispatcher;
import com.namelessgod2008.core.programs.dispatchers.IPolygonProgramDispatcher;
import com.namelessgod2008.core.programs.dispatchers.meshes.MeshUploadingProgramDispatcher;
import com.namelessgod2008.core.programs.dispatchers.TransformProgramDispatcher;
import com.namelessgod2008.core.programs.overrides.ProgramOverride;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.Set;

public interface IBufferEnvironment {

	int									getVertexSize						();
	int									getOverrideCount					();
	IDrawMethod							getDrawMethod						();
	Set<VertexFormat>					getVertexFormats					();
	VertexLayout						getLayout							();
	MeshUploadingProgramDispatcher		selectMeshUploadingProgramDispatcher();
	TransformProgramDispatcher			selectTransformProgramDispatcher	();
	ProgramOverride						getProgramOverride					(int				overrideId);
	ProgramOverride						getProgramOverride					(RenderType			renderType);
	ICullingProgramDispatcher			selectCullingProgramDispatcher		(RenderType			renderType);
	IPolygonProgramDispatcher			selectProcessingProgramDispatcher	(VertexFormat.Mode	mode);
	boolean								isAccelerated						(VertexFormat		vertexFormat);
	void								setupBufferState					();
	void								clear								();

	class Presets {

		public static final IBufferEnvironment BLOCK				= new VanillaBufferEnvironment(DefaultVertexFormat.BLOCK,						ComputeShaderPrograms.CORE_BLOCK_MESH_UPLOADING_KEY,				ComputeShaderPrograms.CORE_BLOCK_VERTEX_TRANSFORM_KEY);
		public static final IBufferEnvironment ENTITY				= new VanillaBufferEnvironment(DefaultVertexFormat.ENTITY,						ComputeShaderPrograms.CORE_ENTITY_MESH_UPLOADING_KEY,				ComputeShaderPrograms.CORE_ENTITY_VERTEX_TRANSFORM_KEY);
		public static final IBufferEnvironment POS					= new VanillaBufferEnvironment(DefaultVertexFormat.POSITION,					ComputeShaderPrograms.CORE_POS_MESH_UPLOADING_KEY,					ComputeShaderPrograms.CORE_POS_VERTEX_TRANSFORM_KEY);
		public static final IBufferEnvironment POS_COLOR			= new VanillaBufferEnvironment(DefaultVertexFormat.POSITION_COLOR,				ComputeShaderPrograms.CORE_POS_COLOR_MESH_UPLOADING_KEY,			ComputeShaderPrograms.CORE_POS_COLOR_VERTEX_TRANSFORM_KEY);
		public static final IBufferEnvironment POS_TEX				= new VanillaBufferEnvironment(DefaultVertexFormat.POSITION_TEX,				ComputeShaderPrograms.CORE_POS_TEX_MESH_UPLOADING_KEY,				ComputeShaderPrograms.CORE_POS_TEX_VERTEX_TRANSFORM_KEY);
		public static final IBufferEnvironment POS_TEX_COLOR		= new VanillaBufferEnvironment(DefaultVertexFormat.POSITION_TEX_COLOR,			ComputeShaderPrograms.CORE_POS_TEX_COLOR_MESH_UPLOADING_KEY,		ComputeShaderPrograms.CORE_POS_TEX_COLOR_VERTEX_TRANSFORM_KEY);
		public static final IBufferEnvironment POS_COLOR_TEX_LIGHT	= new VanillaBufferEnvironment(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,	ComputeShaderPrograms.CORE_POS_COLOR_TEX_LIGHT_MESH_UPLOADING_KEY,	ComputeShaderPrograms.CORE_POS_COLOR_TEX_LIGHT_VERTEX_TRANSFORM_KEY);
	}
}
