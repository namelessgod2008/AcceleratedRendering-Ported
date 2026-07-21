# AcceleratedRendering 1.21.4 Fabric — 功能状态

## 🔧 2026-07-18 基础设施变更（AcceleratedRendering-Ported 本地副本）

> 以下为移植副本的基础设施修复，**功能状态表尚未经此副本的游戏回归测试确认**。

| 变更 | 内容 | 验证 |
|------|------|------|
| 包全量重命名 | `com.github.argon4w.acceleratedrendering` → `com.namelessgod2008`（374 文件 1278 处：package 行、import、Iris compat 的 `Lcom/...` 描述符、mixin JSON `package`/`plugin`、fabric.mod.json 入口点） | 0 mismatch / 0 残留 |
| assets 命名空间 | `assets/acceleratedrendering-ported` → `assets/acceleratedrendering`（必须 = MOD_ID，否则 `Cannot found compute shader` 崩溃） | 40 个 .compute 路径逐一核对 |
| processResources | `expand replaceProperties`（修复 `Missing property (mod_id)`） | fabric.mod.json 占位符展开正确 |
| 依赖修复（用户完成） | `fabric-loom-remap` 1.17-SNAPSHOT、Loader 0.19.3、Parchment 2025.03.23、Iris 1.8.8 等 | `compileJava` 0 错误 |
| refMap 机制废弃 | `useLegacyMixinAp` 移除；remapJar 静态重映射 mixin 注解（实证：`method_22702`/`method_23182` 已写入注解）；JSON 中 refmap 条目为无害残留 | 反编译产物 jar 验证 |
| 文本加速 bug 修复（4 环） | ① `drawCoreBuffers` ordinal 0→1（端末批次在方块实体之后）；② `FastColorCompat` sign 改 alpha-first；③ `StringRenderOutputMixin` 11 参构造注入 + setup() alpha 修复；④ `ComponentMesh` Y 坐标修正 | 告示牌普通/荧光字 + ModernUI 发光字体全部恢复 |
| Xaero+Tweakeroo NPE 修复 | `compat/xaero/` 模块：`@ModifyVariable` on `getBuffer(RenderType)`，null→`RenderType.gui()` | 灵魂出窍 + Xaero's mods 稳定运行 |
| 灵魂出窍 HUD 文字消失 **修复** | `GuiMixin`: `startBatching` 注入点从 HEAD→方法体内 `getCameraPlayer()` INVOKE AFTER；若方法体被取消则永不触发 | ✅ 根因修复（2026-07-21），移除 `cameraEntity` workaround |

---


## ✅ 已实现并正常工作

