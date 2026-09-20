# HANDOFF — 给下一个模型的提示词

> 本项目是 **AcceleratedRendering-Ported** 从 **Minecraft 1.21.4 Fabric** 到 **Minecraft 26.1 Fabric** 的移植。
> 上一轮工作已完成**实体加速功能的移植并验证通过**。本文档说明当前状态、已踩过的坑、以及接手时该怎么做。

---

## 一、当前状态（务必先看）

> **HEAD = `01c564c`**（"totally fix entity disappearing issue"），工作区干净、与 HEAD 零差异。

| 项目 | 状态 |
|---|---|
| `./gradlew build` / `compileJava` | ✅ 通过 |
| 实体加速（无光影） | ✅ 视觉正常、**240-291 fps**（1100 只羊场景，原版约 150 fps） |
| 实体加速（**开光影**） | ✅ **不透明实体正常**（羊/箱子/守卫者/铁傀儡/熊猫），阴影正常 —— 2026-09-20 修复 |
| 半透明实体（开光影） | ⚠️ **回退原版管线**（`force_translucent_acceleration = DISABLED`），加速路径有未突破的架构障碍，见下 |
| **实体堆叠闪烁** | ❌ **未解决** —— 多只羊堆叠时遮挡关系持续闪烁，**无光影也复现**，`01c564c` 即存在 |
| 阴影加速 | ✅ 已加回并验证（`ShadowFeatureRendererMixin`，priority=999 压制 Sodium） |
| layeringTransform（z-fighting） | ✅ 已修复（`RenderTypeUtils.applyLayeringTransform`） |
| 其余功能（items GIU/text/geckolib/ftb 等） | ❌ 仍从编译排除（文件保留在磁盘），待逐项加回 |

### 当前两个已知问题

**1. 实体堆叠闪烁（未解决，优先排查）**
- 症状：多只羊堆叠时遮挡关系持续闪烁；**关闭光影同样复现** → 与 Iris 无关
- `force_translucent_acceleration` 开/关都闪
- 已排除：Iris gbuffer 重定向、本次会话的全部 Iris 改动（回退后仍闪）
- **首要下一步**：关闭实体加速（走 vanilla）是否还闪 —— 这是区分「加速路径引入」与
  「26.1 本身/其它 mod」的关键对照实验
- 详见 `memory/entity-stacking-flicker.md`

**2. 半透明实体在光影下无法加速（架构障碍，已回退）**
- 根因链（全部有实测证据）：
  1. Photon 的半透明实体程序是 `DRAWBUFFERS:01` → **需 2 个 color attachment（MRT）**
  2. 而 26.1 的 `createRenderPass` **只支持单 color view**
     （`DirectStateAccess.bindFrameBufferTextures` 硬编码 `COLOR_ATTACHMENT0` + `DEPTH_ATTACHMENT`）
  3. 单附件 FBO 的 `DRAW_BUFFER1` 默认 `GL_NONE` → `location=1` 输出被静默丢弃
  4. 本该由 Iris 的 `ExtendedShader.iris$setupState` 换成 MRT FBO，但它没执行 ——
     实测 `programCls=GlProgram`（vanilla）、`isIrisProgram=false`、`overrideShaders=false`
  5. `overrideShaders = isRenderingWorld && isMainBound`，而 `isMainBound` 只在
     「绑主渲染目标」时为 true —— 我们的自建 FBO 永远拿不到（**循环依赖**）
- **当前策略**：默认 `DISABLED`，半透明回退原版（已验证正常）
- 详见 `memory/iris-shader-entity-invisible.md` 的「2026-09-20 补充」章节

> **⚠️ 本轮（2026-09-20）尝试过但已回退的方案**：按 OPAQUE/TRANSLUCENT 拆两个绘制时机
> + `GlCommandEncoder.trySetup` RETURN 重定向 + 强制 `setIsMainBound`。
> **不透明实体确实修好了**（用户确认「箱子和羊全部正常显示」），但引入了/未消除堆叠闪烁，
> 故整体回退。**将来重做时可直接参考上述根因分析**，不必重新排查。

**⚠️ 项目根目录下的 `HANDOFF.md` 是本文件；`.decompile/` 是 MC 26.1 的全量反编译源码，已加入 `.gitignore`。
另有两份反编译产物供静态分析（不在仓库内）：**
- `/d/Programs/MC/26.1/_analysis/iris_rt` —— Iris 1.11.4（运行时实际版本）
- `/d/Programs/MC/26.1/_analysis/mc` —— MC 26.1 未混淆

---

## 二、必读的参考文档（按优先级）

1. **`CLAUDE.md`**（`项目根/.claude/CLAUDE.md`）—— 编码行为准则，**必读并遵守**。特别注意项目区：
   > mixin操作优先使用mixinextras的，除非用户明确说明，否则禁止使用redirect

