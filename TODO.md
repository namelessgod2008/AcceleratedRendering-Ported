# AcceleratedRendering 1.21.4 Fabric — Feature Status

## ✅ Implemented & Working

| Feature | Mixin(s) | Notes |
|---------|----------|-------|
| Entity model part acceleration | `ModelPartMixin` | `compile()` only; `isRenderingLevel()` guard |
| Entity shadow acceleration | `EntityRenderDispatcherMixin` | Color fix: ABGR conversion via `FastColorCompat` |
| Item acceleration (world) | `ItemRendererMixin` | `remap=false` target `renderModelLists`; `instanceof` check |
| Block acceleration | `ModelBlockRendererMixin` | Uses `-1` color (no tint) |
| BakedModel/BakedQuad/SimpleBakedModel | `models.BakedModelMixin`, `models.BakedQuadMixin`, `models.SimpleBakedModelMixin` | STRIDE fix + color pass-through |
| Text acceleration (glyph/font) | `BakedGlyphMixin`, `FontMixin`, `ClientLanguageMixin`, etc. | 1.21.4 signatures updated |
| Core buffer pipeline | `BufferBuilderMixin`, `BufferSourceMixin`, `SheetedDecalTextureGeneratorMixin`, etc. | `beginTransform`/`endTransform` delegation fixed |
| LevelRenderer hooks | `LevelRendererMixin` | HEAD/RETURN on `renderLevel`; draw buffers on `method_62214` |
| Hand rendering (vanilla) | `GameRendererMixin` | `require=0` — when target matches, sets hand flag |

---

## ⚠️ Partially Working (Functionality Degraded)

| Feature | Issue | Affected Mixins | Fix Difficulty |
|---------|-------|-----------------|----------------|
| **Block tinting** | Blocks rendered without biome-colored tints (grass, leaves, etc.) | `ModelBlockRendererMixin` | 🟢 Easy — fix `colorFromFloat` parameter order |
| **Item tinting** | Items rendered without color tints (potions, spawn eggs, etc.) | `ItemLayerColors` | 🟢 Easy — migrate to `TintSource` API |
| **Multipart models** | Fences, walls, fire, etc. not accelerated | `models.MultipartBakedModelMixin` | 🟡 Medium — verify 1.21.4 class structure, fix `@Shadow` fields |
| **Weighted models** | Weighted variants not accelerated | `models.WeightedBakedModelMixin` | 🟡 Medium — fix `@Shadow` field `list` (renamed in 1.21.4) |
| **Particle rendering** | Accelerated pipelines not paused during particle rendering | `compatibility.ParticleEngineMixin` | 🟡 Medium — update `render()` method signature |
| **Hand item acceleration** | Items in hand use vanilla rendering (not accelerated) | `ItemRendererMixin` | 🟡 Medium — needs `isRenderingHand()` support + proper buffer draw timing |

---

## ❌ Not Implemented

### Core Features

| Feature | Reason | Affected Mixins | Fix Difficulty |
|---------|--------|-----------------|----------------|
| **GUI batching** (fill/blit/item) | `GuiGraphics` API changed: `bufferSource`→private, `innerBlit` signature changed, `renderItem` signature changed | `gui.GuiGraphicsMixin`, `gui.GuiMixin` | 🔴 High — complete rewrite needed |
| **String render output** | `Font.StringRenderOutput` internal fields completely refactored (r/g/b/a → packed color, dropShadow removed) | `StringRenderOutputMixin` | 🔴 High — complete rewrite needed |
| **Inventory entity rendering** | `InventoryScreen.method_29977` replaced in 1.21.4 entity render state refactor | `InventoryScreenMixin` | 🔴 High — need to find new injection point |
| **Vanilla render layer fix** | `LivingEntityRenderer.render(LivingEntity,...)` → `render(S state,...)`; `HumanoidArmorLayer` trim rendering refactored | `LivingEntityRendererMixin`, `HumanoidArmorLayerMixin` | 🔴 High — entity render state system |
| **Block entity filter** | `BlockEntityRenderDispatcher.tryRender` renamed in 1.21.4 | `BlockEntityRenderDispatcherMixin` | 🟡 Medium |
| **Entity filter** | `LevelRenderer.renderEntity` signature may have changed | `filter.mixins.json:LevelRendererMixin` | 🟡 Medium |
| **Container filter** | `AbstractContainerScreen.render` changed | `filter.mixins.json:AbstractContainerScreenMixin` | 🟡 Medium |

