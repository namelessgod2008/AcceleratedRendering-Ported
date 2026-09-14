package com.namelessgod2008.core.utils;

import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.namelessgod2008.core.buffers.accelerated.layers.LayerDrawType;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import static org.lwjgl.opengl.GL46.GL_LINES;
import static org.lwjgl.opengl.GL46.GL_LINE_STRIP;
import static org.lwjgl.opengl.GL46.GL_POINTS;
import static org.lwjgl.opengl.GL46.GL_TRIANGLES;
import static org.lwjgl.opengl.GL46.GL_TRIANGLE_FAN;
import static org.lwjgl.opengl.GL46.GL_TRIANGLE_STRIP;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 26.1 RenderTypeUtils — 适配 26.1 的 RenderType/RenderSetup 重构。
 *
 * 1.21.4 中 RenderType 有 CompositeRenderType/CompositeState/RenderStateShard 内部结构；
 * 26.1 中 RenderType 只暴露 create(String, RenderSetup) + 少量公开访问器（pipeline/format/mode/
 * sortOnUpload 等），纹理与状态封装在私有 RenderSetup 中。此工具类改用：
 * - 公开 API（pipeline()/sortOnUpload()）判断 cull/depth/translucent
 * - 反射访问 RenderType.state → RenderSetup.textures，恢复 getTextureLocation（26.1 无公开等价）
 */
public class RenderTypeUtils {

	public static final Map<RenderType, RenderType> WITH_DEPTH_CACHE = new Object2ObjectOpenHashMap<>();

	private static Field STATE_FIELD;
	private static Field TEXTURES_FIELD;
	private static Method TEXTURE_BINDING_LOCATION;
	private static Method GET_TEXTURES_METHOD;
	private static Field TEXTURE_TRANSFORM_FIELD;
	private static Method GET_MATRIX_METHOD;

	static {
		try {
			Class<?> renderTypeClass = RenderType.class;
			STATE_FIELD = renderTypeClass.getDeclaredField("state");
			STATE_FIELD.setAccessible(true);

			Class<?> renderSetupClass = STATE_FIELD.getType(); // RenderSetup class
			TEXTURES_FIELD = renderSetupClass.getDeclaredField("textures");
			TEXTURES_FIELD.setAccessible(true);

			// TextureBinding record 的 location() 方法
			Class<?> bindingClass = Class.forName("net.minecraft.client.renderer.rendertype.RenderSetup$TextureBinding");
			TEXTURE_BINDING_LOCATION = bindingClass.getDeclaredMethod("location");
			TEXTURE_BINDING_LOCATION.setAccessible(true);

			// RenderSetup.getTextures() → Map<String, TextureAndSampler>（RenderPass 绑定纹理用）
			GET_TEXTURES_METHOD = renderSetupClass.getDeclaredMethod("getTextures");
			GET_TEXTURES_METHOD.setAccessible(true);

			// RenderSetup.textureTransform → TextureTransform.getMatrix()（DynamicTransforms uniform 用）
			TEXTURE_TRANSFORM_FIELD = renderSetupClass.getDeclaredField("textureTransform");
			TEXTURE_TRANSFORM_FIELD.setAccessible(true);
			GET_MATRIX_METHOD = Class.forName("net.minecraft.client.renderer.rendertype.TextureTransform").getDeclaredMethod("getMatrix");
			GET_MATRIX_METHOD.setAccessible(true);
		} catch (ReflectiveOperationException e) {
			// 26.1 内部字段名若变化，此处失败，getTextureLocation 降级为 null
		}
	}

