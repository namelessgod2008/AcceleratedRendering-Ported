package com.namelessgod2008.core;

/**
 * 性能诊断计数器。
 *
 * 注意：mixin 类内不允许存在非 private 的静态字段（Mixin 会拒绝应用），
 * 故所有计数器集中放在此处，供 mixin 与方法体引用。
 */
public final class AccelStats {

	// ---- ModelPart 网格构建 ----
	/** compile() 被调用次数（所有实体所有部件） */
	public static long COMPILE_CALLS	= 0L;
	/** 实际走加速路径次数 */
	public static long ACCELERATED		= 0L;
	/** 一级缓存命中（按 buffer 键） */
	public static long MESH_HIT			= 0L;
	/** 二级缓存命中（按网格数据） */
	public static long MERGE_HIT		= 0L;
	/** 新建网格次数 */
	public static long BUILD			= 0L;

	// ---- 绘制阶段 ----
	/** 绘制帧数 */
	public static long FRAMES			= 0L;
	/** 加速绘制总耗时（纳秒） */
	public static long TOTAL_NANOS		= 0L;
	/** 绘制调用次数 */
	public static long DRAWS			= 0L;
	/** 顶点吞吐 */
	public static long VERTICES			= 0L;
	/** 准备阶段耗时（纳秒） */
	public static long PREPARE_NANOS	= 0L;
	/** pass 内绘制耗时（纳秒） */
	public static long DRAW_NANOS		= 0L;

	// ---- GPU 同步 ----
	/** ring buffer 等待 GPU 的次数 */
	public static long SYNC_WAITS		= 0L;
	/** ring buffer 等待 GPU 的耗时（纳秒） */
	public static long SYNC_NANOS		= 0L;

	// ---- prepareBuffers 分段计时（定位瓶颈）----
	/** glGetInteger(GL_CURRENT_PROGRAM) 耗时 —— 同步查询，可能阻塞等待 GPU */
	public static long GL_QUERY_NANOS	= 0L;
	/** mesh 上传 + 顶点变换 compute dispatch 耗时 */
	public static long DISPATCH_NANOS	= 0L;
	/** builder 循环（含剔除 compute dispatch、setupContext）耗时 */
	public static long BUILDER_NANOS	= 0L;
	/** glUseProgram 耗时 */
	public static long USE_PROGRAM_NANOS= 0L;
	/** prepareBuffers 调用次数 */
	public static long PREPARE_CALLS	= 0L;
	/** 处理的 builder 总数 */
	public static long BUILDER_COUNT	= 0L;
	/** mesh 上传 dispatcher 耗时 */
	public static long UPLOAD_NANOS		= 0L;
	/** 顶点变换 dispatcher 耗时 */
	public static long TRANSFORM_NANOS	= 0L;

	// ---- MeshUploadingProgramDispatcher 内部分段 ----
	public static long MU_BARRIER		= 0L;	// 起始 glMemoryBarrier
	public static long MU_COLLECT		= 0L;	// 收集 builders 的 meshUploaders
	public static long MU_CLASSIFY		= 0L;	// dense/sparse 分类循环
	public static long MU_PREPARE		= 0L;	// ringBuffer.prepare + bindTransformBuffers
	public static long MU_SPARSE		= 0L;	// sparse 上传 + transform dispatch
	public static long MU_DENSE			= 0L;	// dense 上传
	public static long MU_CLEAR			= 0L;	// buffers clear
	/** mesh 上传阶段的 glDispatchCompute 次数 */
	public static long MU_DISPATCHES	= 0L;
	/** 处理的 mesh uploader 总数 */
	public static long MU_UPLOADERS		= 0L;
	/** dense 循环的 CPU 部分（offsets/infoBuffer.reserve + uploader.upload） */
	public static long MD_CPU_NANOS		= 0L;
	/** dense 循环的提交部分（useProgram + bindBufferBase + glUniform + glDispatchCompute） */
	public static long MD_SUBMIT_NANOS	= 0L;
	/** dense 上传的实例总数（overrideCounts 累加） */
	public static long MD_INSTANCES		= 0L;
	/** dense 上传的 mesh 顶点数（meshSize 累加） */
	public static long MD_MESH_VERTS	= 0L;
	/** dense compute 的工作组总数 */
	public static long MD_WORKGROUPS	= 0L;

	private AccelStats() {
	}