### Mod Compatibility

| Mod | Reason | Affected Mixins | Fix Difficulty |
|-----|--------|-----------------|----------------|
| **Iris Shaders** | Iris 1.8.5+1.21.4 available but compat not validated; frame graph API changes | `compat.iris.mixins.json` (19 mixins) | 🔴 High — many @Pseudo targets; needs runtime verification |
| **ImmediatelyFast** | IF 1.3.4+1.21.4 available but mixins target ImmediatelyFast internals (`BatchableBufferSource`, `BatchingBuffers`, `WrappedRenderLayer`) | `compat.immediatelyfast.mixins.json` (3 mixins) | 🟡 Medium |
| **Create** | All code commented out; needs `PartialItemModelRenderer` API update | `PartialItemModelRendererMixin` | 🟡 Medium |
| **Entity Model Features (EMF)** | `EMFModelPartMixin.java.disabled`; EMF 3.2.4+1.21.4 available but API not verified | `feature.entitymodelfeature.mixins.json` (5 mixins) | 🟢 Low — rename file, verify API |
| **GeckoLib** | Geckolib 4.8 available but API not verified | `feature.geckolib.mixins.json` (2 mixins) | 🟢 Low — verify API |
| **Touhou Little Maid (TLM)** | TLM 1.21.4 compatibility unknown | `feature.touhoulittlemaid.mixins.json` (2 mixins) | 🟢 Low — verify API |
| **FTB Library** | FTB Library 1.21.4 compatibility unknown | `feature.ftb.mixins.json` (1 mixin) | 🟢 Low — verify API |
| **Trinkets** | Original `dev.emi:trinkets` has no 1.21.4 release; `trinkets-canary` fork available but API not verified | `compat.trinkets.mixins.json` (1 mixin) | 🟢 Low — switch dep, verify API |
| **TweakMore** | Tweakmore 1.21.4 compatibility unknown | `compat.tweakmore.mixins.json` (1 mixin) | 🟢 Low — verify API |
| **Sophisticated Core** | All code commented out | `compat.sophisticated.mixins.json` (1 mixin) | 🟡 Medium |
| **ModernUI** | ModernUI 4.x available; @Pseudo mixins should work but untested | `feature.modernui.mixins.json` (5 mixins) | 🟢 Low — test with ModernUI |

### Completely Removed (1.21.4 Incompatible — Won't Fix)

| Feature | Reason |
|---------|--------|
| `ItemColors` accessor | `ItemColors` class removed in 1.21.4; replaced by data-driven `TintSource` |
| `MCItemColorsAccessor` | `Minecraft.getItemColors()` removed |
| `compatibility.WindowMixin` | Superseded by `core/mixins/WindowMixin.java` |

---

## Difficulty Legend

| Level | Meaning |
|-------|---------|
| 🟢 Low | Simple API verification or configuration change; <1 hour |
| 🟡 Medium | Method signature update or moderate refactoring; 2-4 hours |
| 🔴 High | Complete rewrite needed; 1+ days |

---

## Quick Wins (Recommended Next Steps)

1. 🟢 **Block tinting** — Fix `colorFromFloat` parameter order in `ModelBlockRendererMixin`
2. 🟢 **GeckoLib compat** — Verify Geckolib 4.8 API, re-enable JSON
3. 🟢 **EMF compat** — Rename `.disabled` file, verify API
4. 🟡 **Multipart/Weighted models** — Fix `@Shadow` fields, re-enable
5. 🟡 **ParticleEngine** — Update `render()` signature
6. 🟡 **Hand item acceleration** — Add `isRenderingHand()` support
7. 🔴 **GUI batching** — Major rewrite needed
8. 🔴 **Iris compat** — Test with Iris 1.8.5, fix broken mixins