	/**
	 * 26.1: 反射读取 RenderType 的纹理集合（RenderPass.bindTexture 用）。
	 *
	 * 与原版 RenderType.draw 行为一致：每次调用都重新解析，避免缓存到过期/未就绪的
	 * GpuTextureView（尤其 Sampler1/Sampler2 指向的 overlay/lightmap 会随重载替换）。
	 * 返回 Map&lt;String, TextureAndSampler&gt;；失败返回空 Map。
	 */
	// 26.1: RenderSetup.getTextures() 每次调用都会新建 Map 并逐个向 TextureManager 解析
	// 纹理视图，而 mod 每帧有大量 draw，故按 RenderType 缓存。空结果不缓存（避免把
	// 一次解析失败永久固化），资源重载时由 clearCaches() 失效。
	private static final Map<RenderType, java.util.Map<String, net.minecraft.client.renderer.rendertype.RenderSetup.TextureAndSampler>> TEXTURE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

	@SuppressWarnings("unchecked")
	public static java.util.Map<String, net.minecraft.client.renderer.rendertype.RenderSetup.TextureAndSampler> getTextures(RenderType renderType) {
		if (renderType == null) {
			reportOnce("renderType == null");
			return java.util.Collections.emptyMap();
		}

		var cached = TEXTURE_CACHE.get(renderType);

		if (cached != null) {
			return cached;
		}

		var textures = resolveTextures(renderType);

		if (!textures.isEmpty()) {
			TEXTURE_CACHE.put(renderType, textures);
		}

		return textures;
	}

	@SuppressWarnings("unchecked")
	private static java.util.Map<String, net.minecraft.client.renderer.rendertype.RenderSetup.TextureAndSampler> resolveTextures(RenderType renderType) {
		if (GET_TEXTURES_METHOD == null) {
			reportOnce("GET_TEXTURES_METHOD == null（静态块反射初始化失败）");
			return java.util.Collections.emptyMap();
		}

		try {
			Object state = STATE_FIELD.get(renderType);

			if (state == null) {
				reportOnce("RenderType.state == null");
				return java.util.Collections.emptyMap();
			}

			var textures = (java.util.Map<String, net.minecraft.client.renderer.rendertype.RenderSetup.TextureAndSampler>) GET_TEXTURES_METHOD.invoke(state);

			if (textures == null || textures.isEmpty()) {
				reportOnce("getTextures() 返回空：" + (textures == null ? "null" : "emptyMap"));
				return java.util.Collections.emptyMap();
			}

			return textures;
		} catch (java.lang.reflect.InvocationTargetException e) {
			var cause = e.getCause();

			reportOnce("getTextures() 内部异常: " + cause);

			if (cause != null) {
				cause.printStackTrace(System.out);
			}

			return java.util.Collections.emptyMap();
		} catch (ReflectiveOperationException e) {
			reportOnce("反射调用失败: " + e);
			e.printStackTrace(System.out);
			return java.util.Collections.emptyMap();
		}
	}

	private static boolean TEXTURE_DIAGNOSED = false;

	private static void reportOnce(String message) {
		if (TEXTURE_DIAGNOSED) {
			return;
		}

		TEXTURE_DIAGNOSED = true;
		System.out.println("[AR-TEX] " + message);
	}

	/** 资源重载时失效纹理缓存（纹理视图可能被替换）。 */
	public static void clearCaches() {
		TEXTURE_CACHE.clear();
		TEXTURE_MATRIX_CACHE.clear();
	}

	// 纹理变换矩阵按 RenderType 缓存（绝大多数 RenderType 为 default_texturing，矩阵恒为 identity）
	private static final Map<RenderType, org.joml.Matrix4f> TEXTURE_MATRIX_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

	/**
	 * 26.1: 反射读取 RenderType 的纹理变换矩阵（DynamicTransforms uniform 用）。
	 * 失败返回单位矩阵。
	 */
	public static org.joml.Matrix4f getTextureMatrix(RenderType renderType) {
		if (renderType == null || TEXTURE_TRANSFORM_FIELD == null || GET_MATRIX_METHOD == null) {
			return new org.joml.Matrix4f();
		}

		return TEXTURE_MATRIX_CACHE.computeIfAbsent(renderType, type -> {
			try {
				Object state = STATE_FIELD.get(type);

				if (state == null) {
					return new org.joml.Matrix4f();
				}

				Object textureTransform = TEXTURE_TRANSFORM_FIELD.get(state);

				if (textureTransform == null) {
					return new org.joml.Matrix4f();
				}

				return new org.joml.Matrix4f((org.joml.Matrix4f) GET_MATRIX_METHOD.invoke(textureTransform));
			} catch (ReflectiveOperationException e) {
				return new org.joml.Matrix4f();
			}
		});
	}

