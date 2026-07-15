# AcceleratedRendering-reFabricated — Project Architecture & Migration Notes

## Project Overview
- **Repository**: `ZhuRuoLing/AcceleratedRendering-reFabricated` (Fabric port)
- **Upstream**: `Argon4W/AcceleratedRendering` (NeoForge, original), `Luna5ama/AcceleratedRendering` (NeoForge 1.21.4-port)
- **Target**: Minecraft Fabric, client-side only
- **Purpose**: GPU compute-shader based entity model part rendering acceleration (vertex transform + mesh caching)
- **Java**: 21, **Loom**: 1.16.x, **Loader**: 0.16.x
- **Mappings**: Mojang official mappings + Parchment (NOT Yarn)

## Build System
- **Build tool**: Gradle + Fabric Loom
- **Key deps**: Fabric API, ForgeConfigAPIPort, NeoForge Event Bus (bundled), Lombok, MixinExtras
- **Access widener**: `src/main/resources/acceleratedrendering.accesswidener` (v2 named)
- **Mixin JSONs**: Listed in `fabric.mod.json` → `mixins` array
- **JDK**: Compile with JDK 21+, run with JDK 21 or 24 (JDK 25 removes `LambdaMetafactory`)

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
| `feature.text.mixins.json` | BakedGlyph, Font text acceleration | Active (StringRenderOutput disabled) |
| `feature.items.mixins.json` | Item/block rendering acceleration | Active (limited) |
| `compat.iris.mixins.json` | Iris shader compat | @Pseudo, optional |
| `compat.immediatelyfast.mixins.json` | ImmediatelyFast compat | @Pseudo, optional |
| `compat.modernui.mixins.json` | Modern UI compat | @Pseudo, optional |

---

## Critical Pitfalls (Migration & Development)

### 1. `doRender()` does NOT call `beginTransform`/`endTransform`
`AcceleratedBufferBuilder.doRender()` directly calls `renderer.render(this, ...)` — unlike Luna5ama's NeoForge version. Always call `beginTransform(transform, normal)` and `endTransform()` inside the `render()` callback.

### 2. Wrapper mixins MUST override `beginTransform`/`endTransform`
`SheetedDecalTextureGeneratorMixin`, `SpriteCoordinateExpanderMixin`, `EntityOutlineGeneratorMixin` must delegate these methods to `delegate.getAccelerated().beginTransform(...)` / `endTransform()`. Without this, the `IAcceleratedVertexConsumer` default (throw UnsupportedOperationException) is used.

### 3. `compile()` must use `doRender()` pattern — NOT direct `beginTransform`
In `ModelPartMixin.compile()`, call `extension.doRender(this, null, pPose.pose(), pPose.normal(), ...)` instead of `extension.beginTransform(...)`. The `doRender()` properly delegates through the wrapper chain, while `beginTransform()` on a wrapper throws UnsupportedOperationException.

### 4. `GameRendererMixin` must NOT draw entity buffers
Entity buffers must be drawn during world rendering (`method_62214`), NOT during hand rendering (`renderItemInHand`). Drawing in the wrong context applies wrong view-projection matrices → entities float/shift.

### 5. Rendering context checks are CRITICAL
Every accelerated mixin must check the rendering context:
- `ModelPartMixin`: only accelerate when `isRenderingLevel()` and NOT `isRenderingHand()`
- `ItemRendererMixin`: only accelerate when `isRenderingLevel()`
- Without these checks, acceleration activates during wrong rendering phases (GUI, hand, etc.) causing visual glitches or rendering failures.

### 6. `LevelRenderer.renderLevel` → frame graph in 1.21.4
In MC 1.21.4, `renderLevel` delegates rendering to frame graph lambdas (`method_62214` in Fabric intermediary). `endLastBatch()`/`endOutlineBatch()` calls are inside the lambda, NOT in `renderLevel` itself:
- `startRenderLevel`/`stopRenderLevel` → target `renderLevel` (HEAD/RETURN) with 8 params: `(GraphicsResourceAllocator, DeltaTracker, boolean, Camera, GameRenderer, Matrix4f, Matrix4f, CallbackInfo)`
- `drawCoreBuffers`/`endOutlineBatches` → target `method_62214` with 14 frame graph params

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

