package com.namelessgod2008.compat.iris;

import net.minecraft.client.renderer.rendertype.RenderType;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Iris 包装 RenderType 的解包工具。
 *
 * <p><b>为什么需要解包</b>：Iris 用 {@code net.irisshaders.iris.layer.OuterWrappedRenderType}
 * 包装部分 RenderType。该类 {@code extends RenderType}，但内部用的是 **{@code FAKE_SETUP}**
 * （伪造的 RenderSetup），真实的 setup 在私有字段 {@code wrapped} 指向的对象里。
 *
 * <p>{@code RenderTypeUtils} 靠反射读取 {@code RenderType.state} 来解析纹理/矩阵 ——
 * 不解包就会读到 {@code FAKE_SETUP}，得到空结果。后果不只是贴图错误，而是**整个绘制被跳过**：
 * {@code BaseVertexDrawContextPool} 在 {@code preparedTextures} 为空时直接 return
 * （否则 {@code RenderPass.bindTexture} 会抛 "Missing sampler"）。
 *
 * <p><b>实际故障（2026-09-14）</b>：箱子/末影箱模型完全消失，碰撞箱仍在。探针实测：
 * <pre>
 * rtClass=net.irisshaders.iris.layer.OuterWrappedRenderType isIris=true texturesEmpty=true
 * → collectorVerts=0 meshCls=EmptyMesh      （网格为空）
 * → 跳过绘制（纹理为空）count=324            （324 个索引全部没画）
 * </pre>
 * 未被包装的类型（如羊的 {@code entity_cutout}）不受影响 —— 这解释了「只有部分方块实体消失」。
 *
 * <p><b>为什么用反射而不是 accessor / access widener</b>：
 * <ul>
 *   <li>Iris 的 {@code unwrap()} 是 private，且没有公开的解包接口
 *       （旧版曾有 {@code WrappableRenderType}，在当前 Iris 1.11.4 中已随
 *       {@code batchedentityrendering} 包一起移除）；</li>
 *   <li>access widener 只能作用于 Loom 处理的类（Minecraft jar），
 *       对外部 mod 的类会报 {@code validateAccessWidener: Could not find class}
 *       （已于 2026-09-14 实测确认）；</li>
 *   <li>accessor mixin 需要额外注册，且目标类缺失时的失败方式更隐蔽。</li>
 * </ul>
 * 故用反射 + 惰性初始化，未装 Iris 时全程零开销。
 */
public final class IrisRenderTypeUnwrapper {

	private static final String WRAPPER_CLASS = "net.irisshaders.iris.layer.OuterWrappedRenderType";

	/** null = 尚未初始化；初始化为 null 字段表示「无 Iris 或不支持」 */
	private static boolean	initialized	= false;
	private static Field	wrappedField;

	// 包装检测用的身份集合。用 IdentityHashMap：RenderType 的 equals 可能被覆写，
	// 而这里要的是「是不是同一个对象实例」，必须是引用比较。
	private static final Map<RenderType, RenderType> UNWRAPPED = new IdentityHashMap<>();

	private IrisRenderTypeUnwrapper() {
	}

	/**
	 * 若 {@code renderType} 是 Iris 的包装类型，返回其内部真实的 RenderType；
	 * 否则原样返回。
	 */
	public static RenderType unwrap(RenderType renderType) {
		if (renderType == null) {
			return null;
		}

		var cached = UNWRAPPED.get(renderType);

		if (cached != null) {
			return cached;
		}

		RenderType result = renderType;

		if (!initialized) {
			init();
		}

		if (wrappedField != null && WRAPPER_CLASS.equals(renderType.getClass().getName())) {
			try {
				result = (RenderType) wrappedField.get(renderType);
			} catch (ReflectiveOperationException e) {
				result = renderType;
			}
		}

		UNWRAPPED.put(renderType, result);

		return result;
	}

	/**
	 * 判断是否为 Iris 的包装类型。
	 *
	 * <p>与 {@link #unwrap} 分开是因为二者用途不同：{@code unwrap} 要拿内部真实类型，
	 * 而本方法只回答「送进来的这个东西是不是 FAKE_SETUP 的持有者」。
	 * 需要后者的场景：{@code RenderType.outputTarget()} ——
	 * {@code OuterWrappedRenderType} 覆写了 {@code pipeline()} 却没有覆写
	 * {@code outputTarget()}，于是它返回的是 {@code FAKE_SETUP}（用
	 * {@code RenderPipelines.GUI_TEXTURED} 建的假 setup）的目标而非真实渲染目标。
	 * 若把它当真实答案用，RenderPass 会建在错误的目标上，实体整体不可见。
	 *
	 * <p><b>实测故障（2026-09-14）</b>：原版无光影一切正常，开 Photon main 后羊全部消失、
	 * 史莱姆只剩无眼模型、箱子与末影箱完全消失 —— 因为只有开光影时才走
	 * {@code IrisBufferEnvironment.irisSubSet} 分支，包装类型只在该分支生效。
	 */
	public static boolean isWrapped(RenderType renderType) {
		if (renderType == null) {
			return false;
		}

		return WRAPPER_CLASS.equals(renderType.getClass().getName());
	}

	private static synchronized void init() {
		if (initialized) {
			return;
		}

		initialized = true;

		try {
			Class<?> wrapperClass = Class.forName(WRAPPER_CLASS);

			wrappedField = wrapperClass.getDeclaredField("wrapped");
			wrappedField.setAccessible(true);
		} catch (ClassNotFoundException | NoSuchFieldException e) {
			// 未装 Iris，或 Iris 改了内部结构 —— 此时不做解包（原样返回）
			wrappedField = null;
		}
	}
}