	/**
	 * 26.1: 从 RenderType 的私有 RenderSetup.textures 中取第一个纹理 location。
	 * 1.21.4 语义：textureState.cutoutTexture()。26.1 用反射（RenderType 无公开纹理 getter）。
	 */
	public static Identifier getTextureLocation(RenderType renderType) {
		if (renderType == null) {
			return null;
		}

		try {
			Object state = STATE_FIELD.get(renderType);
			if (state == null) {
				return null;
			}

			Object textures = TEXTURES_FIELD.get(state);
			if (!(textures instanceof Map<?, ?> map) || map.isEmpty()) {
				return null;
			}

			// 取第一个纹理绑定，调用其 location() 方法得到 Identifier
			Object firstBinding = map.values().stream().findFirst().orElse(null);
			if (firstBinding == null) {
				return null;
			}

			Object location = TEXTURE_BINDING_LOCATION.invoke(firstBinding);
			return location instanceof Identifier id ? id : null;
		} catch (IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
			return null;
		}
	}

	private static Field LAYERING_TRANSFORM_FIELD;
	private static Method GET_MODIFIER_METHOD;

	static {
		try {
			Class<?> renderSetupClass = Class.forName("net.minecraft.client.renderer.rendertype.RenderSetup");
			LAYERING_TRANSFORM_FIELD = renderSetupClass.getDeclaredField("layeringTransform");
			LAYERING_TRANSFORM_FIELD.setAccessible(true);

			GET_MODIFIER_METHOD = Class.forName("net.minecraft.client.renderer.rendertype.LayeringTransform")
					.getDeclaredMethod("getModifier");
			GET_MODIFIER_METHOD.setAccessible(true);
		} catch (ReflectiveOperationException e) {
			// 26.1 内部字段名若变化，此处失败，applyLayeringTransform 降级为 no-op
		}
	}

	/**
	 * 26.1: 应用 RenderType 的 layeringTransform 到 modelView 矩阵。
	 *
	 * <p>原版 {@code RenderType.draw} 在做 DynamicTransforms 之前会 push modelView 栈并应用该变换
	 * （见 {@code LayeringTransform.VIEW_OFFSET_Z_LAYERING}），作用是沿深度方向微调，避免与共面几何
	 * （如方块表面）z-fighting。加速路径自行拼 DynamicTransforms，必须补上这一步，否则
	 * entity_shadow / entity_cutout / entity_solid / armor_cutout_no_cull / banner_pattern 等
	 * 带 layering 的 RenderType 会闪烁。
	 *
	 * <p>无 layering（modifier 为 null）时原样返回入参，不产生额外分配。
	 */
	@SuppressWarnings("unchecked")
	public static org.joml.Matrix4f applyLayeringTransform(RenderType renderType, org.joml.Matrix4f modelView) {
		if (renderType == null || LAYERING_TRANSFORM_FIELD == null || GET_MODIFIER_METHOD == null) {
			return modelView;
		}

		try {
			Object state = STATE_FIELD.get(renderType);

			if (state == null) {
				return modelView;
			}

			Object layeringTransform = LAYERING_TRANSFORM_FIELD.get(state);

			if (layeringTransform == null) {
				return modelView;
			}

			var modifier = (java.util.function.Consumer<org.joml.Matrix4fStack>) GET_MODIFIER_METHOD.invoke(layeringTransform);

			if (modifier == null) {
				return modelView;
			}

			// 原版是在 modelViewStack 上 push 后施加；此处无栈，改为直接复合到给定矩阵。
			// 变换形式为 scale/translate（见 ProjectionType），与矩阵左乘等价。
			var stack = new org.joml.Matrix4fStack(4);
			stack.set(modelView);
			modifier.accept(stack);
			return new org.joml.Matrix4f(stack);
		} catch (ReflectiveOperationException e) {
			return modelView;
		}
	}