2. **本项目的记忆库**：`C:\Users\xzx\.claude\projects\D--Programs-MC-26-1-AcceleratedRendering-Ported\memory\`
   | 文件 | 内容 |
   |---|---|
   | `entity-acceleration-migration.md` | **最重要** —— 实体加速的绘制通道实现、26.1 时序约束、性能修复、一次错误修改的教训 |
   | `iris-shader-entity-invisible.md` | **2026-09-20 核心** —— 光影下实体消失的完整根因链 + 半透明加速的 MRT 架构障碍 |
   | `entity-stacking-flicker.md` | **2026-09-20 未解决** —— 实体堆叠闪烁（无光影也复现） |
   | `iris-outerwrapped-rendertype-unwrap.md` | Iris 包装 RenderType 必须在 `getBuffer` 入口解包，否则方块实体消失 |
   | `iris-vertexformat-padding-not-needed.md` | Iris 1.11.4 自带顶点对齐，旧 padding mixin 会导致 58 字节崩溃 |
   | `perf-10fps-investigation.md` | JDK 25 + LWJGL FFM 后端的反射级开销（10fps 卡顿结案） |
   | `indirect-draw-26-1.md` | INDIRECT 绘制：RenderPass 无 indirect 入口，用 mixin + 静态桥接 |
   | `item-acceleration-26-1.md` | 物品加速：BakedQuad 是自包含 record，GUI 部分未加回 |
   | `shadow-acceleration-26-1.md` | 阴影加速、Sodium 优先级冲突、layeringTransform 闪烁陷阱 |
   | `build-system.md` | 构建系统、依赖版本、sourceSets 排除列表 |
   | `migration-26.1.md` | 移植全记录、26.1 API 重构要点、剩余待办清单 |
   | `log.md` | 日志位置、`[AR-FRAME]`/`[AR-SLOW]` 格式说明与正常/异常样本 |

3. **1.21.4 原项目的记忆库**（架构参考，大量专题文件）：
   `C:\Users\xzx\.claude\projects\D--Programs-MC-1-21-4-AcceleratedRendering-Ported\memory\`
   以及其项目根 `CLAUDE.md`（45KB，含完整架构与 Mixin 签名参考）。

4. **1.21.4 原项目源码**（对照基准）：
   `D:\Programs\MC\1.21.4\AcceleratedRendering-Ported\src\main\java`
   **移植时以它为蓝本** —— 未被 26.1 API 迫使改动的地方，应尽量与它逐字一致。

---

## 三、26.1 与 1.21.4 的关键差异（不了解这些会反复踩坑）

### 构建层
- **26.1 官方取消了反混淆**：下载的 jar 未经混淆，直接是官方命名（`net/minecraft/...`）。
  因此 **没有 mappings 块**（`officialMojangMappings()` 会报错），**不需要 parchment**。
- **用标准 `net.fabricmc.fabric-loom`**（非 1.21.4 的 `fabric-loom-remap`）→
  `modImplementation`/`modCompileOnly` 不存在，用 **`implementation`/`compileOnly`** 替代。
- **access widener 头必须是 `v2 official`**（不是 `v2 named`）。

### 渲染层（本次移植最难的部分）
- **`ShaderInstance` 移除** → 只能走 `RenderPipeline` + `RenderPass`（详见记忆 `entity-acceleration-migration.md`）
- **绘制必须在 `frame.execute()` 期间**（注册 FramePass），不能在其之后 —— 否则纹理懒加载上传抛异常
- **render pass 打开期间不得编码其它命令** → 纹理解析、uniform 写入必须在开 pass 前完成
- **`readsAndWrites` 会把句柄内容移入新句柄**，必须写回字段
- **`bindTexture` 在 sampler 为 null 时会静默跳过绑定** → 需兜底 sampler
- **实体渲染是「提交-渲染两段式」**：`submit()` 只记录，顶点写入延后到
  `FeatureRenderDispatcher` 阶段 —— 所以 1.21.4 那种「在渲染调用处 push defaultLayer」的
  mixin 在 26.1 **全部失效**；正确做法是映射原版 `order(int)` 桶（详见
  `memory/migration-26.1.md` 的「提交-渲染两段式」与 `entity-acceleration-migration.md`）
- 其它：`ResourceLocation`→`Identifier`、`GuiGraphics`→`GuiGraphicsExtractor`、
  `RenderType`→`rendertype` 包、`BakedModel` 体系移除、`ItemRenderer`/`LightTexture`/`BufferUploader` 移除 等

---

## 四、工作方式要求

1. **不确定就问，不要臆测。** 用户明确要求过这一点。有歧义时列出选项让用户拍板。
2. **改动前先查 `.decompile/`** 确认 26.1 的实际类/方法签名，不要凭 1.21.4 或记忆推断。
3. **改动数据布局前，先确认消费者（尤其是 shader）的读取方式。**
   上一轮有一次错误修改就是因此导致贴图错乱（详见记忆中的教训段落）。
4. **移植优先与 1.21.4 保持一致**：未被 26.1 API 迫使改动的地方，逐字照搬原实现。
5. **大改动前先备份/说明**，改完立即 `./gradlew build` 验证。

---

## 五、下一步该做什么

按优先级：

1. **【最高】排查实体堆叠闪烁**（未解决，见第一节）
   首个对照实验：**关闭实体加速，看是否还闪**。
   - 若不闪 → 问题在加速路径（深度精度 / 绘制顺序 / 深度写入状态）
   - 若仍闪 → 问题在 26.1 本身或其它 mod（Sodium / Iris / 光影包）
   细节与已排除项见 `memory/entity-stacking-flicker.md`。

2. **【高】半透明实体在光影下的加速**（架构障碍，见第一节）
   三条候选路径（均未实施）：
   1. 复用 Iris 的 `GlFramebuffer` 实例（读 `ExtendedShader` 的 private 字段，需 mixin 暴露）
   2. 自建 MRT FBO（`IrisRenderSystem.framebufferTexture2D` + `drawBuffers`）—— 复制 Iris 逻辑，版本升级易碎
   3. 按 RenderType 分组，让每个程序各开一个 RenderPass（绕开 `isSetup` 守卫）
   **注意**：重做时第一天做过的「OPAQUE/TRANSLUCENT 拆分 + `trySetup` 重定向」确实修好了
   不透明实体，可直接参考 —— 但需先解决/排除它与堆叠闪烁的关系。

3. 逐项加回其余功能：从 `build.gradle` 的 `sourceSets` 排除列表中移除并适配
   （items GUI 部分、text、geckolib、ftb、modernui、tlm、create、emf 等）。

4. 清理诊断代码（用户当前要求**暂时保留**，动它之前请先与用户确认）——
   `core/AccelStats.java` 及各处埋点，会每秒打印 `[AR-FRAME]`。

5. 其它：`TextureUtils.downloadTexture`（26.1 需 `GpuDevice` command encoder）、
   trinkets 配置项未翻译警告。

---

## 六、验证与调试

```bash
cd D:/Programs/MC/26.1/AcceleratedRendering-Ported

