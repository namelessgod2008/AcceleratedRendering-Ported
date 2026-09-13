package com.namelessgod2008.compat.iris.programs;

import com.namelessgod2008.compat.iris.programs.culling.IrisCullingProgramSelector;
import com.namelessgod2008.compat.iris.programs.processing.IrisPolygonProcessor;
import com.namelessgod2008.core.backends.programs.BarrierFlags;
import com.namelessgod2008.core.programs.LoadComputeShaderEvent;
import com.namelessgod2008.core.programs.culling.LoadCullingProgramSelectorEvent;
import com.namelessgod2008.core.programs.processing.LoadPolygonProcessorEvent;
import com.namelessgod2008.core.utils.ResourceLocationUtils;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;

public class IrisPrograms {

	public static final Identifier IRIS_BLOCK_VERTEX_TRANSFORM_KEY		= ResourceLocationUtils.create("compat_block_vertex_transform_iris");
	public static final Identifier IRIS_ENTITY_VERTEX_TRANSFORM_KEY		= ResourceLocationUtils.create("compat_entity_vertex_transform_iris");
	public static final Identifier IRIS_GLYPH_VERTEX_TRANSFORM_KEY		= ResourceLocationUtils.create("compat_glyph_vertex_transform_iris");
	public static final Identifier IRIS_BLOCK_QUAD_CULLING_KEY			= ResourceLocationUtils.create("compat_block_quad_cull_iris");
	public static final Identifier IRIS_BLOCK_TRIANGLE_CULLING_KEY		= ResourceLocationUtils.create("compat_block_triangle_cull_iris");
	public static final Identifier IRIS_ENTITY_QUAD_CULLING_KEY			= ResourceLocationUtils.create("compat_entity_quad_cull_iris");
	public static final Identifier IRIS_ENTITY_TRIANGLE_CULLING_KEY		= ResourceLocationUtils.create("compat_entity_triangle_cull_iris");
	public static final Identifier IRIS_BLOCK_QUAD_PROCESSING_KEY			= ResourceLocationUtils.create("compat_block_quad_processing_iris");
	public static final Identifier IRIS_BLOCK_TRIANGLE_PROCESSING_KEY		= ResourceLocationUtils.create("compat_block_triangle_processing_iris");
	public static final Identifier IRIS_ENTITY_QUAD_PROCESSING_KEY		= ResourceLocationUtils.create("compat_entity_quad_processing_iris");
	public static final Identifier IRIS_ENTITY_TRIANGLE_PROCESSING_KEY	= ResourceLocationUtils.create("compat_entity_triangle_processing_iris");
	public static final Identifier IRIS_GLYPH_QUAD_PROCESSING_KEY			= ResourceLocationUtils.create("compat_glyph_quad_processing_iris");
	public static final Identifier IRIS_GLYPH_TRIANGLE_PROCESSING_KEY		= ResourceLocationUtils.create("compat_glyph_triangle_processing_iris");
	public static final Identifier IRIS_BLOCK_MESH_UPLOADING_KEY			= ResourceLocationUtils.create("compat_block_mesh_uploading_iris");
	public static final Identifier IRIS_ENTITY_MESH_UPLOADING_KEY			= ResourceLocationUtils.create("compat_entity_mesh_uploading_iris");
	public static final Identifier IRIS_GLYPH_MESH_UPLOADING_KEY			= ResourceLocationUtils.create("compat_glyph_mesh_uploading_iris");

