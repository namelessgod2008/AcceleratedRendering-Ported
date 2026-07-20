# AcceleratedRendering 1.21.4 Fabric — Feature Status

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
| Xaero+Tweakeroo NPE 修复 | `compat/xaero/` 模块：`@ModifyVariable` on `getBuffer(RenderType)`，null→`RenderType.gui()`（拦截点位于 `@WrapMethod` 覆盖不到的链路尽头） | 灵魂出窍 + Xaero's mods 稳定运行 |

---


## ✅ Implemented & Working

| Feature | Mixin(s) | Notes |
|---------|----------|-------|
| Entity model part acceleration | `ModelPartMixin` | `compile()` only; `isRenderingLevel()` guard |
| Entity shadow acceleration | `EntityRenderDispatcherMixin` | Color fix: ABGR conversion via `FastColorCompat` |
| Item acceleration (world) | `ItemRendererMixin` | `remap=true` via refMap on `renderModelLists`; `renderItem` uses default `remap=true` |
| Block acceleration | `ModelBlockRendererMixin` | Uses `-1` color (no tint) |
| Block acceleration (with tint) | `ModelBlockRendererMixin` | Pre-tinted BakedQuad vertex data from 1.21.4 TintSource; `-1` avoids double-tinting |
| BakedModel/BakedQuad/SimpleBakedModel | `models.BakedModelMixin`, `models.BakedQuadMixin`, `models.SimpleBakedModelMixin` | STRIDE fix + color pass-through |
| Multipart model acceleration | `models.MultipartBakedModelMixin` | Lazy Boolean cache + instanceof checks; no constructor injection |
| Weighted model acceleration | `models.WeightedBakedModelMixin` | `SimpleWeightedRandomList` + `unwrap()` + `getRandomValue()` |
| Item tinting | `TintLayerColors` + `ItemRendererMixin` | `TintLayerColors(tintLayers)` from 1.21.4 TintSource |
| Text acceleration (glyph/font) | `BakedGlyphMixin`, `FontMixin`, `ClientLanguageMixin` | renderText descriptors fixed (+Z bidirectional); BakedGlyph param semantics corrected (color/bold/packedLight) |
| String render output | `StringRenderOutputMixin` | Updated to 1.21.4 packed color, drawShadow, no dimFactor |
| Core buffer pipeline | `BufferBuilderMixin`, `BufferSourceMixin`, `SheetedDecalTextureGeneratorMixin`, etc. | `beginTransform`/`endTransform` delegation fixed |
| LevelRenderer hooks | `LevelRendererMixin` | HEAD/RETURN on `renderLevel`; draw buffers on `method_62214` |
| Hand rendering (vanilla) | `GameRendererMixin` | `require=0` — when target matches, sets hand flag |
| Hand item acceleration | `ItemRendererMixin`, `GameRendererMixin` | `isRenderingHand()` guard; POS_TEX_COLOR drawn before modelView popMatrix; `shouldAccelerateInHand()` config gate |
| Vanilla render layer fix | `LivingEntityRendererMixin`, `HumanoidArmorLayerMixin` | 1.21.4: render state API; `EquipmentLayerRenderer.renderLayers` replaces removed `renderTrim` |
| Entity filter | `filter.LevelRendererMixin` | Targets `renderEntities` (private); `renderEntity` still exists with same signature |
| Block entity filter | `filter.BlockEntityRenderDispatcherMixin` | HEAD/RETURN on `render(E, float, PoseStack, Buffer)` with @Share |
| Container filter | `filter.AbstractContainerScreenMixin` | No API changes needed; was blocked by missing `filter.mixins.json` entry in `fabric.mod.json` |
| Inventory entity rendering | `InventoryScreenMixin` | `method_64045` replaces removed `method_29977`; `remap=false` |
| GUI batching (fill/blit/slot) | `gui.GuiGraphicsMixin`, `gui.AbstractContainerScreenMixin`, `gui.GuiMixin` | `flushBatching()` excludes ENTITY/BLOCK; `innerBlit` updated; `Lighting.setupFor3DItems()` restored; `GuiMixin` scoped to `renderItemHotbar` only |
| GUI item batching | `GuiBatchingController` | `ItemStackRenderState.render()` + `ItemModelResolver.updateForTopItem()` replaces removed `ItemRenderer.render()` |
| GUI font/string batching | `gui.FontMixin` | `context.drawString()` → `font.drawInBatch()` (10-param); re-enabled |
| GUI slot highlight batching | `gui.AbstractContainerScreenMixin` | `@WrapOperation` on `blitSprite`; `submitBlit()` with `renderTypeGetter`; AW: sprites+getSprite |

