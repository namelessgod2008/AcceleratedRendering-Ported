# AcceleratedRendering-Ported — Project Architecture & Migration Notes

> ⚠️ **重要：在用户经过游戏测试并确认功能正常前，禁止修改 CLAUDE.md、memory/*.md 和 TODO.md。只允许修改源代码（src/）。**（本文档最后一次更新：2026-07-18，经用户明确指示）

## 🚀 新对话快速入门

你正在协助一个 Minecraft Fabric 1.21.4 模组项目 — **AcceleratedRendering-Ported**（AcceleratedRendering-reFabricated 的本地移植副本），GPU 计算着色器实体渲染加速模组（从 NeoForge 移植至 Fabric）。

**⚠️ 2026-07-18 本副本关键差异**（相对 reFabricated 原文档）：
- 项目路径：`D:\Programs\MC\1.21.4\AcceleratedRendering-Ported`（非 git 仓库）
- Java 包名已全量重命名：`com.github.argon4w.acceleratedrendering` → **`com.namelessgod2008`**（374 文件；含 mixin JSON `package`/`plugin` 字段、fabric.mod.json 入口点、Iris compat mixin 的 `Lcom/...` 描述符）
- MOD_ID 仍为 `acceleratedrendering`，资源命名空间必须是 `assets/acceleratedrendering`（见 Pitfall #4g）
- refMap 机制已废弃 — `fabric-loom-remap` 静态重映射注解（见 Pitfall #4e）
- ⚠️ 已有 5 处文本加速渲染 bug 已修（见 Pitfall #4h、#8、#9；`StringRenderOutputMixin` 11 参构造注入）；告示牌文字现在正常显示

### 第一次读这些文件
| 文件 | 位置 | 用途 |
|------|------|------|
| **CLAUDE.md** | 项目根目录（就是本文件） | 完整架构、关键易错点、Mixin 签名参考 |
| **MEMORY.md** | `C:\Users\xzx\.claude\projects\D--Programs-MC-1-21-4-AcceleratedRendering-Ported\MEMORY.md` | 持久化知识索引（指向 `memory/` 目录下的 19 个专题文件） |
| **TODO.md** | 项目根目录 | 功能状态清单（✅ 已实现 / ⚠️ 部分实现 / ❌ 未实现） |

### 如何反编译外部 JAR
**Minecraft 源码**：
```
# 找到 jar 路径并提取/反编译某个类（hash 随映射配置变化，用 find 动态定位）
JAR=$(find "D:/Programs/MC/1.21.4/AcceleratedRendering-Ported/.gradle/loom-cache/minecraftMaven" -name "minecraft-merged-*.jar" | head -1)

cd "$(dirname "$JAR")"
jar xf "$JAR" net/minecraft/client/renderer/LevelRenderer.class   # 提取
javap -p -c net/minecraft/client/renderer/LevelRenderer.class      # 反编译字节码
javap -p -c -l net/minecraft/client/renderer/LevelRenderer.class   # 含局部变量表
```

**外部 Mod JAR**（示例：Iris、ModernUI、Sodium 等）：
```
MODDIR="D:/Programs/MC/1.21.4/AcceleratedRendering-Ported/run/mods"
cd "$MODDIR"
jar xf "$(find . -name '*Iris*' -print -quit)" net/irisshaders/iris/mixin/fabric/MixinLevelRenderer.class
javap -p -c net/irisshaders/iris/mixin/fabric/MixinLevelRenderer.class
```

### 关键映射信息
- **Mojang Official Mappings** + Parchment（不是 Yarn！）
- **Mixin 约定**：MC 类目标（public **和** private）一律默认 `remap=true`；只有自己的类 / `@Pseudo` mod 类用 `remap=false`
- **⚠️ 2026-07 更新**：`remap=false` 仅适用于非 MC 类（自己的类/Mod 类）。对于 MC 类（含 private 方法），生产环境中的 intermediary jar 会重命名**所有**方法 → 必须用 `remap=true`（由 remapJar 静态重映射注解翻译，见下条）。
- **TAB 缩进**：多数 `.java` 文件使用 TAB。避免 Windows `sed` 中使用 `\n` 或 `\t`；使用 Edit 工具或 Python 精确替换。
- **构建**：`./gradlew build`（需要 JDK 21+；gradle.properties 指定 GraalVM JDK 24），运行 `./gradlew runClient`
- **⚠️ RefMap 已废弃（2026-07-18 实证）**：本副本使用 `fabric-loom-remap` 1.17-SNAPSHOT 插件，`useLegacyMixinAp` 已移除。remapJar 时 mixin **注解字符串被静态重写**为 intermediary 名（已验证产物 jar：`@Inject(method="method_22702")`、`@At target` 含 `method_23182`）。refMap 文件不再生成也不再需要；18 个 mixin JSON 中的 `"refmap": "acceleratedrendering-refmap.json"` 为无害残留（生产环境只产生找不到 refmap 的警告）。详见 Pitfall #4e。

### 验证注入点的标准流程
1. 用 `javap -p -c` 反编译目标 Minecraft/mod 类
2. 找到目标方法，确认字节码中的精确 INVOKE 调用及其偏移量/序号
3. 对照当前 mixin 中的 `@At` 目标进行验证
4. 用 `javap -p -c -l` 获取局部变量表（确认 `@Local` 名称/索引）

---

## Project Overview
- **Local project**: `AcceleratedRendering-Ported` @ `D:\Programs\MC\1.21.4\AcceleratedRendering-Ported`（非 git 仓库；`rootProject.name = acceleratedrendering-ported`）
- **Java package**: `com.namelessgod2008`（2026-07-18 从 `com.github.argon4w.acceleratedrendering` 全量重命名）
- **Based on**: `ZhuRuoLing/AcceleratedRendering-reFabricated` (Fabric port)
- **Upstream**: `Argon4W/AcceleratedRendering` (NeoForge, original), `Luna5ama/AcceleratedRendering` (NeoForge 1.21.4-port)
- **Target**: Minecraft Fabric 1.21.4, client-side only
- **Purpose**: GPU compute-shader based entity model part rendering acceleration (vertex transform + mesh caching)
- **Java**: 21, **Loom**: `net.fabricmc.fabric-loom-remap` 1.17-SNAPSHOT, **Loader**: 0.19.3
- **Mappings**: Mojang official mappings + Parchment 1.21.4:2025.03.23 (NOT Yarn)

## Build System
- **Build tool**: Gradle + `fabric-loom-remap` 1.17-SNAPSHOT（remapJar 静态重映射 mixin 注解，无 refMap）
- **Key deps**: Fabric API 0.119.4+1.21.4, ForgeConfigAPIPort 21.4.3, ModMenu 13.0.3, NeoForge Event Bus 8.0.5 (`include implementation`), Lombok 1.18.40, mixinconstraints 1.0.9
- **modCompileOnly**: Sodium mc1.21.4-0.6.13, Iris 1.8.8+1.21.4, ModernUI 3.12.0.3, GeckoLib 4.8, EMF 3.2.4, ImmediatelyFast 1.3.4, Trinkets-canary 3.10.0-1.21.4, TouhouLittleMaid-orihime 0.8.2, FTB Library (curse 7312255), Axiom, MaLiLib, TweakerMore
- **Access widener**: `src/main/resources/acceleratedrendering.accesswidener` (v2 named)
- **Mixin JSONs**: 18 个存在于 resources；**10 个**注册于 `fabric.mod.json` → `mixins` 数组（core、entities、items、modelparts、text、filter、compat.vanilla、compat.iris、compat.immediatelyfast、feature.modernui）；其余 8 个未注册（create、entitymodelfeature、ftb、geckolib、touhoulittlemaid、sophisticated、trinkets、tweakmore）
- **processResources**: `filesMatching("fabric.mod.json") { expand replaceProperties }` — fabric.mod.json 用 `${mod_id}` 等占位符，必须展开完整映射（只 expand version 会报 `Missing property (mod_id)`）
- **JDK**: Compile with JDK 21+ (`options.release = 21`); gradle.properties `org.gradle.java.home` 指向 GraalVM JDK 24.0.2。运行用 JDK 21/24（JDK 25 removes `LambdaMetafactory`）

## Architecture

### Core Pipeline
```
ModelPart.compile() → [ModelPartMixin intercept] → AcceleratedBufferBuilder
    → mesh data built & cached → compute shader transform → GPU buffer
    → LevelRendererMixin draws buffers at endLastBatch/endOutlineBatch

LivingEntityRenderer.render() → BufferSourceMixin wraps BufferBuilder
    → BufferBuilder implements IAcceleratedVertexConsumer
    → AcceleratedBufferBuilder.doRender() → renderer.render() callback
```

### Key Classes
| Class | Role |
|-------|------|
| `AcceleratedBufferBuilder` | Core accelerated vertex consumer, owns `beginTransform`/`endTransform`/`doRender` |
| `IAcceleratedVertexConsumer` | Interface with default-throwing methods (must be overridden) |
| `VertexConsumerExtension` | Static utility: `getAccelerated(vc)` — direct cast to IAcceleratedVertexConsumer |
| `CoreFeature` | Global state: `isRenderingLevel()`, `setRenderingLevel()`, `createMeshCollector()` |
| `CoreBuffers` | Static buffer instances (ENTITY, BLOCK, POS, POS_TEX_COLOR, etc.) |

### Wrapper Chain (VertexConsumer wrappers that implement IAcceleratedVertexConsumer)
```
EntityOutlineGenerator → SheetedDecalTextureGenerator → SpriteCoordinateExpander → BufferBuilder (AcceleratedBufferBuilder)
```
Each wrapper delegates `isAccelerated()`, `doRender()`, `beginTransform()`, `endTransform()` to its inner `delegate`.

### Mixin Organization
| JSON | Purpose | Status |
|------|---------|--------|
| `core.mixins.json` | Buffer pipeline, LevelRenderer, GameRenderer | Active |
| `feature.entities.mixins.json` | Entity shadow rendering | Active |
| `feature.modelparts.mixins.json` | ModelPart compile acceleration | Active |
| `feature.text.mixins.json` | BakedGlyph, Font text acceleration | Active (StringRenderOutput ✅ re-enabled) |
| `feature.items.mixins.json` | Item/block rendering acceleration | Active (limited) |
| `compat.iris.mixins.json` | Iris shader compat | @Pseudo, optional |
| `compat.immediatelyfast.mixins.json` | ImmediatelyFast compat | @Pseudo, optional |
| `compat.modernui.mixins.json` | Modern UI compat | @Pseudo, optional |
| `compat.xaero.mixins.json` | Xaero's Minimap/World Map compat | `getBuffer` null renderType 防御（Tweakeroo 灵魂出窍） |

---

## Critical Pitfalls (Migration & Development)

### 1. `doRender()` does NOT call `beginTransform`/`endTransform`
`AcceleratedBufferBuilder.doRender()` directly calls `renderer.render(this, ...)` — unlike Luna5ama's NeoForge version. Always call `beginTransform(transform, normal)` and `endTransform()` inside the `render()` callback.

### 2. Wrapper mixins MUST override `beginTransform`/`endTransform`
`SheetedDecalTextureGeneratorMixin`, `SpriteCoordinateExpanderMixin`, `EntityOutlineGeneratorMixin` must delegate these methods to `delegate.getAccelerated().beginTransform(...)` / `endTransform()`. Without this, the `IAcceleratedVertexConsumer` default (throw UnsupportedOperationException) is used.

### 3. `compile()` must use `doRender()` pattern — NOT direct `beginTransform`
In `ModelPartMixin.compile()`, call `extension.doRender(this, null, pPose.pose(), pPose.normal(), ...)` instead of `extension.beginTransform(...)`. The `doRender()` properly delegates through the wrapper chain, while `beginTransform()` on a wrapper throws UnsupportedOperationException.

### 4. `GameRendererMixin` only sets the hand flag — it does NOT draw buffers
`GameRendererMixin` sets/clears `CoreFeature.isRenderingHand()` around `renderItemInHand`. This flag is checked by `ModelPartMixin` (and `ModelBlockRendererMixin`) to skip acceleration during hand rendering. Entity buffers are drawn exclusively by `LevelRendererMixin` inside `method_62214` (world rendering). Drawing entity buffers during hand rendering would apply the hand's view-projection matrices to world entities → entities float/shift.

### 4b. Hand item acceleration — NOT working (coordinate space mismatch)
Hand item acceleration is currently **disabled** — items render via vanilla. Investigation findings:
- Full chain verified: `ItemInHandRenderer.renderItem()` → `ItemRenderer.renderStatic()` → `ItemStackRenderState.render()` → `LayerRenderState.render()` → `ItemRenderer.renderItem()` → `renderModelLists()`
- `LayerRenderState.render()` pushPose → ItemTransform → translate(-0.5,-0.5,-0.5) → `renderItem()` → foil/compass scaling
- At injection (`At.Shift.AFTER` on `renderHandsWithItems`): modelView still has `frustum`, projection is `handProjection`
- Compute shader produces `inverse(frustum) * handTransforms * itemTransforms * modelVertex`
- Vertex shader applies `handProjection * (viewMatrix * frustum) * vertexPos`
- Theoretically `frustum * inverse(frustum)` cancels → `handProjection * viewMatrix * handTransforms * itemTransforms * modelVertex` — same as vanilla
- **But stretching persists at runtime.** Static analysis cannot identify the cause — needs runtime matrix-value inspection.
- Block entities in hand (BlockEntityRenderer path, not accelerated) and empty hand (ModelPart, not accelerated) render correctly.

### 4c. GUI batching: `Lighting.setupForFlatItems()` must be paired with `Lighting.setupFor3DItems()`
When GUI batching is active, `flushBatching(GuiGraphics)` calls `Lighting.setupForFlatItems()` for item rendering. The matching `Lighting.setupFor3DItems()` and `resetDefaultLayer` calls MUST execute after rendering — if they're inside a commented-out block (item rendering path disabled for 1.21.4), the OpenGL lighting state remains stuck in FLAT mode. This persists across frames → all entities/block entities render without 3D diffuse lighting → appear gray.
- **Fix**: Move `Lighting.setupFor3DItems()`, `CoreFeature.resetDefaultLayer()`, `CoreFeature.resetDefaultLayerBeforeFunction()`, `CoreFeature.resetDefaultLayerAfterFunction()` outside any disabled code block.
- Also: `flushBatching()` (no-param) must NOT `prepareBuffers`/`drawBuffers`/`clearBuffers` on `ENTITY`/`BLOCK` — only GUI-related buffers (`POS`, `POS_TEX_COLOR`, `POS_COLOR_TEX_LIGHT`, `POS_COLOR`, `POS_TEX`).

### 4d. Modern UI compat: `@Pseudo` mixins use bytecode-dependent injection points
Modern UI mixins (`feature.modernui.mixins.json`) target obfuscated class internals via `@Local(index)` and ordinal-based `@At`. These are fragile across Modern UI versions — variable names, ordinals, and method signatures change between releases. When fixing for a new Modern UI version:
- Decompile the actual Modern UI jar with `javap -p -c -l` to get ordinals + local variable indices
- Use `@Local(index = N)` not `@Local(name = "...")` — names are removed from debug info in some releases
- Verify exact method signatures — Modern UI frequently adds/removes overloads and trailing parameters
- Example: ModernUI 3.12.0 added `boolean` to `drawUnderline`/`drawStrikethrough`, removed `w`/`h` locals from `drawText`, swapped bg/glyph vertex ordinals

**⚠️ ModernUI 版本锁定**：编译依赖必须与运行时版本一致。当前锁定 ModernUI **3.12.0.3**（Modrinth ID: `8ttmPfHK`）。3.13.0.1 移除了 `TextLayout.drawText` 和 `ModernTextRenderer.drawText` 的 `boolean isSdf` 参数（15→14 参数），`MUIStringDrawContext` 也需相应适配。

### 4e. RefMap 已废弃 — fabric-loom-remap 静态重映射注解（2026-07-18 实证更新）
本副本使用 `net.fabricmc.fabric-loom-remap` 1.17-SNAPSHOT 插件，`build.gradle` 的 `loom {}` 块**不再有** `mixin { useLegacyMixinAp = true }`。

**现状（已通过反编译产物 jar 验证）**：
- `remapJar` 时 mixin **注解字符串值被静态重写**为 intermediary 名：`@Inject(method="compile")` → `method="method_22702"`；`@At(target="...renderModelLists...")` → `...method_23182(Lnet/minecraft/class_1087;[IIIL...)V`
- refMap 文件**不再生成**（build 目录和产物 jar 中均无）
- 18 个 mixin JSON 中的 `"refmap": "acceleratedrendering-refmap.json"` 是**无害残留** — 生产环境 Mixin 只会警告找不到该文件，注解已携带 intermediary 名，功能不受影响
- 开发环境 `runClient`（Mojang 名，named 环境）：编译类中的注解未重映射，直接匹配 — 同样正常

**remap 语义规则不变**（重要）：`remap=true/false` 仍然控制静态重映射器是否处理该注入点 —
- MC 类目标（public 和 private 方法都会被 intermediary 重命名）→ 默认 `remap=true`
- 自己的类 / `@Pseudo` mod 类 → 显式 `remap=false`（防止重映射器错误处理非 MC 成员）

**历史备注**：reFabricated 原仓库用标准 Loom 1.16 + `useLegacyMixinAp = true` + refMap。旧 AP 的"非 MC 目标必须显式 remap=false 否则编译报错"的强制约束在本副本已不存在，但该约定仍应遵守。

### 4f. Known issue: GUI batching NPE with Tweakeroo Free Camera → ✅ Fixed via `compat/xaero`
Xaero's Minimap/World Map 在 Tweakeroo 灵魂出窍关闭瞬间调用 `GuiGraphics.fill(null, ...)` 传入 null RenderType → AR 的 `@WrapMethod` 跳过 `original.call()` 直达 `submitFill` → `FillDrawContext(null)` → `flushBatching` 时 `getBuffer(null)` NPE。**修复**：`compat/xaero/mixins/XaeroGuiGraphicsMixin` 用 `@ModifyVariable` 在 `MultiBufferSource$BufferSource.getBuffer(RenderType)` 入口将 null 替换为 `RenderType.gui()`。详见 [[memory/text-sign-bug-investigation]] 及 [[memory/ported-repo-migration]]。

### 4g. 包重命名与资源命名空间一致性（2026-07-18，本副本）
本副本从 reFabricated 复制而来，涉及三处必须一致的命名，曾各自引发一类故障：
1. **Java 包名 = 目录路径**：文件在 `com/namelessgod2008/` 下，package 声明必须同步。全量重命名时**必须同时替换**：856 处 import、Iris compat mixin 中 22 处 `Lcom/github/argon4w/...` 斜杠描述符（`@At(target=...)` 注入自身类）、18 个 mixin JSON 的 `"package"`/`"plugin"` 字段、fabric.mod.json 两个入口点。漏任何一类 → 编译失败或 Mixin 加载失败。
2. **资源命名空间 = MOD_ID**：`ResourceLocationUtils.create()` 用 `MOD_ID = "acceleratedrendering"` 查资源。assets 目录必须是 `assets/acceleratedrendering/`（曾是 `assets/acceleratedrendering-ported/` → 启动时 `Cannot found compute shader` 崩溃）。40 个 `.compute` shader 全部依赖此路径。
3. **processResources 模板展开**：fabric.mod.json 含 `${mod_id}` 等 7 个占位符，`filesMatching` 必须 `expand replaceProperties`（完整映射），只传 `version` 会构建失败。

### 4h. 帧图 `endLastBatch` ordinal 验证（2026-07-18，告示牌文字消失的根因之一）
1.21.4 帧图 `method_62214` 中 `endLastBatch` 出现 3 次（entities→BE→translucent 之间各一次）。`drawCoreBuffers` 必须注入在**方块实体之后**的那个 `endLastBatch`（bytecode offset 370, ordinal=1）。错用 ordinal=0（offset 336, 在 `renderEntities` 和 `renderBlockEntities` 之间）→ 方块实体阶段（告示牌、箱子等）写入加速 buffer 的数据当帧不画 → 随后被 `flushBatching`（GUI 阶段）用正交矩阵 draw→clear → 数据消失。

移植到帧图时 **ordinal 不可沿用 1.21.1 的取值** — 必须用 `javap -p -c LevelRenderer.class` 核对目标方法内同名调用的次序和上下文。详细分析见 [[text-sign-bug-investigation]]。

### 5. Rendering context checks are CRITICAL
Every accelerated mixin must check the rendering context:
- `ModelPartMixin`: only accelerate when `isRenderingLevel()` and NOT `isRenderingHand()`
- `ItemRendererMixin`: only accelerate when `isRenderingLevel()`
- Without these checks, acceleration activates during wrong rendering phases (GUI, hand, etc.) causing visual glitches or rendering failures.

### 6. `LevelRenderer.renderLevel` → frame graph in 1.21.4
In MC 1.21.4, `renderLevel` delegates rendering to frame graph lambdas (`method_62214` in Fabric intermediary). `endLastBatch()`/`endOutlineBatch()` calls are inside the lambda, NOT in `renderLevel` itself:
- `startRenderLevel`/`stopRenderLevel` → target `renderLevel` (HEAD/RETURN) with 8 params: `(GraphicsResourceAllocator, DeltaTracker, boolean, Camera, GameRenderer, Matrix4f, Matrix4f, CallbackInfo)`
- `drawCoreBuffers`/`endOutlineBatches` → target `method_62214` with 14 frame graph params
- **⚠️ `endLastBatch` ordinal 验证**：1.21.4 帧图中 `endLastBatch` 出现多次（entities→BE→translucent 各阶段之间均有）。`drawCoreBuffers` 必须注入在 **方块实体之后** 的 `endLastBatch`（off 370, ordinal=1）。错用 ordinal=0（off 336, entities 和 BE 之间）会导致方块实体阶段写入的加速数据当帧不画→随后被 GUI 批处理错误消费。**移植到帧图时 ordinal 不可沿用 1.21.1 的取值，必须用 javap 重新核对。**

### 7. frame graph lambda signature (14 params)
```java
(FogParameters, DeltaTracker, Camera, ProfilerFiller, Matrix4f, Matrix4f,
 ResourceHandle<RenderTarget>×4, boolean, Frustum, ResourceHandle<RenderTarget>, CallbackInfo)
```
**Note**: In Fabric/Yarn, `boolean` comes BEFORE `Frustum` (different from NeoForge/Mojang where Frustum comes before boolean).

### 8. Color Format: ARGB vs ABGR (GPU format)
**This is the most error-prone area.** The GPU vertex buffer expects **ABGR** byte order (Blue, Green, Red, Alpha), while 1.21.4 MC uses **ARGB** (Alpha, Red, Green, Blue) for its `ARGB.color()` API.

**Rules:**
- Colors FROM vanilla MC (`ModelPart.compile()` pColor, entity model tints): already in ABGR format → pass through directly to `mesh.write()`
- Colors WE compute (`FastColorCompat.ARGB32.color()`, block tints, shadow colors): need ARGB→ABGR conversion via `FastColorCompat.ABGR32.fromArgb32()` before `mesh.write()`
- `FastColorCompat.ABGR32.fromArgb32()` does R/B swap ONLY: `(argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16)` — NOT `Integer.reverseBytes()` (which does full byte reversal producing wrong format)
- `FastColorCompat.ABGR32.fromArgb32()` does R/B swap ONLY: `(argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16)` — NOT `Integer.reverseBytes()` (which does full byte reversal producing wrong format)

### 9. `FastColorCompat` utility
Replaces removed `net.minecraft.util.FastColor`. **⚠️ 2026-07-18 修复：4 参签名已改为与上游一致的 alpha-first 顺序**（全项目 9 处调用点均为上游移植代码、全按 `(alpha, r, g, b)` 传参，参数类型同为 int 编译器无法检测错位）。Key differences:
- `ARGB32.color(a, r, g, b)` → maps to `ARGB.color(a, r, g, b)` — 与上游 `FastColor.ARGB32.color` 签名一致
- `ARGB32.color(alpha, packedColor)` → extracts R/G/B and calls `ARGB.color(alpha, r, g, b)`
- `ARGB32.colorFromFloat(a, r, g, b)` → calls `ARGB.colorFromFloat(a, r, g, b)` — alpha FIRST
- `ABGR32.fromArgb32(argb)` → swaps R and B channels only (NOT `Integer.reverseBytes`)
- `ABGR32.alpha(abgr)` → extracts alpha byte from ABGR

### 10. `FastColorUtils.convert()` — NO-OP in 1.21.4
In 1.21.1, this swapped R/B channels to convert ABGR→ARGB. In 1.21.4, BakedQuad vertex colors are already ARGB, so `convert()` is a pass-through (`return color`).

### 11. `IQuadTransformer.STRIDE` must be used (not hardcoded `8`)
In `SimpleBakedModelMixin.render()` and `BakedQuadMixin.renderFast()`, vertex iteration must use `data.length / IQuadTransformer.STRIDE` instead of `data.length / 8`. The STRIDE is computed from `DefaultVertexFormat.BLOCK.getVertexSize() / 4` and may differ between MC versions.

### 12. `BakedModelExtension.getAccelerated()` needs `instanceof` check
Always check `if (bakedModel instanceof IAcceleratedBakedModel)` before casting. If the model mixins are disabled, the cast fails with `ClassCastException`.

### 13. `remap = false` for Mojang mappings (this project)
This project uses Mojang official mappings. Use `remap = false` on `@Inject`/`@WrapOperation` when the method name is a Mojang name. Without it, Fabric Loom remaps to intermediary names which won't match the compiled class.

### 14. `require = 0` for graceful degradation
Use `require = 0` on ALL injections that target methods whose existence is uncertain (new 1.21.4 APIs, frame graph methods). This prevents crashes when the target doesn't exist — the injection silently skips.

### 15. `ModelPartMixin` — color from MC is already GPU format
`ModelPart.compile()` receives `pColor` already in ABGR format from the entity renderer. Do NOT convert it via `fromArgb32()`. Only colors WE compute (shadows, block tints) need conversion.

### 16. `ItemRendererMixin` — use `tintLayers` array for item tinting
In 1.21.4, `renderModelLists` signature changed from `(BakedModel, ItemStack, ...)` to `(BakedModel, int[] tintLayers, ...)`. The `int[] tintLayers` is pre-computed by `TintSource`. Created `TintLayerColors` record to wrap this array and pass tints through the acceleration pipeline.

### 17. `MultipartBakedModelMixin` — avoid constructor injection
In 1.21.4, never use `@Inject(method = "<init>")` on model mixins — it can break model baking. Use lazy `isAccelerated()` evaluation with `instanceof` checks.

### 18. `WeightedBakedModelMixin` — refactored in 1.21.4
- Field `list` type changed: `List<WeightedEntry.Wrapper<BakedModel>>` → `SimpleWeightedRandomList<BakedModel>`
- Field `totalWeight` removed
- Use `list.getRandomValue(random)` instead of `WeightedRandom.getWeightedItem(list, seed)`
- Use `list.unwrap()` to iterate entries in `checkAll()`

### 18. Tab-formatted files
Many `.java` files use TAB indentation. **Avoid `sed` with `\n` or `\t` in replacement text** on Windows — it inserts literal characters instead of escape sequences. Use the Edit tool for precise replacements.

---

## 1.21.4 Migration — MC API Changes

| MC Class | Change | Our Fix |
|----------|--------|---------|
| `FastColor` → `ARGB` | Class removed | Create `FastColorCompat` utility |
| `ItemColors` | Class removed | `ItemLayerColors` returns -1 (pass-through) |
| `Direction.getNearest(float,float,float)` | Removed | Use `getNearest(int,int,int,Direction)` |
| `NativeImage.getPixelRGBA` | → `getPixel` | Rename call |
| `Registry.get(key)` | Returns `Optional<Holder.Reference<T>>` | Use `.ifPresent(ref → filtered.add(ref.value()))` |
| `TextureStateShard` constructor | `(RL, boolean, boolean)` → `(RL, TriState, boolean)` | Add `TriState.FALSE` |
| `TextureTarget` constructor | Removed 4th param `Minecraft.ON_OSX` | Remove 4th arg |
| `PropertyDispatch.QuadFunction` | Moved to `net.minecraft.client.data.models.blockstates` | Update import |
| `ArmorMaterial`/`ArmorTrim` | Moved to `net.minecraft.world.item.equipment` | Update imports |
| `BakedGlyph.render()` | Now private `(boolean,float,float,Matrix4f,VertexConsumer,int,boolean,int)` = `(italic,x,y,matrix,buffer,color,bold,packedLight)`; was 11-param public | Update param semantics: color NOT packedLight, bold NOT dropShadow |
| `BakedGlyph.Effect` | Now record: `(float,float,float,float,float,int)` | Use `effect.color()` |
| `Font.drawInBatch(String,...)` | Removed `bidirectional` param | Remove parameter |
| `Font.StringRenderOutput` | `dropShadow`, `dimFactor`, `r`, `g`, `b`, `a` fields removed | Removed from mixins JSON |
| `StringRenderOutput.finish(int,float)` | → `finish(float)` | Remove `background` parameter |
| `LevelRenderer.renderLevel()` | Frame graph refactor, `method_62214` for endBatch/endOutlineBatch | Split into HEAD/RETURN + INVOKE injections |
| `ModelPart$Polygon`/`Vertex` | Now records with methods | Added access widener for `pos`, `u`, `v`, `normal`, `vertices` |
| `ParticleEngine.render()` | `(LightTexture,Camera,float)` → `(Camera,float,BufferSource)` | Removed: pausing acceleration during particles degrades performance |
| `ItemRenderer.render()` | Removed — replaced by `renderItem()` + frame graph | `renderItem`/`renderModelLists` 均默认 `remap=true`（remapJar 静态重映射注解） |
| `GuiGraphics.innerBlit()` | First param changed to `Function<ResourceLocation,RenderType>` | Disabled |
| `WeightedBakedModel.list` | → `SimpleWeightedRandomList<BakedModel>` | Fixed: `unwrap()` + `getRandomValue()` |
| `MultipartBakedModel` | `@Shadow` fields changed; constructor injection breaks baking | Fixed: lazy Boolean cache + `instanceof` checks |

---

## Disabled Features & Status

| Feature | Status | Reason / TODO |
|---------|--------|---------------|
| **Item/block acceleration** | ✅ Working | `ItemRendererMixin` (public `renderItem` remap=true, private `renderModelLists` remap=true — 注解由 remapJar 静态重映射, no require=0) + `ModelBlockRendererMixin` + `SimpleBakedModelMixin` with color/stride fixes |
| **Entity model acceleration** | ✅ Working | `ModelPartMixin.compile()` with `isRenderingLevel()` check |
| **Entity shadows** | ✅ Working | Color conversion fix |
| **Text acceleration** | ✅ Working | BakedGlyph render param semantics fixed (color/bold/packedLight); FontMixin renderText descriptors fixed (+Z bidirectional); drawInBatch8xOutline require=0 by design; **StringRenderOutput 11-param constructor injection + FastColorCompat sign fix + LevelRenderer ordinal fix (告示牌文字, 2026-07-18)** |
| **Multipart Baked Model** | ✅ Working | Lazy Boolean cache + instanceof checks; no constructor injection |
| **Weighted Baked Model** | ✅ Working | `SimpleWeightedRandomList<BakedModel>` + `unwrap()` + `getRandomValue()` |
| **Item tinting (color)** | ✅ Working | `TintLayerColors(tintLayers)` from 1.21.4 TintSource; accelerated model path skips when tint layers present |
| **StringRenderOutput** | ✅ Working | Updated to 1.21.4: packed `color`, `drawShadow`, no `dimFactor`, `finish(float)`; **11-param constructor inject added & style color alpha fix (2026-07-18, 告示牌荧光文字)** |
| **GUI batching** (fill/blit/slot) | ✅ Working | `flushBatching()` excludes ENTITY/BLOCK; `innerBlit` updated; `Lighting` restored; `GuiMixin` scoped to `renderItemHotbar`; `AbstractContainerScreenMixin` + `InventoryScreenMixin` enabled |
| **GUI item batching** | ✅ Working | `ItemStackRenderState.render()` + `ItemModelResolver.updateForTopItem()` replaces removed `ItemRenderer.render()` |
| **GUI font/string batching** | ✅ Working | `context.drawString()` via `font.drawInBatch()` 10-param; `gui.FontMixin` active |
| **GUI slot highlight batching** | ✅ Working | `@WrapOperation` on `blitSprite` INVOKE; `sprites.getSprite()` → `submitBlit()` with `renderTypeGetter` param; AW: `GuiGraphics.sprites` + `GuiSpriteManager.getSprite` |
| **Block entity filter** | ✅ Working | Updated to 1.21.4 `render(E, float, PoseStack, MultiBufferSource)`; `tryRender` removed |
| **Entity filter** | ✅ Working | Updated to target `renderEntities` (private); `renderEntity` still exists with same signature |
| **Inventory entity rendering** | ✅ Working | `method_64045` replaces removed `method_29977` |
| **LivingEntityRenderer/HumanoidArmorLayer** | ✅ Working | Updated to 1.21.4 entity render state API; EquipmentLayerRenderer replaces renderTrim |
| **Iris compat** | ✅ Working | `vanilla.LevelRendererMixin` updated to `method_62214` (14-param); injection points verified against bytecode |
| **ImmediatelyFast compat** | ⚠️ @Pseudo | Not yet verified for 1.21.4 |
| **ModernUI compat** | ✅ Working | Updated for ModernUI 3.12.0.3: `drawText` ordinals + `@Local(index)`, `drawUnderline`/`drawStrikethrough` signature + boolean param, `isSdf` in `MUIStringDrawContext`. **编译依赖必须锁定 3.12.0.3** (#4d) |

---

## Mixin Method Reference

### ItemRendererMixin (1.21.4)
Targets `ItemRenderer.renderItem()` wrapping call to `renderModelLists()`:
- `renderItem` is `public static` in 1.21.4 — uses default `remap=true` (public methods get intermediary-mapped)
- `renderModelLists` is `private static` — uses default `remap=true`（production intermediary jar renames ALL methods including private ones; remapJar 静态重映射注解完成翻译）
- Sig: `renderModelLists(BakedModel, int[], int, int, PoseStack, VertexConsumer)`
- Handler: `(BakedModel, int[], int, int, PoseStack, VertexConsumer, Operation<Void>)` — NO ItemRenderer instance
- Uses `instanceof IAcceleratedBakedModel` check before casting
- Only accelerates when `CoreFeature.isRenderingLevel()` is true
- No `require=0` — this is a core feature, failures must be loud

### BakedGlyphMixin (1.21.4)
- `render(boolean, float, float, Matrix4f, VertexConsumer, int, boolean, int)` = `(italic, x, y, matrix, buffer, color, bold, packedLight)`
- Parameters 6-8 are `(int color, boolean bold, int packedLight)` — NOT `(int packedLight, boolean dropShadow, int color)`
- Private method → 默认 `remap=true` 即可（intermediary 同样重命名 private 方法，remapJar 静态重映射注解处理）

### FontMixin (1.21.4)
- `renderText` is private with 11 params in 1.21.4: includes final `boolean bidirectional`
- Method descriptor MUST include trailing `Z` before `)F`: `(...IIZ)F`
- Handler must accept all 11 params including `boolean bidirectional`
- `drawInBatch8xOutline`: 3 coordinated injections with `@Share`, `@Local(index)`, ordinal+shift — all `require=0` by design (bytecode-layout dependent)

### ModelBlockRendererMixin
Targets `ModelBlockRenderer.renderModel()` at HEAD. Uses `-1` for color (no tint).

### LevelRendererMixin
- HEAD/RETURN: `renderLevel` with `(GraphicsResourceAllocator, DeltaTracker, boolean, Camera, GameRenderer, Matrix4f, Matrix4f, CallbackInfo)`
- drawCoreBuffers/endOutlineBatches: `method_62214` with 14 frame graph params

### ModelPartMixin
- Only `compile()` (not `render()`) is intercepted
- Checks `isRenderingLevel()` AND not `isRenderingHand()`
- Uses `doRender()` pattern for wrapper chain delegation
