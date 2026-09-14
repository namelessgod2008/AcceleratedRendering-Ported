package com.namelessgod2008.core.utils;

import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public class MemUtils {

	public static void putNormal(long address, float value) {
		MemoryUtil.memPutByte(address, (byte) ((int) (Mth.clamp(value, -1.0f, 1.0f) * 127.0f) & 0xFF));
	}

	/**
	 * 写入 3×3 法线矩阵（48 字节：3 列 × vec4，末位补 0，与 std430 的 mat3 布局一致）。
	 *
	 * <p><b>⚠️ 此处保持逐字段写入，不要图省事改用 {@link Matrix3f#getToAddress(long)}。</b>
	 * JOML 的 {@code Matrix3f.getToAddress} 写的是紧凑的 36 字节（无列填充），
	 * 与 shader 期望的 std430 布局不符，替换会导致法线错乱。
	 *
	 * <p>性能上它同样是 {@code beginTransform} 热路径（每帧约 39000 次 × 9 次 memPutFloat），
	 * 但 JOML 无合适 API，且法线矩阵的实际影响小于 mat4（后者已用 getToAddress 优化）。
	 * 若日后此处成为瓶颈，应改为写入预置的 float[] 再整体 {@code MemoryUtil.memPut}，
	 * 而非换 getToAddress。
	 */
	public static void putMatrix3f(long address, Matrix3f matrix) {
		// 每列 3 个 float（第 4 个位置留作 std430 填充，不写）：
		// 用 1 次 memPutLong 写前两个 float + 1 次 memPutFloat 写第三个，9 次调用降为 6 次。
		// 与 putMatrix4f 同因：FFM 后端下 memPut* 在 JIT 内联前极慢（实测 18ms/帧），
		// 减少调用次数能显著缩短收敛时间（第 2 帧即满速）。
		MemoryUtil.memPutLong(address + 0L * 16L,	packFloats(matrix.m00(), matrix.m01()));
		MemoryUtil.memPutFloat(address + 0L * 16L + 8L,	matrix.m02());

		MemoryUtil.memPutLong(address + 1L * 16L,	packFloats(matrix.m10(), matrix.m11()));
		MemoryUtil.memPutFloat(address + 1L * 16L + 8L,	matrix.m12());

		MemoryUtil.memPutLong(address + 2L * 16L,	packFloats(matrix.m20(), matrix.m21()));
		MemoryUtil.memPutFloat(address + 2L * 16L + 8L,	matrix.m22());
	}

	/** 把两个 float 打包为一个 long（低 32 位在前），用于减少 native 调用次数。 */
	private static long packFloats(float low, float high) {
		return		((long) Float.floatToRawIntBits(high) << 32)
				|	(		 Float.floatToRawIntBits(low)  & 0xFFFFFFFFL);
	}

	/**
	 * 写入 4×4 矩阵（64 字节，列主序，与 std430 的 mat4 布局一致）。
	 *
	 * <p><b>⚠️ 性能关键：不要改回逐字段的 {@code MemoryUtil.memPutFloat}。</b>
	 *
	 * <p>JDK 25 下 LWJGL 3.4.1 会自动启用 FFM 后端（{@code META-INF/versions/25/} 覆盖
	 * 根目录实现），其 {@code memPutFloat} 内部是 {@code VarHandle.set}，**在 JIT 完成内联前**
	 * 会退化为 {@code LambdaForm} + {@code Array.newArray} 的反射级调用（实测每次约 1500ns，
	 * 是 Unsafe 路径的上千倍），且需累计数百万次调用才被优化。
	 *
	 * <p>本方法位于 {@code beginTransform} 的热路径上：1100 只羊场景下每帧约 39000 次调用
	 * × 16 次 memPutFloat = 62 万次 NDK 调用/帧。改用 {@link Matrix4f#getToAddress(long)}
	 * （内部走 JOML 自带的 Unsafe 实现，单次 64 字节拷贝）后，实测冷启动第 2 帧即恢复满速
	 * （0.4ms/帧 vs 逐字段的 7ms/帧且长期不收敛）。
	 */
	public static void putMatrix4f(long address, Matrix4f matrix) {
		matrix.getToAddress(address);
	}
}
