本文档使用中文编写。

# AcceleratedRendering-Ported — 项目架构与迁移笔记

> ⚠️ **重要：在用户经过游戏测试并确认功能正常前，禁止修改 CLAUDE.md、memory/*.md 和 TODO.md。只允许修改源代码（src/）。**（本文档最后一次更新：2026-07-29，经用户明确指示 — 物品加速修复已确认 & Sodium 兼容已确认）

## 🚀 新对话快速入门

你正在协助一个 Minecraft Fabric 1.21.4 模组项目 — **AcceleratedRendering-Ported**（AcceleratedRendering-reFabricated 的本地移植副本），GPU 计算着色器实体渲染加速模组（从 NeoForge 移植至 Fabric）。

**⚠️ 2026-07-18 本副本关键差异**（相对 reFabricated 原文档）：
- 项目路径：`D:\Programs\MC\1.21.4\AcceleratedRendering-Ported`（非 git 仓库）
- Java 包名已全量重命名：`com.github.argon4w.acceleratedrendering` → **`com.namelessgod2008`**（374 文件；含 mixin JSON `package`/`plugin` 字段、fabric.mod.json 入口点、Iris compat mixin 的 `Lcom/...` 描述符）
- MOD_ID 仍为 `acceleratedrendering`，资源命名空间必须是 `assets/acceleratedrendering`（见 Pitfall #4g）
- refMap 机制已废弃 — `fabric-loom-remap` 静态重映射注解（见 Pitfall #4e）
- ⚠️ 已有 5 处文本加速渲染 bug 已修（见 Pitfall #4h、#8、#9；`StringRenderOutputMixin` 11 参构造注入）；告示牌文字现在正常显示
- ⚠️ 灵魂出窍 HUD 文字消失 bug 已修复（2026-07-21，见 Pitfall #4i）：`GuiMixin` 注入点从 HEAD→`getCameraPlayer()` INVOKE AFTER，方法体被取消时永不触发合批

### 第一次读这些文件
| 文件 | 位置 | 用途 |
|------|------|------|
| **CLAUDE.md** | 项目根目录（就是本文件） | 完整架构、关键易错点、Mixin 签名参考 |
| **MEMORY.md** | `C:\Users\xzx\.claude\projects\D--Programs-MC-1-21-4-AcceleratedRendering-Ported\MEMORY.md` | 持久化知识索引（指向 `memory/` 目录下的 25 个专题文件） |
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

## 项目概览
- **本地项目**：`AcceleratedRendering-Ported` @ `D:\Programs\MC\1.21.4\AcceleratedRendering-Ported`（非 git 仓库；`rootProject.name = acceleratedrendering-ported`）
- **Java 包**：`com.namelessgod2008`（2026-07-18 从 `com.github.argon4w.acceleratedrendering` 全量重命名）
- **基于**：`ZhuRuoLing/AcceleratedRendering-reFabricated`（Fabric 移植版）
- **上游**：`Argon4W/AcceleratedRendering`（NeoForge 原始版），`Luna5ama/AcceleratedRendering`（NeoForge 1.21.4 移植版）
- **目标**：Minecraft Fabric 1.21.4，仅客户端
- **用途**：基于 GPU 计算着色器的实体模型部件渲染加速（顶点变换 + 网格缓存）
- **Java**：21，**Loom**：`net.fabricmc.fabric-loom-remap` 1.17-SNAPSHOT，**Loader**：0.19.3
- **映射**：Mojang official mappings + Parchment 1.21.4:2025.03.23（不是 Yarn）

## 构建系统
- **构建工具**：Gradle + `fabric-loom-remap` 1.17-SNAPSHOT（remapJar 静态重映射 mixin 注解，无 refMap）
- **关键依赖**：Fabric API 0.119.4+1.21.4, ForgeConfigAPIPort 21.4.3, ModMenu 13.0.3, NeoForge Event Bus 8.0.5 (`include implementation`), Lombok 1.18.40, mixinconstraints 1.0.9。`repositories` 中包含 `mavenCentral()`（forgeconfigapiport compat 需要，见 Pitfall #4l）
- **modCompileOnly**：Sodium mc1.21.4-0.6.13, Iris 1.8.8+1.21.4, ModernUI 3.12.0.3, GeckoLib 4.8, EMF 3.2.4, ImmediatelyFast 1.3.4, Trinkets-canary 3.10.0-1.21.4, TouhouLittleMaid-orihime 0.8.2, FTB Library (curse 7312255), Axiom, MaLiLib, TweakerMore
- **生产环境关键 mod**：Sodium 0.6.13, Iris 1.8.8, C2ME（始终存在，含 night-config 3.6.5）, Continuity（EmissiveBakedModel 包装器）, DistantHorizons（含旧版 night-config）, CustomSkinLoader（render-patch on CapeLayer + PlayerTabOverlay）
- **Access widener**：`src/main/resources/acceleratedrendering.accesswidener`（v2 named）
- **Mixin JSON**：18 个存在于 resources；**10 个**注册于 `fabric.mod.json` → `mixins` 数组（core、entities、items、modelparts、text、filter、compat.vanilla、compat.iris、compat.immediatelyfast、feature.modernui）；其余 8 个未注册（create、entitymodelfeature、ftb、geckolib、touhoulittlemaid、sophisticated、trinkets、tweakmore）
- **processResources**：`filesMatching("fabric.mod.json") { expand replaceProperties }` — fabric.mod.json 用 `${mod_id}` 等占位符，必须展开完整映射（只 expand version 会报 `Missing property (mod_id)`）
- **JDK**：使用 JDK 21+ 编译（`options.release = 21`）；gradle.properties `org.gradle.java.home` 指向 GraalVM JDK 24.0.2。运行用 JDK 21/24（JDK 25 移除了 `LambdaMetafactory`）

## 架构

### 核心管线
```
ModelPart.compile() → [ModelPartMixin 拦截] → AcceleratedBufferBuilder
    → 构建网格数据并缓存 → 计算着色器变换 → GPU 缓冲区
    → LevelRendererMixin 在 endLastBatch/endOutlineBatch 处绘制缓冲区

LivingEntityRenderer.render() → BufferSourceMixin 包装 BufferBuilder
    → BufferBuilder 实现 IAcceleratedVertexConsumer
    → AcceleratedBufferBuilder.doRender() → renderer.render() 回调
```

### 关键类
| 类 | 作用 |
|-------|------|
| `AcceleratedBufferBuilder` | 核心加速顶点消费者，拥有 `beginTransform`/`endTransform`/`doRender` |
| `IAcceleratedVertexConsumer` | 带有默认抛出异常方法的接口（必须被子类重写） |
| `VertexConsumerExtension` | 静态工具类：`getAccelerated(vc)` — 直接转换为 IAcceleratedVertexConsumer |
| `CoreFeature` | 全局状态：`isRenderingLevel()`、`setRenderingLevel()`、`createMeshCollector()` |
| `CoreBuffers` | 静态缓冲区实例（ENTITY、BLOCK、POS、POS_TEX_COLOR 等） |

### 包装链（实现 IAcceleratedVertexConsumer 的 VertexConsumer 包装器）
```
EntityOutlineGenerator → SheetedDecalTextureGenerator → SpriteCoordinateExpander → BufferBuilder (AcceleratedBufferBuilder)
```
每个包装器将 `isAccelerated()`、`doRender()`、`beginTransform()`、`endTransform()` 委托给其内部 `delegate`。

### Mixin 组织
| JSON | 用途 | 状态 |
|------|---------|--------|
| `core.mixins.json` | 缓冲区管线、LevelRenderer、GameRenderer | 激活 |
| `feature.entities.mixins.json` | 实体阴影渲染 | 激活 |
| `feature.modelparts.mixins.json` | ModelPart 编译加速 | 激活 |
| `feature.text.mixins.json` | BakedGlyph、Font 文本加速 | 激活（StringRenderOutput ✅ 已重新启用） |
| `feature.items.mixins.json` | 物品/方块渲染加速 | 激活（有限） |
| `compat.iris.mixins.json` | Iris 着色器兼容 | @Pseudo，可选 |
| `compat.immediatelyfast.mixins.json` | ImmediatelyFast 兼容 | @Pseudo，可选 |
| `compat.modernui.mixins.json` | Modern UI 兼容 | @Pseudo，可选 |
| `compat.xaero.mixins.json` | Xaero 小地图/世界地图兼容 | `getBuffer` null renderType 防御（Tweakeroo 灵魂出窍） |
| `compat.forgeconfigapiport.mixins.json` | forgeconfigapiport 兼容 | C2ME night-config 3.6.5 冲突 workaround（Pitfall #4l） |
| `compat.tweakeroo.mixins.json` | Tweakeroo 兼容 | 灵魂出窍安全网（Pitfall #4i） |
| `compat.rei.mixins.json` | REI 兼容 | 占位（尚未有实际 mixin） |

---

## 关键易错点（迁移与开发）

### 1. `doRender()` 不会调用 `beginTransform`/`endTransform`
`AcceleratedBufferBuilder.doRender()` 直接调用 `renderer.render(this, ...)` — 与 Luna5ama 的 NeoForge 版本不同。必须始终在 `render()` 回调内部调用 `beginTransform(transform, normal)` 和 `endTransform()`。

### 2. 包装器 mixin 必须重写 `beginTransform`/`endTransform`
`SheetedDecalTextureGeneratorMixin`、`SpriteCoordinateExpanderMixin`、`EntityOutlineGeneratorMixin` 必须将这些方法委托给 `delegate.getAccelerated().beginTransform(...)` / `endTransform()`。否则会使用 `IAcceleratedVertexConsumer` 的默认实现（抛出 UnsupportedOperationException）。

### 3. `compile()` 必须使用 `doRender()` 模式 — 不能直接调用 `beginTransform`
在 `ModelPartMixin.compile()` 中，调用 `extension.doRender(this, null, pPose.pose(), pPose.normal(), ...)` 而非 `extension.beginTransform(...)`。`doRender()` 正确地通过包装链委托调用，而对包装器调用 `beginTransform()` 会抛出 UnsupportedOperationException。

### 4. `GameRendererMixin` 仅设置手部标志 — 它不绘制缓冲区
`GameRendererMixin` 在 `renderItemInHand` 前后设置/清除 `CoreFeature.isRenderingHand()`。`ModelPartMixin`（和 `ModelBlockRendererMixin`）会检查此标志以在手部渲染期间跳过加速。实体缓冲区**仅**由 `LevelRendererMixin` 在 `method_62214`（世界渲染）内部绘制。在手部渲染期间绘制实体缓冲区会将手部的视图-投影矩阵应用于世界实体 → 实体浮动/偏移。

### 4b. 手部物品加速 — 未正常工作（坐标空间不匹配）
手部物品加速当前已**禁用** — 物品通过原版方式渲染。调查发现：
- 已验证完整调用链：`ItemInHandRenderer.renderItem()` → `ItemRenderer.renderStatic()` → `ItemStackRenderState.render()` → `LayerRenderState.render()` → `ItemRenderer.renderItem()` → `renderModelLists()`
- `LayerRenderState.render()` pushPose → ItemTransform → translate(-0.5,-0.5,-0.5) → `renderItem()` → 附魔光效/指南针缩放
- 注入点（`At.Shift.AFTER` 在 `renderHandsWithItems` 上）：modelView 仍包含 `frustum`，投影矩阵为 `handProjection`
- 计算着色器产生 `inverse(frustum) * handTransforms * itemTransforms * modelVertex`
- 顶点着色器应用 `handProjection * (viewMatrix * frustum) * vertexPos`
- 理论上 `frustum * inverse(frustum)` 互相抵消 → `handProjection * viewMatrix * handTransforms * itemTransforms * modelVertex` — 与原版一致
- **但运行时时拉伸仍然存在。** 静态分析无法确定原因 — 需要运行时矩阵值检查。
- 手中的方块实体（BlockEntityRenderer 路径，未加速）和空手（ModelPart，未加速）渲染正常。

### 4c. GUI 批处理：`Lighting.setupForFlatItems()` 必须与 `Lighting.setupFor3DItems()` 配对使用
当 GUI 批处理激活时，`flushBatching(GuiGraphics)` 为物品渲染调用 `Lighting.setupForFlatItems()`。匹配的 `Lighting.setupFor3DItems()` 和 `resetDefaultLayer` 调用必须在渲染之后执行 — 如果它们在注释掉的代码块内部（物品渲染路径因 1.21.4 而被禁用），OpenGL 光照状态将卡在 FLAT 模式。此状态跨帧持续 → 所有实体/方块实体无 3D 漫反射光照渲染 → 呈现灰色。
- **修复**：将 `Lighting.setupFor3DItems()`、`CoreFeature.resetDefaultLayer()`、`CoreFeature.resetDefaultLayerBeforeFunction()`、`CoreFeature.resetDefaultLayerAfterFunction()` 移出任何被禁用的代码块。
- 另外：`flushBatching()`（无参数版本）不得对 `ENTITY`/`BLOCK` 执行 `prepareBuffers`/`drawBuffers`/`clearBuffers` — 仅处理 GUI 相关缓冲区（`POS`、`POS_TEX_COLOR`、`POS_COLOR_TEX_LIGHT`、`POS_COLOR`、`POS_TEX`）。

### 4d. Modern UI 兼容：`@Pseudo` mixin 使用依赖字节码的注入点
Modern UI mixin（`feature.modernui.mixins.json`）通过 `@Local(index)` 和基于序号的 `@At` 定位混淆后的类内部实现。这些在 Modern UI 版本之间很脆弱 — 变量名、序号和方法签名在各版本间会发生变化。为新版 Modern UI 修复时：
- 用 `javap -p -c -l` 反编译实际 Modern UI jar 以获取序号 + 局部变量索引
- 使用 `@Local(index = N)` 而非 `@Local(name = "...")` — 某些版本的调试信息中移除了名称
- 验证精确的方法签名 — Modern UI 经常添加/移除重载和尾部参数
- 示例：ModernUI 3.12.0 为 `drawUnderline`/`drawStrikethrough` 添加了 `boolean` 参数，从 `drawText` 中移除了 `w`/`h` 局部变量，交换了背景/字形顶点序号

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

### 4f. 已知问题：Tweakeroo 灵魂出窍时 GUI 批处理 NPE → ✅ 已通过 `compat/xaero` 修复
Xaero 小地图/世界地图在 Tweakeroo 灵魂出窍关闭瞬间调用 `GuiGraphics.fill(null, ...)` 传入 null RenderType → AR 的 `@WrapMethod` 跳过 `original.call()` 直达 `submitFill` → `FillDrawContext(null)` → `flushBatching` 时 `getBuffer(null)` 导致 NPE。**修复**：`compat/xaero/mixins/XaeroGuiGraphicsMixin` 用 `@ModifyVariable` 在 `MultiBufferSource$BufferSource.getBuffer(RenderType)` 入口将 null 替换为 `RenderType.gui()`。详见 [[memory/text-sign-bug-investigation]] 及 [[memory/ported-repo-migration]]。

### 4g. 包重命名与资源命名空间一致性（2026-07-18，本副本）
本副本从 reFabricated 复制而来，涉及三处必须一致的命名，曾各自引发一类故障：
1. **Java 包名 = 目录路径**：文件在 `com/namelessgod2008/` 下，package 声明必须同步。全量重命名时**必须同时替换**：856 处 import、Iris compat mixin 中 22 处 `Lcom/github/argon4w/...` 斜杠描述符（`@At(target=...)` 注入自身类）、18 个 mixin JSON 的 `"package"`/`"plugin"` 字段、fabric.mod.json 两个入口点。漏任何一类 → 编译失败或 Mixin 加载失败。
2. **资源命名空间 = MOD_ID**：`ResourceLocationUtils.create()` 用 `MOD_ID = "acceleratedrendering"` 查资源。assets 目录必须是 `assets/acceleratedrendering/`（曾是 `assets/acceleratedrendering-ported/` → 启动时 `Cannot found compute shader` 崩溃）。40 个 `.compute` shader 全部依赖此路径。
3. **processResources 模板展开**：fabric.mod.json 含 `${mod_id}` 等 7 个占位符，`filesMatching` 必须 `expand replaceProperties`（完整映射），只传 `version` 会构建失败。

### 4h. 帧图 `endLastBatch` ordinal 验证（2026-07-18，告示牌文字消失的根因之一）
1.21.4 帧图 `method_62214` 中 `endLastBatch` 出现 3 次（entities→BE→translucent 之间各一次）。`drawCoreBuffers` 必须注入在**方块实体之后**的那个 `endLastBatch`（bytecode offset 370, ordinal=1）。错用 ordinal=0（offset 336, 在 `renderEntities` 和 `renderBlockEntities` 之间）→ 方块实体阶段（告示牌、箱子等）写入加速 buffer 的数据当帧不画 → 随后被 `flushBatching`（GUI 阶段）用正交矩阵 draw→clear → 数据消失。

移植到帧图时 **ordinal 不可沿用 1.21.1 的取值** — 必须用 `javap -p -c LevelRenderer.class` 核对目标方法内同名调用的次序和上下文。详细分析见 [[text-sign-bug-investigation]]。

### 4i. ✅ 已修复：灵魂出窍时 GUI 批处理导致全局 HUD overlay 文字消失（2026-07-18 发现，2026-07-21 根因修复）

**症状**：Tweakeroo Free Camera（灵魂出窍）激活时，若同时启用 "GUI物品加速"+"GUI物品合批"，所有 HUD overlay 文字（MiniHUD/Xaero/告示牌/F3/ESC）消失。禁用任一 config 或打开物品栏立即恢复。

**根因**（2026-07-21 定位）：Mixin 的 `@At("HEAD")` 回调全部执行完毕后才会检查 `ci.cancel()` 标志。Tweakeroo 的 `MixinInGameHud_freeCam` 在 HEAD 处通过 `ci.cancel()` 取消 `renderItemHotbar` 方法体——但 AR 的 `GuiMixin` 也在 HEAD 处调用 `startBatching()`，此时方法体尚未被取消，`GUI_BATCHING` 已被设为 `true`。方法体被跳过无任何渲染发生，但 `GUI_BATCHING = true` 触发了所有 @WrapMethod/@ModifyReturnValue 拦截器的字节码叠加，在 1.21.4 的 `LayeredDraw` 渲染生命周期中破坏后续文字渲染。

**修复**（`GuiMixin.java`，1 行变更）：将 `startBatching` 注入点从 `@At("HEAD")` 移至 `renderItemHotbar` 方法体内的首个 INVOKE 之后：
```java
@Inject(method = "renderItemHotbar",
    at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/Gui;getCameraPlayer()Lnet/minecraft/world/entity/player/Player;",
        shift = At.Shift.AFTER))
```
`getCameraPlayer()` 是 `renderItemHotbar` 方法体中的第一条指令。若 Tweakeroo 在 HEAD 处取消方法体，该 INVOKE 永不执行 → 注入点永不触发 → `startBatching` 永不调用 → `GUI_BATCHING` 永不为 true → 无 bug。此修复通用，不依赖 `cameraEntity` 检测、不依赖 Mixin priority。

**旧 workaround 已移除**：`compat/tweakeroo/mixins/TweakerooGuiMixin` 中的 `CameraEntity != player → ci.cancel()` 判定已移除，保留为无害安全网。

### 4j. ✅ 已修复：物品加速在生产环境失效（Sodium FRAPI 冲突，2026-07-29）

**症状**：生产环境（含 Sodium 0.6.13）物品加速完全失效——开关物品加速帧数不变，物品走原版渲染。开发环境（无 Sodium）正常。

**根因**（2026-07-29 定位）：Sodium 的 FRAPI `ItemRendererMixin`（`features.render.frapi.ItemRendererMixin`）在 `ItemRenderer.renderItem()` HEAD 处注入 `@Inject(cancellable=true)`。当它取消方法时，整个 `renderItem` 的方法体被跳过——包括我们的 `@WrapOperation` 在 `renderModelLists` INVOKE 处的包装器。这就是为什么物品加速永远不触发。

**修复**（`ItemRendererMixin.java`，全面重写）：
1. 将注入策略从 `@WrapOperation` 在 `renderModelLists` INVOKE 处改为 **`@Inject` 在 `renderItem` HEAD 处，`priority=999`**（高于 Sodium 默认 priority=1000）
2. 在回调中直接检查加速条件 → 满足则 `ci.cancel()` 并用 GPU 渲染 → 不满足则让 Sodium/原版处理
3. 附魔物品（`foilType != NONE`）不加速——加速管线不支持 `VertexMultiConsumer` 箔片包装

**关键教训**：
- `@WrapOperation` 包装的是目标方法**内部**的 INVOKE 指令。如果目标方法本身被另一个 mixin 的 `@Inject(cancellable=true)` 取消，包装器永远没有机会执行
- 解决办法：用自己的 `@Inject` 在 HEAD 处（上层的 mixin 回调），在自己执行加速的同时 cancel 掉方法体
- `priority=999` 确保回调在 Sodium 默认 priority=1000 之前执行——必须先加速再让 Sodium 处理
- 生产环境 `fabric-loom-remap` 不会影响 `priority` 属性（已验证：`RuntimeInvisibleAnnotations: priority=999`）

**⚠️ 物品渲染调用链澄清（ItemRenderType.LAYER vs SIMPLE）**：
- `ItemStackRenderState.render()` 内部遍历 layers，对每个 layer 调用 `ItemRenderType.render()`
- **ItemRenderType.LAYER** → 调用 `LayerRenderState.render()` → 内部调用 `ItemRenderer.renderItem()`（invokestatic）→ 我们的 `@Inject` 在 `renderItem` HEAD 拦截到
- **ItemRenderType.SIMPLE** → 调用自己的渲染逻辑，**不经过** `ItemRenderer.renderItem()` → 我们的注入点不会触发
- GUI 物品渲染同样经过 `ItemStackRenderState.render()` → `ItemRenderType.render()`（或 `renderInGui()`）
- 因此：我们的物品加速仅对 **LAYER 类型** 的物品生效；SIMPLE 类型（如 2D 平面物品、某些 GUI 专用渲染）走原版路径

**⚠️ Continuity EmissiveBakedModel 包装链**：
Continuity mod 会在模型外层包裹 `EmissiveBakedModel`，该 wrapper 实现了 `BakedModel` 但**不实现** `IAcceleratedBakedModel`。当 `ItemRendererMixin` 的 `instanceof IAcceleratedBakedModel` 检查遇到被 Continuity 包装的模型时，检查失败 → 物品走原版渲染。详见 Pitfall #4m。

### 4k. ✅ 已修复：GuiBatchingController `depthLayers` 空指针（2026-07-29）

**症状**：生产环境 `simpleshulkerpreview` mod 通过 REI 触发 `GuiGraphics.fill()` 时，`GuiBatchingController.submitFill()` 在 `depthLayers.lastEntry().getKey()` 处 NPE 崩溃。

**根因**：`submitFill()` 和 `submitGradient()` 在处理无 depth 的 RenderType 时，假设 `depthLayers` 已包含至少一层。但当首个 fill 调用触发时，`depthLayers` 为空 → `lastEntry()` 返回 null → NPE。

**修复**（`GuiBatchingController.java`）：两个方法在访问 `lastEntry()` 前添加 `depthLayers.isEmpty()` 守卫——空时创建首层并直接添加 fill/gradient context。

**⚠️ 设备依赖性**：此 NPE 在多数设备上不触发，因为正常的 GUI 渲染流程中首个调用通常是 `submitBlit`（会初始化 `depthLayers`）。只有在特定 GPU/驱动组合 + 特定 mod 调用顺序下，首个调用才会是 `submitFill` 且 `depthLayers` 为空。`simpleshulkerpreview` 恰好触发了此边缘情况。

### 4l. ✅ 已修复：forgeconfigapiport 配置保存崩溃（C2ME night-config 冲突，2026-07-29）

**症状**：通过 Mod Menu 关闭配置页面时游戏崩溃：`NoSuchFieldError: WritingMode.REPLACE_ATOMIC`。

**根因**：C2ME 通过 jar-in-jar 打包了 night-config **3.6.5**（2022 版），而 forgeconfigapiport 打包了 **3.8.1**。`WritingMode.REPLACE_ATOMIC` 在 3.7.0 才加入。Fabric Loader 加载了 C2ME 的旧版本 → `ConfigTracker.writeConfig` 的字节码中 `getstatic REPLACE_ATOMIC` 指令在类链接时抛出 `NoSuchFieldError`。

**修复**（新建 `compat/forgeconfigapiport/mixins/LoadedConfigMixin.java`）：
- `@WrapOperation` 包装 `LoadedConfig.save()` 中对 `ConfigTracker.writeConfig()` 的调用
- 捕获 `NoSuchFieldError`，降级使用 `WritingMode.valueOf("REPLACE")`（所有版本都有）
- `@Pseudo` + `remap=false` + `require=0`（目标为 forgeconfigapiport 内部类）
- 注册于 `acceleratedrendering.compat.forgeconfigapiport.mixins.json` → `fabric.mod.json`

**已尝试但失败的方案**：
- 在 build.gradle 中 `include("com.electronwill.night-config:core:3.8.1")` → Fabric Loader 仍加载 3.6.5
- 删除 `.fabric/processedMods` → 3.6.5 被重新提取
- `include implementation` night-config 到自己 JAR → 类加载器仍优先加载 C2ME 版本

### 4m. ⚠️ Continuity EmissiveBakedModel 包装导致物品加速跳过（2026-07-29 发现）

Continuity mod 通过 mixin 将模型包裹在 `EmissiveBakedModel` 中（自定义 BakedModel 实现），该包装器：
- 实现了 `BakedModel` 接口
- **不实现** `IAcceleratedBakedModel` 接口
- 在 `ItemRenderer.renderItem()` 被调用时，传入的 `BakedModel` 参数可能已被 Continuity 的 mixin 替换为 `EmissiveBakedModel`

**影响**：`ItemRendererMixin.accelerateAtHead()` 中的 `bakedModel instanceof IAcceleratedBakedModel accelModel` 检查失败 → `isAccelerated()` 路径被跳过 → 回退到 fallback 的 Quad 烘焙渲染（`shouldBakeMeshForQuad()`），或者如果 Quad 烘焙也失败，则走原版渲染。

**当前状态**：尚未修复。可能的方案：
- 在 `instanceof` 检查前尝试 unwrap Continuity 的包装器（通过反射获取内部 delegate model）
- 或添加 Continuity compat mixin 让 EmissiveBakedModel 实现 `IAcceleratedBakedModel`

**注意**：此问题只在用户安装了 Continuity mod 的生产环境中出现。

### 4n. ⚠️ CustomSkinLoader (CSL) render-patch 兼容性（2026-07-29 知悉）

CustomSkinLoader 使用 render-patch 技术注入 `CapeLayer` 和 `PlayerTabOverlay`。这意味着 CSL 可能在玩家渲染管线中修改渲染状态或替换模型，可能与我们的实体加速产生交互。当前尚未观察到具体 bug，但需要留意此兼容点。

### 4o. ✅ 已修复：GUI_BATCHING 在单机世界切换后卡住（2026-07-29 知悉）

此前认为 `resetGuiBatching` 仅在多人游戏中需要（离开服务器后屏幕过渡），但实际单机游戏进出世界也会触发相同问题。`LevelRendererMixin.startRenderLevel()` 中的防御性 `resetGuiBatching` 在单机和多人游戏都生效——每次 `renderLevel` 开始前都会清理残留的 GUI_BATCHING 状态。

### 4p. MaLiLib + processResources 交互（2026-07-29 知悉）

MaLiLib 及其衍生 mod（Tweakeroo、MiniHUD 等）使用 build-time JSON minification（在构建时移除 JSON 注释和空白字符）。如果我们的 `processResources` 配置不当（如 expand 重写 fabric.mod.json 的时机），可能与 MalilLib 的 JSON 处理产生交互。当前已确认 `processResources` 使用 `filesMatching("fabric.mod.json") { expand replaceProperties }` 会展开所有占位符，如果在 MalilLib 相关 mod 之前执行则无冲突。

**注意**：`processResources` 输出 warning "Overlapping outputs with mod" 是正常的——Gradle 在 expand 时临时创建重叠输出，Loom 最终会处理 remap。

### 5. 渲染上下文检查至关重要
每个加速 mixin 都必须检查渲染上下文：
- `ModelPartMixin`：仅当 `isRenderingLevel()` 为 true 且**不是** `isRenderingHand()` 时才加速
- `ItemRendererMixin`：仅当 `isRenderingLevel()` 为 true 时才加速
- 没有这些检查，加速会在错误的渲染阶段（GUI、手部等）激活，导致视觉故障或渲染失败。

### 6. `LevelRenderer.renderLevel` → 1.21.4 中的帧图
在 MC 1.21.4 中，`renderLevel` 将渲染委托给帧图 lambda（Fabric intermediary 中的 `method_62214`）。`endLastBatch()`/`endOutlineBatch()` 调用位于 lambda 内部，而不是 `renderLevel` 本身：
- `startRenderLevel`/`stopRenderLevel` → 目标 `renderLevel`（HEAD/RETURN），8 个参数：`(GraphicsResourceAllocator, DeltaTracker, boolean, Camera, GameRenderer, Matrix4f, Matrix4f, CallbackInfo)`
- `drawCoreBuffers`/`endOutlineBatches` → 目标 `method_62214`，14 个帧图参数
- **⚠️ `endLastBatch` ordinal 验证**：1.21.4 帧图中 `endLastBatch` 出现多次（entities→BE→translucent 各阶段之间均有）。`drawCoreBuffers` 必须注入在 **方块实体之后** 的 `endLastBatch`（off 370, ordinal=1）。错用 ordinal=0（off 336, entities 和 BE 之间）会导致方块实体阶段写入的加速数据当帧不画→随后被 GUI 批处理错误消费。**移植到帧图时 ordinal 不可沿用 1.21.1 的取值，必须用 javap 重新核对。**

### 7. 帧图 lambda 签名（14 个参数）
```java
(FogParameters, DeltaTracker, Camera, ProfilerFiller, Matrix4f, Matrix4f,
 ResourceHandle<RenderTarget>×4, boolean, Frustum, ResourceHandle<RenderTarget>, CallbackInfo)
```
**注意**：在 Fabric/Yarn 中，`boolean` 在 `Frustum` **之前**（与 NeoForge/Mojang 中 Frustum 在 boolean 之前不同）。

### 8. 颜色格式：ARGB vs ABGR（GPU 格式）
**这是最容易出错的领域。** GPU 顶点缓冲区期望 **ABGR** 字节顺序（蓝、绿、红、Alpha），而 1.21.4 MC 在其 `ARGB.color()` API 中使用 **ARGB**（Alpha、红、绿、蓝）。

**规则：**
- 来自原版 MC 的颜色（`ModelPart.compile()` 的 pColor、实体模型着色）：已经是 ABGR 格式 → 直接传递到 `mesh.write()`
- 我们计算的颜色（`FastColorCompat.ARGB32.color()`、方块着色、阴影颜色）：需要通过 `FastColorCompat.ABGR32.fromArgb32()` 进行 ARGB→ABGR 转换后再传给 `mesh.write()`
- `FastColorCompat.ABGR32.fromArgb32()` **仅**交换 R/B 通道：`(argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16)` — 不是 `Integer.reverseBytes()`（后者做完全字节反转，产生错误格式）

### 9. `FastColorCompat` 工具类
替代已移除的 `net.minecraft.util.FastColor`。**⚠️ 2026-07-18 修复：4 参签名已改为与上游一致的 alpha-first 顺序**（全项目 9 处调用点均为上游移植代码、全按 `(alpha, r, g, b)` 传参，参数类型同为 int 编译器无法检测错位）。关键区别：
- `ARGB32.color(a, r, g, b)` → 映射到 `ARGB.color(a, r, g, b)` — 与上游 `FastColor.ARGB32.color` 签名一致
- `ARGB32.color(alpha, packedColor)` → 提取 R/G/B 并调用 `ARGB.color(alpha, r, g, b)`
- `ARGB32.colorFromFloat(a, r, g, b)` → 调用 `ARGB.colorFromFloat(a, r, g, b)` — alpha 在最前面
- `ABGR32.fromArgb32(argb)` → 仅交换 R 和 B 通道（不是 `Integer.reverseBytes`）
- `ABGR32.alpha(abgr)` → 从 ABGR 中提取 alpha 字节

### 10. `FastColorUtils.convert()` — 在 1.21.4 中为空操作
在 1.21.1 中，此方法交换 R/B 通道以将 ABGR 转换为 ARGB。在 1.21.4 中，BakedQuad 顶点颜色已经是 ARGB，所以 `convert()` 直接透传（`return color`）。

### 11. 必须使用 `IQuadTransformer.STRIDE`（不能硬编码 `8`）
在 `SimpleBakedModelMixin.render()` 和 `BakedQuadMixin.renderFast()` 中，顶点迭代必须使用 `data.length / IQuadTransformer.STRIDE` 而非 `data.length / 8`。STRIDE 由 `DefaultVertexFormat.BLOCK.getVertexSize() / 4` 计算得出，不同 MC 版本可能不同。

### 12. `BakedModelExtension.getAccelerated()` 需要 `instanceof` 检查
在转换前务必检查 `if (bakedModel instanceof IAcceleratedBakedModel)`。如果模型 mixin 被禁用，转换会因 `ClassCastException` 而失败。

### 13. 本项目的 `remap = false`（Mojang 映射）
本项目使用 Mojang official mappings。当方法名是 Mojang 名时，在 `@Inject`/`@WrapOperation` 上使用 `remap = false`。否则 Fabric Loom 将其重映射为 intermediary 名，无法匹配编译后的类。

### 14. 使用 `require = 0` 实现优雅降级
对所有目标方法存在性不确定的注入（新的 1.21.4 API、帧图方法）使用 `require = 0`。这可以防止目标不存在时崩溃 — 注入静默跳过。

### 15. `ModelPartMixin` — 来自 MC 的颜色已经是 GPU 格式
`ModelPart.compile()` 接收的 `pColor` 已经是 ABGR 格式（来自实体渲染器）。不要通过 `fromArgb32()` 转换它。只有我们计算的颜色（阴影、方块着色）需要转换。

### 16. `ItemRendererMixin` — 使用 `tintLayers` 数组进行物品着色
在 1.21.4 中，`renderModelLists` 的签名从 `(BakedModel, ItemStack, ...)` 变更为 `(BakedModel, int[] tintLayers, ...)`。`int[] tintLayers` 由 `TintSource` 预先计算。创建了 `TintLayerColors` 记录来包装此数组并通过加速管线传递着色信息。

### 17. `MultipartBakedModelMixin` — 避免构造函数注入
在 1.21.4 中，永远不要在模型 mixin 上使用 `@Inject(method = "<init>")` — 它可能破坏模型烘焙。改用带 `instanceof` 检查的惰性 `isAccelerated()` 求值。

### 18. `WeightedBakedModelMixin` — 在 1.21.4 中重构
- 字段 `list` 类型变更：`List<WeightedEntry.Wrapper<BakedModel>>` → `SimpleWeightedRandomList<BakedModel>`
- 字段 `totalWeight` 已移除
- 使用 `list.getRandomValue(random)` 替代 `WeightedRandom.getWeightedItem(list, seed)`
- 使用 `list.unwrap()` 在 `checkAll()` 中遍历条目

### 19. 使用 TAB 缩进的文件
许多 `.java` 文件使用 TAB 缩进。**在 Windows 上避免在 `sed` 的替换文本中使用 `\n` 或 `\t`** — 它会插入字面字符而非转义序列。

**⚠️ Claude Code Edit 工具与 TAB**：Claude Code 内置的 Edit 工具在处理含 TAB 字符的文件时经常匹配失败（TAB 在渲染时被规范化或转义）。遇到 Edit 工具连续失败的情况，改用 **Python 脚本** 进行精确替换。示例：
```python
import pathlib
content = pathlib.Path("file.java").read_text()
content = content.replace("old\ttext", "new\ttext")  # 精确匹配 TAB
pathlib.Path("file.java").write_text(content)
```


---

## 1.21.4 迁移 — MC API 变更

| MC 类 | 变更 | 我们的修复 |
|----------|--------|---------|
| `FastColor` → `ARGB` | 类已移除 | 创建 `FastColorCompat` 工具类 |
| `ItemColors` | 类已移除 | `ItemLayerColors` 返回 -1（透传） |
| `Direction.getNearest(float,float,float)` | 已移除 | 使用 `getNearest(int,int,int,Direction)` |
| `NativeImage.getPixelRGBA` | → `getPixel` | 重命名调用 |
| `Registry.get(key)` | 返回 `Optional<Holder.Reference<T>>` | 使用 `.ifPresent(ref → filtered.add(ref.value()))` |
| `TextureStateShard` 构造函数 | `(RL, boolean, boolean)` → `(RL, TriState, boolean)` | 添加 `TriState.FALSE` |
| `TextureTarget` 构造函数 | 移除第 4 个参数 `Minecraft.ON_OSX` | 移除第 4 个参数 |
| `PropertyDispatch.QuadFunction` | 移至 `net.minecraft.client.data.models.blockstates` | 更新 import |
| `ArmorMaterial`/`ArmorTrim` | 移至 `net.minecraft.world.item.equipment` | 更新 imports |
| `BakedGlyph.render()` | 现为 private `(boolean,float,float,Matrix4f,VertexConsumer,int,boolean,int)` = `(italic,x,y,matrix,buffer,color,bold,packedLight)`；原为 11 参数 public | 更新参数语义：color 不是 packedLight，bold 不是 dropShadow |
| `BakedGlyph.Effect` | 现为 record：`(float,float,float,float,float,int)` | 使用 `effect.color()` |
| `Font.drawInBatch(String,...)` | 移除 `bidirectional` 参数 | 移除参数 |
| `Font.StringRenderOutput` | `dropShadow`、`dimFactor`、`r`、`g`、`b`、`a` 字段已移除 | 从 mixins JSON 中移除 |
| `StringRenderOutput.finish(int,float)` | → `finish(float)` | 移除 `background` 参数 |
| `LevelRenderer.renderLevel()` | 帧图重构，`method_62214` 用于 endBatch/endOutlineBatch | 拆分为 HEAD/RETURN + INVOKE 注入 |
| `ModelPart$Polygon`/`Vertex` | 现为带方法的 record | 为 `pos`、`u`、`v`、`normal`、`vertices` 添加 access widener |
| `ParticleEngine.render()` | `(LightTexture,Camera,float)` → `(Camera,float,BufferSource)` | 已移除：粒子期间暂停加速会降低性能 |
| `ItemRenderer.render()` | 已移除 — 由 `renderItem()` + 帧图替代 | `renderItem`/`renderModelLists` 均默认 `remap=true`（remapJar 静态重映射注解） |
| `GuiGraphics.innerBlit()` | 第一个参数变更为 `Function<ResourceLocation,RenderType>` | 已禁用 |
| `WeightedBakedModel.list` | → `SimpleWeightedRandomList<BakedModel>` | 已修复：`unwrap()` + `getRandomValue()` |
| `MultipartBakedModel` | `@Shadow` 字段变更；构造函数注入破坏烘焙 | 已修复：惰性 Boolean 缓存 + `instanceof` 检查 |

---

## 已禁用功能与状态

| 功能 | 状态 | 原因 / 待办 |
|---------|--------|---------------|
| **物品/方块加速** | ✅ 正常工作（含 Sodium） | `ItemRendererMixin`：`@Inject` 在 `renderItem` HEAD，`priority=999`（高于 Sodium FRAPI）。附魔物品走原版。`ModelBlockRendererMixin` + `SimpleBakedModelMixin` 带颜色/stride 修复 |
| **实体模型加速** | ✅ 正常工作 | `ModelPartMixin.compile()` 带 `isRenderingLevel()` 检查 |
| **实体阴影** | ✅ 正常工作 | 颜色转换修复 |
| **文字加速** | ✅ 正常工作 | BakedGlyph 渲染参数语义已修复（color/bold/packedLight）；FontMixin renderText 描述符已修复（+Z bidirectional）；drawInBatch8xOutline require=0 按设计；**StringRenderOutput 11 参构造注入 + FastColorCompat 签名修复 + LevelRenderer ordinal 修复（告示牌文字，2026-07-18）** |
| **Multipart 烘焙模型** | ✅ 正常工作 | 惰性 Boolean 缓存 + instanceof 检查；无构造函数注入 |
| **Weighted 烘焙模型** | ✅ 正常工作 | `SimpleWeightedRandomList<BakedModel>` + `unwrap()` + `getRandomValue()` |
| **物品着色（颜色）** | ✅ 正常工作 | 来自 1.21.4 TintSource 的 `TintLayerColors(tintLayers)`；加速模型路径在存在着色层时跳过 |
| **StringRenderOutput** | ✅ 正常工作 | 已更新至 1.21.4：打包的 `color`、`drawShadow`，无 `dimFactor`，`finish(float)`；**11 参构造注入已添加 & 样式颜色 alpha 修复（2026-07-18，告示牌荧光文字）** |
| **GUI 批处理**（fill/blit/slot） | ✅ 正常工作 | `flushBatching()` 排除 ENTITY/BLOCK；`innerBlit` 已更新；`Lighting` 已恢复；`GuiMixin` 限定于 `renderItemHotbar`；`AbstractContainerScreenMixin` + `InventoryScreenMixin` 已启用 |
| **GUI 物品批处理** | ✅ 正常工作 | `ItemStackRenderState.render()` + `ItemModelResolver.updateForTopItem()` 替代已移除的 `ItemRenderer.render()` |
| **GUI 字体/字符串批处理** | ✅ 正常工作 | `context.drawString()` 通过 `font.drawInBatch()` 10 参数；`gui.FontMixin` 激活 |
| **GUI 物品栏高亮批处理** | ✅ 正常工作 | `@WrapOperation` 在 `blitSprite` INVOKE 上；`sprites.getSprite()` → `submitBlit()` 带 `renderTypeGetter` 参数；AW：`GuiGraphics.sprites` + `GuiSpriteManager.getSprite` |
| **方块实体过滤器** | ✅ 正常工作 | 已更新至 1.21.4 `render(E, float, PoseStack, MultiBufferSource)`；`tryRender` 已移除 |
| **实体过滤器** | ✅ 正常工作 | 已更新以目标 `renderEntities`（private）；`renderEntity` 仍以相同签名存在 |
| **物品栏实体渲染** | ✅ 正常工作 | `method_64045` 替代已移除的 `method_29977` |
| **LivingEntityRenderer/HumanoidArmorLayer** | ✅ 正常工作 | 已更新至 1.21.4 实体渲染状态 API；EquipmentLayerRenderer 替代 renderTrim |
| **Iris 兼容** | ✅ 正常工作 | `vanilla.LevelRendererMixin` 已更新至 `method_62214`（14 参数）；注入点已对照字节码验证 |
| **ImmediatelyFast 兼容** | ⚠️ @Pseudo | 尚未针对 1.21.4 验证 |
| **ModernUI 兼容** | ✅ 正常工作 | 已更新至 ModernUI 3.12.0.3：`drawText` 序号 + `@Local(index)`，`drawUnderline`/`drawStrikethrough` 签名 + boolean 参数，`MUIStringDrawContext` 中的 `isSdf`。**编译依赖必须锁定 3.12.0.3**（#4d） |
| **Continuity 兼容** | ⚠️ 已知问题 | EmissiveBakedModel 包装导致物品加速跳过（`instanceof IAcceleratedBakedModel` 失败）。见 Pitfall #4m。 |
| **CustomSkinLoader 兼容** | ⚠️ 需留意 | CSL render-patch 注入 `CapeLayer`/`PlayerTabOverlay`，可能影响实体渲染。尚未观察到具体 bug。见 Pitfall #4n。 |
| **C2ME 兼容** | ✅ 正常 | C2ME 始终存在于生产环境。`LoadedConfigMixin` 的 night-config 降级 fix 始终生效。见 Pitfall #4l。 |

---

## Mixin 方法参考

### ItemRendererMixin（1.21.4，2026-07-29 全面重写）
**已从 `@WrapOperation` 在 `renderModelLists` 改为 `@Inject` 在 `renderItem` HEAD**：
- `@Mixin(value = {ItemRenderer.class}, priority = 999)` — 高于 Sodium（默认 1000），先于 Sodium 的 FRAPI mixin 执行
- `@Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)` — 在方法体执行前拦截
- 回调签名：`(ItemDisplayContext, PoseStack, MultiBufferSource, int, int, int[], BakedModel, RenderType, FoilType, CallbackInfo)` — 匹配 renderItem 的 9 参签名
- 检查 `isRenderingLevel()` + `foilType == NONE` + `isAccelerated()` — 全部通过才 `ci.cancel()` 并执行 GPU 渲染
- 附魔物品（`foilType != NONE`）直接返回，让原版处理（加速管线不支持 VertexMultiConsumer 箔片包裹）
- **调用链**：`ItemStackRenderState.render()` → `ItemRenderType.render()` →（LAYER 类型）→ `LayerRenderState.render()` → `ItemRenderer.renderItem()`（invokestatic @ HEAD 被拦截）
- **ItemRenderType.SIMPLE** 不经过 `ItemRenderer.renderItem()` → 我们的注入点不触发 → SIMPLE 类型物品走原版渲染
- **不再使用 `@WrapOperation`**，因为 Sodium FRAPI 用 `@Inject(cancellable=true)` 取消方法体后，任何方法体内的 INVOKE 包装器都无法触发。详见 Pitfall #4j。
- **Continuity 包装**：`EmissiveBakedModel` 包裹模型 → `instanceof IAcceleratedBakedModel` 失败 → fallback 到 Quad 烘焙渲染。详见 Pitfall #4m。

### BakedGlyphMixin（1.21.4）
- `render(boolean, float, float, Matrix4f, VertexConsumer, int, boolean, int)` = `(italic, x, y, matrix, buffer, color, bold, packedLight)`
- 参数 6-8 为 `(int color, boolean bold, int packedLight)` — 不是 `(int packedLight, boolean dropShadow, int color)`
- Private 方法 → 默认 `remap=true` 即可（intermediary 同样重命名 private 方法，remapJar 静态重映射注解处理）

### FontMixin（1.21.4）
- `renderText` 在 1.21.4 中为 private，11 个参数：包含最后的 `boolean bidirectional`
- 方法描述符必须在 `)F` 前包含尾随 `Z`：`(...IIZ)F`
- 处理器必须接受全部 11 个参数，包括 `boolean bidirectional`
- `drawInBatch8xOutline`：3 个协调注入，使用 `@Share`、`@Local(index)`、ordinal+shift — 全部按设计使用 `require=0`（依赖字节码布局）

### ModelBlockRendererMixin
目标为 `ModelBlockRenderer.renderModel()` 在 HEAD 处。使用 `-1` 作为颜色（无色）。

### LevelRendererMixin
- HEAD/RETURN：`renderLevel`，参数为 `(GraphicsResourceAllocator, DeltaTracker, boolean, Camera, GameRenderer, Matrix4f, Matrix4f, CallbackInfo)`
- drawCoreBuffers/endOutlineBatches：`method_62214`，14 个帧图参数

### ModelPartMixin
- 仅拦截 `compile()`（不拦截 `render()`）
- 检查 `isRenderingLevel()` 且不是 `isRenderingHand()`
- 使用 `doRender()` 模式进行包装链委托