# 构建
./gradlew build

# 运行客户端（需手动进世界观察）
./gradlew runClient

# 查看诊断日志（本 mod 的 [AR-*] 输出）
grep "AR-FRAME\|AR-SLOW" run/logs/latest.log | tail -30

# 查看异常/崩溃
grep -iE "Exception|ERROR|Mixin apply for" run/logs/latest.log | tail -30
```

**诊断日志含义与正常/异常样本**见 `memory/log.md`。
简言之：`[AR-SLOW]` 若只在**进世界头几帧**出现属正常（预热）；若**持续出现**则需排查
（先看 `cpu` vs `submit` 的分布）。

### RenderDoc 抓帧分析（2026-09-20 建立）

**Loom 自带集成**（推荐）：

```bash
./gradlew runClientRenderDoc     # 自动下载并注入 renderdoccmd，进世界按 F12 抓帧
./gradlew startRenderDocUI       # 单独开 RenderDoc 界面
```

⚠️ **不要**改写成 `renderdoccmd capture ... gradlew runClient` —— Gradle daemon 是常驻进程，
被 hook 的会是 Gradle 而非游戏。Loom 的做法是直接以 `javaLauncher + allJvmArgs` 拉起游戏 JVM。

**命令行分析抓帧**（无 GUI，`tools/rd_auto.py`）：

```bash
RD_CAPTURE=<x.rdc> qrenderdoc.exe --python tools/rd_auto.py
# 全量扫描（慢，慎用）：RD_SCAN_ALL=1
```

本机 RenderDoc（`D:\ProgramApps\RenderDoc_1.46_64`）的 Python API 与新版不同：
无 `GetReplayManager` / `GetCaptureFilePath` / `CurEvent`；用 `OpenCaptureFile` + `OpenCapture`，
`GetPipelineState()` 返回 `PipeState`。脚本末尾需 `sys.exit(0)` 才能退出。

⚠️ **遍历全部 draw 逐个 `SetFrameEvent` 会假死**（每次都要重放整帧），必须采样或限定 eventId。

**排查心得**：`glMultiDrawElementsBaseVertex` 是 Sodium 的地形渲染；
本 mod 用 `pass.drawIndexed`（非 Multi）。用「顶点属性名」区分程序最可靠
（`iris_Entity`/`at_tangent` = Iris ENTITY 格式；`mc_Entity`/`at_midBlock` = TERRAIN）。


---

## 七、需要向用户确认的事项

- 用户是**中文交流**（文档、注释、会话均用中文）。
- 用户会亲自跑 `runClient` 做视觉验证，**你无法看到画面** —— 需要用户反馈现象。
- 用户对性能敏感（1100 只羊的基准场景），改动后建议确认帧率未回退。
- 具体完成度、验收标准由用户定义，**不要自行扩大范围**（如顺手重构无关代码）。
