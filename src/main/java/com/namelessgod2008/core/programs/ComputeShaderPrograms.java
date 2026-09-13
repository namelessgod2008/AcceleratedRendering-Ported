package com.namelessgod2008.core.programs;

import com.namelessgod2008.core.backends.programs.BarrierFlags;
import com.namelessgod2008.core.utils.ResourceLocationUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;


@SuppressWarnings("removal")
public class ComputeShaderPrograms {

	public static final Identifier CORE_BLOCK_VERTEX_TRANSFORM_KEY				= ResourceLocationUtils.create("core_block_vertex_transform");
	public static final Identifier CORE_ENTITY_VERTEX_TRANSFORM_KEY				= ResourceLocationUtils.create("core_entity_vertex_transform");
	public static final Identifier CORE_POS_VERTEX_TRANSFORM_KEY					= ResourceLocationUtils.create("core_pos_vertex_transform");
	public static final Identifier CORE_POS_COLOR_VERTEX_TRANSFORM_KEY			= ResourceLocationUtils.create("core_pos_color_vertex_transform");
	public static final Identifier CORE_POS_TEX_VERTEX_TRANSFORM_KEY				= ResourceLocationUtils.create("core_pos_tex_vertex_transform");
	public static final Identifier CORE_POS_TEX_COLOR_VERTEX_TRANSFORM_KEY		= ResourceLocationUtils.create("core_pos_tex_color_vertex_transform");
	public static final Identifier CORE_POS_COLOR_TEX_LIGHT_VERTEX_TRANSFORM_KEY	= ResourceLocationUtils.create("core_pos_color_tex_light_vertex_transform");
	public static final Identifier CORE_PASS_THROUGH_QUAD_CULLING_KEY				= ResourceLocationUtils.create("core_pass_through_quad_culling");
	public static final Identifier CORE_PASS_THROUGH_TRIANGLE_CULLING_KEY			= ResourceLocationUtils.create("core_pass_through_triangle_culling");
	public static final Identifier CORE_BLOCK_MESH_UPLOADING_KEY					= ResourceLocationUtils.create("core_block_mesh_uploading_key");
	public static final Identifier CORE_ENTITY_MESH_UPLOADING_KEY					= ResourceLocationUtils.create("core_entity_mesh_uploading");
	public static final Identifier CORE_POS_MESH_UPLOADING_KEY					= ResourceLocationUtils.create("core_pos_mesh_uploading");
	public static final Identifier CORE_POS_COLOR_MESH_UPLOADING_KEY				= ResourceLocationUtils.create("core_pos_color_mesh_uploading");
	public static final Identifier CORE_POS_TEX_MESH_UPLOADING_KEY				= ResourceLocationUtils.create("core_pos_tex_mesh_uploading");
	public static final Identifier CORE_POS_TEX_COLOR_MESH_UPLOADING_KEY			= ResourceLocationUtils.create("core_pos_tex_color_mesh_uploading");
	public static final Identifier CORE_POS_COLOR_TEX_LIGHT_MESH_UPLOADING_KEY	= ResourceLocationUtils.create("core_pos_color_tex_light_mesh_uploading");

	@SubscribeEvent
	public static void onLoadComputeShaders(LoadComputeShaderEvent event) {
		event.loadComputeShader(
				CORE_BLOCK_VERTEX_TRANSFORM_KEY,
				ResourceLocationUtils	.create("shaders/core/transform/block_vertex_transform_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);

		event.loadComputeShader(
				CORE_ENTITY_VERTEX_TRANSFORM_KEY,
				ResourceLocationUtils	.create("shaders/core/transform/entity_vertex_transform_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);

		event.loadComputeShader(
				CORE_POS_VERTEX_TRANSFORM_KEY,
				ResourceLocationUtils	.create("shaders/core/transform/pos_vertex_transform_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);

		event.loadComputeShader(
				CORE_POS_COLOR_VERTEX_TRANSFORM_KEY,
				ResourceLocationUtils	.create("shaders/core/transform/pos_color_vertex_transform_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);

		event.loadComputeShader(
				CORE_POS_TEX_VERTEX_TRANSFORM_KEY,
				ResourceLocationUtils	.create("shaders/core/transform/pos_tex_vertex_transform_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);

		event.loadComputeShader(
				CORE_POS_TEX_COLOR_VERTEX_TRANSFORM_KEY,
				ResourceLocationUtils	.create("shaders/core/transform/pos_tex_color_vertex_transform_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);

		event.loadComputeShader(
				CORE_POS_COLOR_TEX_LIGHT_VERTEX_TRANSFORM_KEY,
				ResourceLocationUtils	.create("shaders/core/transform/pos_color_tex_light_vertex_transform_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);

		event.loadComputeShader(
				CORE_PASS_THROUGH_QUAD_CULLING_KEY,
				ResourceLocationUtils	.create("shaders/core/culling/pass_through_quad_culling_shader.compute"),
				BarrierFlags			.SHADER_STORAGE,
				BarrierFlags			.ATOMIC_COUNTER
		);

		event.loadComputeShader(
				CORE_PASS_THROUGH_TRIANGLE_CULLING_KEY,
				ResourceLocationUtils	.create("shaders/core/culling/pass_through_triangle_culling_shader.compute"),
				BarrierFlags			.SHADER_STORAGE,
				BarrierFlags			.ATOMIC_COUNTER
		);

		event.loadComputeShader(
				CORE_BLOCK_MESH_UPLOADING_KEY,
				ResourceLocationUtils	.create("shaders/core/uploading/block_mesh_uploading_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);

		event.loadComputeShader(
				CORE_ENTITY_MESH_UPLOADING_KEY,
				ResourceLocationUtils	.create("shaders/core/uploading/entity_mesh_uploading_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);

		event.loadComputeShader(
				CORE_POS_MESH_UPLOADING_KEY,
				ResourceLocationUtils	.create("shaders/core/uploading/pos_mesh_uploading_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);

		event.loadComputeShader(
				CORE_POS_COLOR_MESH_UPLOADING_KEY,
				ResourceLocationUtils	.create("shaders/core/uploading/pos_color_mesh_uploading_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);

		event.loadComputeShader(
				CORE_POS_TEX_MESH_UPLOADING_KEY,
				ResourceLocationUtils	.create("shaders/core/uploading/pos_tex_mesh_uploading_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);

		event.loadComputeShader(
				CORE_POS_TEX_COLOR_MESH_UPLOADING_KEY,
				ResourceLocationUtils	.create("shaders/core/uploading/pos_tex_color_mesh_uploading_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);

		event.loadComputeShader(
				CORE_POS_COLOR_TEX_LIGHT_MESH_UPLOADING_KEY,
				ResourceLocationUtils	.create("shaders/core/uploading/pos_color_tex_light_mesh_uploading_shader.compute"),
				BarrierFlags			.SHADER_STORAGE
		);
	}
}