---

## ⚠️ Partially Working (Functionality Degraded)

| Feature | Issue | Fix Difficulty |
|---------|-------|----------------|
| **Hand item acceleration** | ❌ Vanilla — verified full rendering chain (`ItemInHandRenderer` → `renderStatic` → `ItemStackRenderState` → `LayerRenderState` → `renderItem` → `renderModelLists`); matrix math theoretically cancels (`inverse(frustum)` in compute vs `frustum` in modelView) but stretching persists at `At.Shift.AFTER` injection. Needs runtime matrix-value debugging. | `ItemRendererMixin`, `GameRendererMixin` | 🔴 High |

---

## ❌ Not Implemented

### Core Features

| Feature | Reason | Fix Difficulty |
|---------|--------|----------------|
| ~~String render output~~ | ✅ Fixed — `r/g/b/a`→packed `color`, `dropShadow`→`drawShadow`, `dimFactor` removed, `finish(float)`; re-enabled | — |
| **Particle engine compat** | Removed — pausing accelerated pipelines during particle rendering harms performance more than it helps | — |

### Mod Compatibility

| Mod | Reason | Fix Difficulty |
|-----|--------|----------------|
| **Iris Shaders** | ✅ Iris 1.8.8 compat: `vanilla.LevelRendererMixin` updated `renderLevel`→`method_62214`. Needs runtime verification. | — |
| **ImmediatelyFast** | IF 1.3.4+1.21.4 available but mixins target ImmediatelyFast internals | 🟡 Medium |
| **Create** | All code commented out; needs `PartialItemModelRenderer` API update | 🟡 Medium |
| **Entity Model Features (EMF)** | `EMFModelPartMixin.java.disabled`; API not verified | 🟢 Low |
| **GeckoLib** | Geckolib 4.8 available but API not verified | 🟢 Low |
| **Touhou Little Maid (TLM)** | TLM 1.21.4 compatibility unknown | 🟢 Low |
| **FTB Library** | FTB Library 1.21.4 compatibility unknown | 🟢 Low |
| **Trinkets** | Original `dev.emi:trinkets` has no 1.21.4 release; `trinkets-canary` fork available but API not verified | 🟢 Low |
| **TweakMore** | Tweakmore 1.21.4 compatibility unknown | 🟢 Low |
| **Sophisticated Core** | All code commented out | 🟡 Medium |
| **ModernUI** | ✅ ModernUI 3.12.0.3 compat: `drawText` ordinals + `@Local(index)`, `drawUnderline`/`drawStrikethrough` +boolean, `isSdf` in handler/context. **编译依赖锁定 3.12.0.3**（3.13.x breaking change） |
| **Iris Shaders** | ✅ Iris 1.8.8 compat: `vanilla.LevelRendererMixin`→`method_62214`; injection points verified against bytecode. Needs runtime verification. | — |
| **Mixin remapping** | ✅ `fabric-loom-remap` 静态重映射注解为 intermediary；refMap 不再生成/不再需要（18 个 JSON 中 `"refmap"` 条目为无害残留，可留可删） | — |

### Completely Removed (1.21.4 Incompatible — Won't Fix)

| Feature | Reason |
|---------|--------|
| `ItemColors` accessor | `ItemColors` class removed in 1.21.4; replaced by data-driven `TintSource` |
| `MCItemColorsAccessor` | `Minecraft.getItemColors()` removed |
| `compatibility.WindowMixin` | Superseded by `core/mixins/WindowMixin.java` |
| `compatibility.ParticleEngineMixin` | Pausing acceleration during particles degrades performance; file kept on disk but removed from JSON |

---

## 🔴 Known Bugs (Pre-existing) — All Fixed

| Bug | Symptom | Resolution |
|-----|---------|------------|
| **GUI batching NPE with Tweakeroo Free Camera + Xaero's mods** | `fillDrawContexts` renderType=null in `flushBatching` during soul-out transition with Xaero's Minimap/WorldMap installed | ✅ Fixed: `compat/xaero/mixins/XaeroGuiGraphicsMixin` — `@ModifyVariable` on `getBuffer(RenderType)` replaces null with `RenderType.gui()` |

---

## Quick Wins (Recommended Next Steps)

1. 🟢 **GeckoLib compat** — Verify Geckolib 4.8 API, re-enable JSON
2. 🟢 **EMF compat** — Rename `.disabled` file, verify API
3. 🟢 **Trinkets compat** — Switch to trinkets-canary, verify API
4. 🟢 **FTB Library compat** — Verify API
5. 🟡 **Iris compat** — Runtime test with Iris 1.8.8 installed