	public static boolean isCulled(RenderType renderType) {
		if (renderType == null) {
			return false;
		}

		return renderType.pipeline().isCull();
	}

	/**
	 * 26.1: 动态纹理由 RenderSetup.textureTransform 表达；此处反射判断是否非默认。
	 * 若反射失败降级为 false（非动态）。
	 */
	public static boolean isDynamic(RenderType renderType) {
		if (renderType == null) {
			return false;
		}

		// 26.1: textureTransform 封装于私有 RenderSetup，无公开等价，故反射读取。
		// 非单位矩阵即视为「动态」（纹理坐标随帧变化，如附魔光效、动画纹理），
		// 这类 RenderType 的顶点不能被跨帧缓存，否则贴图会错乱。
		return !getTextureMatrix(renderType).equals(UNIT_MATRIX);
	}

	/** 单位矩阵常量，用于 {@link #isDynamic} 比较（避免每次 new）。 */
	private static final org.joml.Matrix4f UNIT_MATRIX = new org.joml.Matrix4f();

	public static boolean hasDepth(RenderType renderType) {
		if (renderType == null) {
			return false;
		}

		return renderType.pipeline().getDepthStencilState() != null;
	}

	public static RenderType withDepth(RenderType renderType) {
		if (renderType == null) {
			return null;
		}

		if (hasDepth(renderType)) {
			return null;
		}

		// 26.1: RenderType 不可复制重建（内部 RenderSetup 私有）；深度不足时返回原 renderType
		// （由 GL 状态/调用方自行处理），避免反射重建的不确定性。
		return renderType;
	}

	public static LayerDrawType getDrawType(RenderType renderType) {
		return renderType.sortOnUpload() ? LayerDrawType.TRANSLUCENT : LayerDrawType.OPAQUE;
	}

	public static boolean isTranslucent(RenderType renderType) {
		return renderType.sortOnUpload();
	}

	/**
	 * 26.1: VertexFormat.Mode.asGLMode 已移除，自建 Mode → GL 图元常量映射。
	 *
	 * 映射必须与 MC 语义一致（对照 1.21.4 Mode 枚举的 asGLMode 字段，及 26.1 的
	 * GlConst.toGl(Mode)）：
	 * - QUADS → GL_TRIANGLES：MC 的 quad 以三角形绘制，每 4 顶点对应 6 个三角化索引
	 *   （0,1,2, 0,2,3），索引数见 Mode.indexCount(vertexCount) = vertexCount/4*6。
	 *   注意 Core Profile 下不存在 GL_QUADS，若按 GL_QUADS 提交会 GL_INVALID_ENUM。
	 * - LINES → GL_TRIANGLES：MC 的粗线段同样用三角形表达（asGLMode = 4）。
	 * - DEBUG_LINES → GL_LINES、DEBUG_LINE_STRIP → GL_LINE_STRIP 为真正的线图元。
	 */
	public static int toGLMode(Mode mode) {
		return switch (mode) {
			case POINTS ->			GL_POINTS;
			case LINES ->			GL_TRIANGLES;
			case DEBUG_LINES ->		GL_LINES;
			case DEBUG_LINE_STRIP ->GL_LINE_STRIP;
			case TRIANGLES ->		GL_TRIANGLES;
			case TRIANGLE_STRIP ->	GL_TRIANGLE_STRIP;
			case TRIANGLE_FAN ->	GL_TRIANGLE_FAN;
			case QUADS ->			GL_TRIANGLES;
			default ->				GL_TRIANGLES;
		};
	}
}