| 功能 | Mixin | 备注 |
|---------|----------|-------|
| 实体模型部件加速 | `ModelPartMixin` | 仅 `compile()`；`isRenderingLevel()` 守卫 |
| 实体阴影加速 | `EntityRenderDispatcherMixin` | 颜色修复：通过 `FastColorCompat` 进行 ABGR 转换 |
| 物品加速 (世界) | `ItemRendererMixin` | `renderModelLists` 通过 refMap 使用 `remap=true`；`renderItem` 使用默认 `remap=true` |
| 方块加速 | `ModelBlockRendererMixin` | 使用 `-1` 颜色（无着色） |
| 方块加速 (含着色) | `ModelBlockRendererMixin` | 1.21.4 TintSource 预着色 BakedQuad 顶点数据；`-1` 避免重复着色 |
| BakedModel/BakedQuad/SimpleBakedModel | `models.BakedModelMixin`、`models.BakedQuadMixin`、`models.SimpleBakedModelMixin` | STRIDE 修复 + 颜色直通 |
| Multipart 模型加速 | `models.MultipartBakedModelMixin` | 惰性 Boolean 缓存 + instanceof 检查；无构造注入 |
| Weighted 模型加速 | `models.WeightedBakedModelMixin` | `SimpleWeightedRandomList` + `unwrap()` + `getRandomValue()` |
| 物品着色 | `TintLayerColors` + `ItemRendererMixin` | `TintLayerColors(tintLayers)` 来自 1.21.4 TintSource |
| 文本加速 (字形/字体) | `BakedGlyphMixin`、`FontMixin`、`ClientLanguageMixin` | renderText 描述符已修复 (+Z bidirectional)；BakedGlyph 参数语义已修正 (color/bold/packedLight) |
| 字符串渲染输出 | `StringRenderOutputMixin` | 已更新至 1.21.4：packed color、drawShadow、无 dimFactor |
| 核心缓冲区管线 | `BufferBuilderMixin`、`BufferSourceMixin`、`SheetedDecalTextureGeneratorMixin` 等 | `beginTransform`/`endTransform` 委托已修复 |
| LevelRenderer 钩子 | `LevelRendererMixin` | `renderLevel` 上 HEAD/RETURN；在 `method_62214` 上绘制缓冲区 |
| 手部渲染 (原版) | `GameRendererMixin` | `require=0` — 当目标匹配时设置手部标志 |
| 手部物品加速 | `ItemRendererMixin`、`GameRendererMixin` | `isRenderingHand()` 守卫；在 modelView popMatrix 之前绘制 POS_TEX_COLOR；`shouldAccelerateInHand()` 配置开关 |
| Vanilla 渲染层修复 | `LivingEntityRendererMixin`、`HumanoidArmorLayerMixin` | 1.21.4: 渲染状态 API；`EquipmentLayerRenderer.renderLayers` 替换已移除的 `renderTrim` |
| 实体过滤 | `filter.LevelRendererMixin` | 目标 `renderEntities` (private)；`renderEntity` 仍以相同签名存在 |
| 方块实体过滤 | `filter.BlockEntityRenderDispatcherMixin` | `render(E, float, PoseStack, Buffer)` 上 HEAD/RETURN，带 @Share |
| 容器过滤 | `filter.AbstractContainerScreenMixin` | 无需 API 变更；此前被 `fabric.mod.json` 中缺失 `filter.mixins.json` 条目所阻塞 |
| 物品栏实体渲染 | `InventoryScreenMixin` | `method_64045` 替换已移除的 `method_29977`；`remap=false` |
| GUI 批处理 (fill/blit/slot) | `gui.GuiGraphicsMixin`、`gui.AbstractContainerScreenMixin`、`gui.GuiMixin` | `flushBatching()` 排除 ENTITY/BLOCK；`innerBlit` 已更新；`Lighting.setupFor3DItems()` 已恢复；`GuiMixin` 仅作用于 `renderItemHotbar` |
| GUI 物品批处理 | `GuiBatchingController` | `ItemStackRenderState.render()` + `ItemModelResolver.updateForTopItem()` 替换已移除的 `ItemRenderer.render()` |
| GUI 字体/字符串批处理 | `gui.FontMixin` | `context.drawString()` → `font.drawInBatch()` (10 参)；已重新启用 |
| GUI 槽位高亮批处理 | `gui.AbstractContainerScreenMixin` | `@WrapOperation` on `blitSprite`；`submitBlit()` 带 `renderTypeGetter`；AW: sprites+getSprite |
| 灵魂出窍 HUD 文字消失 | `gui.GuiMixin` | ✅ 已修复 (2026-07-21)：`startBatching` 注入点从 HEAD→方法体内 `getCameraPlayer()` INVOKE AFTER |

---

## ⚠️ 部分实现（功能降级）

| 功能 | 问题 | 涉及文件 | 修复难度 |
|---------|-------|----------------|------|
| **手部物品加速** | ❌ 回退至原版 — 已验证完整渲染链（`ItemInHandRenderer` → `renderStatic` → `ItemStackRenderState` → `LayerRenderState` → `renderItem` → `renderModelLists`）；矩阵数学理论上可抵消（计算着色器中 `inverse(frustum)` 与 modelView 中 `frustum`），但在 `At.Shift.AFTER` 注入点仍有拉伸。需要运行时矩阵值调试。 | `ItemRendererMixin`、`GameRendererMixin` | 🔴 高 |

---

## ❌ 未实现

### 核心功能

| 功能 | 原因 | 修复难度 |
|---------|--------|----------------|
| ~~字符串渲染输出~~ | ✅ 已修复 — `r/g/b/a`→packed `color`，`dropShadow`→`drawShadow`，`dimFactor` 已移除，`finish(float)`；已重新启用 | — |
| **粒子引擎兼容** | 已移除 — 在粒子渲染期间暂停加速管线对性能的损害大于其带来的好处 | — |

