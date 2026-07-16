# AcceleratedRendering 1.21.4 Fabric — Feature Status

## ✅ Implemented & Working

| Feature | Mixin(s) | Notes |
|---------|----------|-------|
| Entity model part acceleration | `ModelPartMixin` | `compile()` only; `isRenderingLevel()` guard |
| Entity shadow acceleration | `EntityRenderDispatcherMixin` | Color fix: ABGR conversion via `FastColorCompat` |
| Item acceleration (world) | `ItemRendererMixin` | `remap=false` on private `renderModelLists` only; `require=0` removed; `public renderItem` uses default `remap=true` |
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
| **ModernUI** | ✅ ModernUI 3.12.0 compat: updated `drawText` ordinals + `@Local(index)`, `drawUnderline`/`drawStrikethrough` +boolean | — |
| **Iris Shaders** | ✅ Iris 1.8.8 compat: `vanilla.LevelRendererMixin`→`method_62214`; injection points verified against bytecode. Needs runtime verification. | — |

### Completely Removed (1.21.4 Incompatible — Won't Fix)

| Feature | Reason |
|---------|--------|
| `ItemColors` accessor | `ItemColors` class removed in 1.21.4; replaced by data-driven `TintSource` |
| `MCItemColorsAccessor` | `Minecraft.getItemColors()` removed |
| `compatibility.WindowMixin` | Superseded by `core/mixins/WindowMixin.java` |
| `compatibility.ParticleEngineMixin` | Pausing acceleration during particles degrades performance; file kept on disk but removed from JSON |

---

## Quick Wins (Recommended Next Steps)

1. 🟢 **GeckoLib compat** — Verify Geckolib 4.8 API, re-enable JSON
2. 🟢 **EMF compat** — Rename `.disabled` file, verify API
3. 🟢 **Trinkets compat** — Switch to trinkets-canary, verify API
4. 🟢 **FTB Library compat** — Verify API
5. 🟡 **Iris compat** — Runtime test with Iris 1.8.8 installed