	private static long lastReport = System.nanoTime();

	/** 每秒输出一次统计（由绘制阶段末尾调用）。 */
	public static void report() {
		long now = System.nanoTime();

		if (now - lastReport < 1_000_000_000L) {
			return;
		}

		System.out.println(
				"[AR-FRAME] frames/s=" + FRAMES
				+ " accelTotal=" + (TOTAL_NANOS / 1_000_000L) + "ms/s"
				+ " compile=" + COMPILE_CALLS
				+ " accelerated=" + ACCELERATED
				+ " meshHit=" + MESH_HIT
				+ " mergeHit=" + MERGE_HIT
				+ " build=" + BUILD
				+ " draws=" + DRAWS
				+ " verts=" + VERTICES
				+ " prepare=" + (PREPARE_NANOS / 1_000_000L) + "ms/s"
				+ " draw=" + (DRAW_NANOS / 1_000_000L) + "ms/s"
				+ " syncWaits=" + SYNC_WAITS
				+ " syncWait=" + (SYNC_NANOS / 1_000_000L) + "ms/s"
				+ " | glQuery=" + (GL_QUERY_NANOS / 1_000_000L) + "ms/s"
				+ " dispatch=" + (DISPATCH_NANOS / 1_000_000L) + "ms/s"
				+ " builder=" + (BUILDER_NANOS / 1_000_000L) + "ms/s"
				+ " useProgram=" + (USE_PROGRAM_NANOS / 1_000_000L) + "ms/s"
				+ " preps=" + PREPARE_CALLS
				+ " builders=" + BUILDER_COUNT
				+ " | upload=" + (UPLOAD_NANOS / 1_000_000L) + "ms/s"
				+ " transform=" + (TRANSFORM_NANOS / 1_000_000L) + "ms/s"
				+ " || muBarrier=" + (MU_BARRIER / 1_000_000L)
				+ " muCollect=" + (MU_COLLECT / 1_000_000L)
				+ " muClassify=" + (MU_CLASSIFY / 1_000_000L)
				+ " muPrepare=" + (MU_PREPARE / 1_000_000L)
				+ " muSparse=" + (MU_SPARSE / 1_000_000L)
				+ " muDense=" + (MU_DENSE / 1_000_000L)
				+ " muClear=" + (MU_CLEAR / 1_000_000L)
				+ " muDispatch=" + MU_DISPATCHES
				+ " muUploaders=" + MU_UPLOADERS
				+ " mdCpu=" + (MD_CPU_NANOS / 1_000_000L)
				+ " mdSubmit=" + (MD_SUBMIT_NANOS / 1_000_000L)
				+ " mdInst=" + MD_INSTANCES
				+ " mdMeshVerts=" + MD_MESH_VERTS
				+ " mdWg=" + MD_WORKGROUPS
		);

		FRAMES			= 0L;
		TOTAL_NANOS		= 0L;
		COMPILE_CALLS	= 0L;
		ACCELERATED		= 0L;
		MESH_HIT		= 0L;
		MERGE_HIT		= 0L;
		BUILD			= 0L;
		DRAWS			= 0L;
		VERTICES		= 0L;
		PREPARE_NANOS	= 0L;
		DRAW_NANOS		= 0L;
		SYNC_WAITS		= 0L;
		SYNC_NANOS		= 0L;
		GL_QUERY_NANOS	= 0L;
		DISPATCH_NANOS	= 0L;
		BUILDER_NANOS	= 0L;
		USE_PROGRAM_NANOS = 0L;
		PREPARE_CALLS	= 0L;
		BUILDER_COUNT	= 0L;
		UPLOAD_NANOS	= 0L;
		TRANSFORM_NANOS	= 0L;
		MU_BARRIER		= 0L;
		MU_COLLECT		= 0L;
		MU_CLASSIFY		= 0L;
		MU_PREPARE		= 0L;
		MU_SPARSE		= 0L;
		MU_DENSE		= 0L;
		MU_CLEAR		= 0L;
		MU_DISPATCHES	= 0L;
		MU_UPLOADERS	= 0L;
		MD_CPU_NANOS	= 0L;
		MD_SUBMIT_NANOS	= 0L;
		MD_INSTANCES	= 0L;
		MD_MESH_VERTS	= 0L;
		MD_WORKGROUPS	= 0L;
		lastReport		= now;
	}
}