### 9. `FastColorCompat` utility
Replaces removed `net.minecraft.util.FastColor`. Key differences:
- `ARGB32.color(r, g, b, a)` → maps to `ARGB.color(a, r, g, b)` (alpha FIRST)
- `ARGB32.color(alpha, packedColor)` → extracts R/G/B and calls `ARGB.color(alpha, r, g, b)`
- `ARGB32.colorFromFloat(r, g, b, 1.0f)` → calls `ARGB.colorFromFloat(1.0f, r, g, b)` (alpha FIRST)
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

### 15. Tab-formatted files
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
| `BakedGlyph.render()` | 11 params → 9 params, r/g/b/a → packed color, shadowOffset → boolean | Update method signature |
| `BakedGlyph.Effect` | Now record: `(float,float,float,float,float,int)` | Use `effect.color()` |
| `Font.drawInBatch(String,...)` | Removed `bidirectional` param | Remove parameter |
| `Font.StringRenderOutput` | `dropShadow`, `dimFactor`, `r`, `g`, `b`, `a` fields removed | Removed from mixins JSON |
| `StringRenderOutput.finish(int,float)` | → `finish(float)` | Remove `background` parameter |
| `LevelRenderer.renderLevel()` | Frame graph refactor, `method_62214` for endBatch/endOutlineBatch | Split into HEAD/RETURN + INVOKE injections |
| `ModelPart$Polygon`/`Vertex` | Now records with methods | Added access widener for `pos`, `u`, `v`, `normal`, `vertices` |
| `ParticleEngine.render()` | `(LightTexture,Camera,float)` → `(Camera,float,BufferSource)` | Removed from mixins JSON |
| `ItemRenderer.render()` | Removed — replaced by `renderItem()` + frame graph | `remap=false` target on private `renderModelLists()` |
| `GuiGraphics.innerBlit()` | First param changed to `Function<ResourceLocation,RenderType>` | Disabled |
| `WeightedBakedModel.list` | Field renamed | Disabled |
| `MultipartBakedModel` | `@Shadow` fields changed | Disabled |

---

## Disabled Features & Status

| Feature | Status | Reason / TODO |
|---------|--------|---------------|
| **Item/block acceleration** | ✅ Working | `ItemRendererMixin` + `ModelBlockRendererMixin` + `SimpleBakedModelMixin` with color/stride fixes |
| **Entity model acceleration** | ✅ Working | `ModelPartMixin.compile()` with `isRenderingLevel()` check |
| **Entity shadows** | ✅ Working | Color conversion fix |
| **Text acceleration** | ✅ Working | BakedGlyph/Font signatures updated |
| **Multipart Baked Model** | ❌ Disabled | `@Shadow` fields changed in 1.21.4 |
| **Weighted Baked Model** | ❌ Disabled | `@Shadow` field `list` renamed |
| **StringRenderOutput** | ❌ Disabled | Internal fields completely refactored |
| **GUI batching** | ❌ Disabled | `GuiGraphics.bufferSource` became private |
| **Item tinting (color)** | ⚠️ No tint | `ItemLayerColors` returns -1; `ModelBlockRendererMixin` uses -1 |
| **Block entity filter** | ❌ Disabled | `tryRender` method renamed |
| **LivingEntityRenderer/HumanoidArmorLayer** | ❌ Disabled | Entity render state refactor |
| **Iris/ImmediatelyFast/ModernUI compat** | ⚠️ @Pseudo | Skip silently if mods not installed |

---

## Mixin Method Reference

### ItemRendererMixin (1.21.4)
Targets `ItemRenderer.renderItem()` (Mojang, remap=false) wrapping call to `renderModelLists()`:
- `renderItem` is a private static method in 1.21.4
- `renderModelLists` is now private static with `(BakedModel, int[], int, int, PoseStack, VertexConsumer)` params
- Handler: `(BakedModel, int[], int, int, PoseStack, VertexConsumer, Operation<Void>)` — NO ItemRenderer instance
- Uses `instanceof IAcceleratedBakedModel` check before casting
- Only accelerates when `CoreFeature.isRenderingLevel()` is true

### ModelBlockRendererMixin
Targets `ModelBlockRenderer.renderModel()` at HEAD. Uses `-1` for color (no tint).

### LevelRendererMixin
- HEAD/RETURN: `renderLevel` with `(GraphicsResourceAllocator, DeltaTracker, boolean, Camera, GameRenderer, Matrix4f, Matrix4f, CallbackInfo)`
- drawCoreBuffers/endOutlineBatches: `method_62214` with 14 frame graph params

### ModelPartMixin
- Only `compile()` (not `render()`) is intercepted
- Checks `isRenderingLevel()` AND not `isRenderingHand()`
- Uses `doRender()` pattern for wrapper chain delegation
