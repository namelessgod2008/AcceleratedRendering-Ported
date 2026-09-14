# HANDOFF — 给下一个模型的提示词

> 本项目是 **AcceleratedRendering-Ported** 从 **Minecraft 1.21.4 Fabric** 到 **Minecraft 26.1 Fabric** 的移植。
> 上一轮工作已完成**实体加速功能的移植并验证通过**。本文档说明当前状态、已踩过的坑、以及接手时该怎么做。

---

## 一、当前状态（务必先看）

| 项目 | 状态 |
|---|---|
| `./gradlew build` | ✅ 通过 |
| 实体加速运行 | ✅ 视觉正常（模型/贴图/阴影/粒子），**用户已实测确认** |
| 性能 | ✅ **240-291 fps**（1100 只羊场景；原版约 150 fps） |
| NeoForge 配置界面 | ✅ 可用（Mod Menu → Accelerated Rendering → 配置） |
| vanilla 渲染修复 | ✅ 已加回并**用户目视验证通过**（`FeatureRenderDispatcherMixin`） |
| 阴影加速 | ✅ **已加回并用户目视验证通过**（`ShadowFeatureRendererMixin`） |
| layeringTransform（z-fighting 闪烁） | ✅ 已修复（`RenderTypeUtils.applyLayeringTransform`） |
| 其余功能（items/text/iris/geckolib/ftb 等） | ❌ 仍从编译排除（文件保留在磁盘），待逐项加回 |

> **⚠️ 本轮改动尚未提交 git**（HEAD 仍是 `93e25c4`）。vanilla 修复 + 阴影加速 + layering 修复
> 都还在工作区，建议先提交一次再继续开发，避免改坏无兜底。

**⚠️ 项目根目录下的 `HANDOFF.md` 是本文件；`.decompile/` 是 MC 26.1 的全量反编译源码（6882 个 .java），查询任何 MC 类实现都在这里，已加入 `.gitignore`。**

---

## 二、必读的参考文档（按优先级）

1. **`CLAUDE.md`**（`项目根/.claude/CLAUDE.md`）—— 编码行为准则，**必读并遵守**。特别注意项目区：
   > mixin操作优先使用mixinextras的，除非用户明确说明，否则禁止使用redirect

2. **本项目的记忆库**：`C:\Users\xzx\.claude\projects\D--Programs-MC-26-1-AcceleratedRendering-Ported\memory\`
   | 文件 | 内容 |
   |---|---|
   | `project-overview.md` | 项目概况、关键差异、文档索引 |
   | `entity-acceleration-migration.md` | **最重要** —— 实体加速的绘制通道实现、26.1 时序约束、性能修复、一次错误修改的教训 |
   | `build-system.md` | 构建系统、依赖版本、sourceSets 排除列表 |
   | `migration-26.1.md` | 移植全记录、26.1 API 重构要点、**剩余待办清单** |
   | `shadow-acceleration-26-1.md` | **新增** —— 阴影加速实现、Sodium 优先级冲突、layeringTransform 闪烁陷阱 |
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

**首选**：从 `memory/migration-26.1.md` 的「待办」章节挑一项推进。优先级建议：

1. ~~加回 vanilla 渲染修复~~ ✅ **已完成并验证**（2026-09-14）——
   用户目视确认盔甲/纹饰顺序正确。实现见 `FeatureRenderDispatcherMixin` 的「order 桶 → 加速层」映射。
   注意：1.21.4 的 `HumanoidArmorLayerMixin` / `LivingEntityRendererMixin` **文件仍在磁盘上但未注册**，
   它们在 26.1 是死代码（提交段 push layer 无任何作用），不要误以为改它们能生效。
2. ~~加回阴影加速~~ ✅ **已完成并验证**（2026-09-14）——
   26.1 把 `renderBlockShadow` 拆成「`extractShadowPiece` 判定」+「`ShadowFeatureRenderer.renderTranslucent`
   写顶点」两段，注入点必须落在**渲染段**（提交段拿不到 `VertexConsumer`）。
   新增 `ShadowFeatureRendererMixin`（`feature.entities.mixins.json`），
   **必须 `priority = 999`**：Sodium 0.9.1 也注入该方法且开头就无条件 `cancel`，同优先级下 Sodium 先执行、
   本 mod 的 mixin 永不触发。旧的 `EntityRenderDispatcherMixin` 已删除（目标方法不存在）。
   细节见 `memory/shadow-acceleration-26-1.md`。
2b. ✅ **layeringTransform 缺失（全局闪烁）已修复** —— 加速路径写 `DynamicTransforms` 时漏了
   RenderType 的 `layeringTransform`（1.21.4 靠 `renderType.setupRenderState()` 隐式完成）。
   影响 `entity_shadow`/`entity_cutout`/`entity_solid`/`armor_cutout_no_cull`/`banner_pattern` 等
   所有带 layering 的 RenderType。修复见 `RenderTypeUtils.applyLayeringTransform` +
   `BaseVertexDrawContextPool.prepareDraw`。**注意 `getModelViewMatrix()` 返回的是全局栈本身，不可直接改。**
3. **清理诊断代码**（用户当前要求**暂时保留**，动它之前请先与用户确认）——
   `core/AccelStats.java` 及各处埋点，会每秒打印 `[AR-FRAME]`。
4. **逐项加回其余功能**：从 `build.gradle` 的 `sourceSets` 排除列表中移除并适配。
5. 其它：`TextureUtils.downloadTexture`（26.1 需 `GpuDevice` command encoder）、
   INDIRECT 绘制路径、trinkets 配置项未翻译警告。

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

---

## 七、需要向用户确认的事项

- 用户是**中文交流**（文档、注释、会话均用中文）。
- 用户会亲自跑 `runClient` 做视觉验证，**你无法看到画面** —— 需要用户反馈现象。
- 用户对性能敏感（1100 只羊的基准场景），改动后建议确认帧率未回退。
- 具体完成度、验收标准由用户定义，**不要自行扩大范围**（如顺手重构无关代码）。