	@SubscribeEvent
	public static void onLoadComputeShaders(LoadComputeShaderEvent event) {
		event.loadComputeShader(
				IRIS_BLOCK_VERTEX_TRANSFORM_KEY,
				ResourceLocationUtils	.create("shaders/compat/transform/iris_block_vertex_transform_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);

		event.loadComputeShader(
				IRIS_ENTITY_VERTEX_TRANSFORM_KEY,
				ResourceLocationUtils	.create("shaders/compat/transform/iris_entity_vertex_transform_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);

		event.loadComputeShader(
				IRIS_GLYPH_VERTEX_TRANSFORM_KEY,
				ResourceLocationUtils	.create("shaders/compat/transform/iris_glyph_vertex_transform_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);

		event.loadComputeShader(
				IRIS_BLOCK_QUAD_CULLING_KEY,
				ResourceLocationUtils	.create("shaders/compat/culling/iris_block_quad_culling_shader.compute"),
				BarrierFlags			.SHADER_STORAGE,
				BarrierFlags			.ATOMIC_COUNTER
		);

		event.loadComputeShader(
				IRIS_BLOCK_TRIANGLE_CULLING_KEY,
				ResourceLocationUtils	.create("shaders/compat/culling/iris_block_triangle_culling_shader.compute"),
				BarrierFlags			.SHADER_STORAGE,
				BarrierFlags			.ATOMIC_COUNTER
		);

		event.loadComputeShader(
				IRIS_ENTITY_QUAD_CULLING_KEY,
				ResourceLocationUtils	.create("shaders/compat/culling/iris_entity_quad_culling_shader.compute"),
				BarrierFlags			.SHADER_STORAGE,
				BarrierFlags			.ATOMIC_COUNTER
		);

		event.loadComputeShader(
				IRIS_ENTITY_TRIANGLE_CULLING_KEY,
				ResourceLocationUtils	.create("shaders/compat/culling/iris_entity_triangle_culling_shader.compute"),
				BarrierFlags			.SHADER_STORAGE,
				BarrierFlags			.ATOMIC_COUNTER
		);

		event.loadComputeShader(
				IRIS_BLOCK_QUAD_PROCESSING_KEY,
				ResourceLocationUtils	.create("shaders/compat/processing/iris_block_quad_processing_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);

		event.loadComputeShader(
				IRIS_BLOCK_TRIANGLE_PROCESSING_KEY,
				ResourceLocationUtils	.create("shaders/compat/processing/iris_block_triangle_processing_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);

		event.loadComputeShader(
				IRIS_ENTITY_QUAD_PROCESSING_KEY,
				ResourceLocationUtils	.create("shaders/compat/processing/iris_entity_quad_processing_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);

		event.loadComputeShader(
				IRIS_ENTITY_TRIANGLE_PROCESSING_KEY,
				ResourceLocationUtils	.create("shaders/compat/processing/iris_entity_triangle_processing_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);

		event.loadComputeShader(
				IRIS_GLYPH_QUAD_PROCESSING_KEY,
				ResourceLocationUtils	.create("shaders/compat/processing/iris_glyph_quad_processing_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);

		event.loadComputeShader(
				IRIS_GLYPH_TRIANGLE_PROCESSING_KEY,
				ResourceLocationUtils	.create("shaders/compat/processing/iris_glyph_triangle_processing_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);

		event.loadComputeShader(
				IRIS_BLOCK_MESH_UPLOADING_KEY,
				ResourceLocationUtils	.create("shaders/compat/uploading/iris_block_mesh_uploading_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);

		event.loadComputeShader(
				IRIS_ENTITY_MESH_UPLOADING_KEY,
				ResourceLocationUtils	.create("shaders/compat/uploading/iris_entity_mesh_uploading_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);

		event.loadComputeShader(
				IRIS_GLYPH_MESH_UPLOADING_KEY,
				ResourceLocationUtils	.create("shaders/compat/uploading/iris_glyph_mesh_uploading_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);
	}

	@SubscribeEvent
	public static void onLoadCullingPrograms(LoadCullingProgramSelectorEvent event) {
		event.loadFor(IrisVertexFormats.TERRAIN, parent -> new IrisCullingProgramSelector(
				parent,
				IRIS_BLOCK_QUAD_CULLING_KEY,
				IRIS_BLOCK_TRIANGLE_CULLING_KEY
		));

		event.loadFor(IrisVertexFormats.ENTITY, parent -> new IrisCullingProgramSelector(
				parent,
				IRIS_ENTITY_QUAD_CULLING_KEY,
				IRIS_ENTITY_TRIANGLE_CULLING_KEY
		));
	}

	@SubscribeEvent
	public static void onLoadPolygonProcessors(LoadPolygonProcessorEvent event) {
		event.loadFor(IrisVertexFormats.TERRAIN, parent -> new IrisPolygonProcessor(
				parent,
				IRIS_BLOCK_QUAD_PROCESSING_KEY,
				IRIS_BLOCK_TRIANGLE_PROCESSING_KEY
		));

		event.loadFor(IrisVertexFormats.ENTITY, parent -> new IrisPolygonProcessor(
				parent,
				IRIS_ENTITY_QUAD_PROCESSING_KEY,
				IRIS_ENTITY_TRIANGLE_PROCESSING_KEY
		));

		event.loadFor(IrisVertexFormats.GLYPH, parent -> new IrisPolygonProcessor(
				parent,
				IRIS_GLYPH_QUAD_PROCESSING_KEY,
				IRIS_GLYPH_TRIANGLE_PROCESSING_KEY
		));
	}
}