### Mod 兼容性

| Mod | 原因 | 修复难度 |
|-----|--------|----------------|
| **Iris Shaders** | ✅ Iris 1.8.8 兼容：`vanilla.LevelRendererMixin` 已更新 `renderLevel`→`method_62214`。需要运行时验证。 | — |
| **ImmediatelyFast** | IF 1.3.4+1.21.4 可用，但 mixin 目标指向 ImmediatelyFast 内部 | 🟡 中等 |
| **Create** | 所有代码已注释；需要 `PartialItemModelRenderer` API 更新 | 🟡 中等 |
| **Entity Model Features (EMF)** | `EMFModelPartMixin.java.disabled`；API 未验证 | 🟢 低 |
| **GeckoLib** | Geckolib 4.8 可用，但 API 未验证 | 🟢 低 |
| **Touhou Little Maid (TLM)** | TLM 1.21.4 兼容性未知 | 🟢 低 |
| **FTB Library** | FTB Library 1.21.4 兼容性未知 | 🟢 低 |
| **Trinkets** | 原 `dev.emi:trinkets` 无 1.21.4 版本；`trinkets-canary` fork 可用，但 API 未验证 | 🟢 低 |
| **TweakMore** | Tweakmore 1.21.4 兼容性未知 | 🟢 低 |
| **Sophisticated Core** | 所有代码已注释 | 🟡 中等 |
| **ModernUI** | ✅ ModernUI 3.12.0.3 兼容：`drawText` ordinals + `@Local(index)`、`drawUnderline`/`drawStrikethrough` +boolean、handler/context 中 `isSdf`。**编译依赖锁定 3.12.0.3**（3.13.x breaking change） |
| **Iris Shaders** | ✅ Iris 1.8.8 兼容：`vanilla.LevelRendererMixin`→`method_62214`；注入点已对照字节码验证。需要运行时验证。 | — |
| **Mixin 重映射** | ✅ `fabric-loom-remap` 静态重映射注解为 intermediary；refMap 不再生成/不再需要（18 个 JSON 中 `"refmap"` 条目为无害残留，可留可删） | — |

### 已完全移除（1.21.4 不兼容 — 不再修复）

| 功能 | 原因 |
|---------|--------|
| `ItemColors` accessor | `ItemColors` 类在 1.21.4 中已移除；由数据驱动的 `TintSource` 替代 |
| `MCItemColorsAccessor` | `Minecraft.getItemColors()` 已移除 |
| `compatibility.WindowMixin` | 被 `core/mixins/WindowMixin.java` 取代 |
| `compatibility.ParticleEngineMixin` | 在粒子渲染期间暂停加速会降低性能；文件保留在磁盘上但已从 JSON 中移除 |

---

## 🔴 已知 Bug

| Bug | 症状 | 状态 |
|-----|---------|--------|
| **Tweakeroo 灵魂出窍 + Xaero's mods 时 GUI 批处理 NPE** | 灵魂出窍过渡期间 `flushBatching` 中 `fillDrawContexts` renderType=null | ✅ 已修复：`compat/xaero/mixins/XaeroGuiGraphicsMixin` |
| **灵魂出窍时 HUD overlay 文字全局消失** | Free Camera 激活时 MiniHUD/Xaero/告示牌/F3/ESC 所有文字消失 | ✅ 已修复 (2026-07-21)：`GuiMixin` 注入点从 HEAD→方法体内 `getCameraPlayer()` INVOKE AFTER。根因：Mixin HEAD 回调全部执行完毕后才会检查 `ci.cancel()`，AR 的 `startBatching` 在 `ci.cancel()` 之前已执行。 |

---

## 快速见效项（建议后续步骤）

1. 🟢 **GeckoLib 兼容** — 验证 Geckolib 4.8 API，重新启用 JSON
2. 🟢 **EMF 兼容** — 重命名 `.disabled` 文件，验证 API
3. 🟢 **Trinkets 兼容** — 切换至 trinkets-canary，验证 API
4. 🟢 **FTB Library 兼容** — 验证 API
5. 🟡 **Iris 兼容** — 安装 Iris 1.8.8 进行运行时测试
