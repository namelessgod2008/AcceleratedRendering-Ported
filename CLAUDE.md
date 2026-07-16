# AcceleratedRendering-reFabricated — Project Architecture & Migration Notes

> ⚠️ **重要：在用户经过游戏测试并确认功能正常前，禁止修改 CLAUDE.md、memory/*.md 和 TODO.md。只允许修改源代码（src/）。**

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
| `ItemRenderer.render()` | Removed — replaced by `renderItem()` + frame graph | `remap=false` only on private `renderModelLists()`; `renderItem` uses default `remap=true` |
| `GuiGraphics.innerBlit()` | First param changed to `Function<ResourceLocation,RenderType>` | Disabled |
| `WeightedBakedModel.list` | → `SimpleWeightedRandomList<BakedModel>` | Fixed: `unwrap()` + `getRandomValue()` |
| `MultipartBakedModel` | `@Shadow` fields changed; constructor injection breaks baking | Fixed: lazy Boolean cache + `instanceof` checks |

---

## Disabled Features & Status

| Feature | Status | Reason / TODO |
|---------|--------|---------------|
| **Item/block acceleration** | ✅ Working | `ItemRendererMixin` (public `renderItem` remap=true, private `renderModelLists` remap=false, no require=0) + `ModelBlockRendererMixin` + `SimpleBakedModelMixin` with color/stride fixes |
| **Entity model acceleration** | ✅ Working | `ModelPartMixin.compile()` with `isRenderingLevel()` check |
| **Entity shadows** | ✅ Working | Color conversion fix |
| **Text acceleration** | ✅ Working | BakedGlyph render param semantics fixed (color/bold/packedLight); FontMixin renderText descriptors fixed (+Z bidirectional); drawInBatch8xOutline require=0 by design |
| **Multipart Baked Model** | ✅ Working | Lazy Boolean cache + instanceof checks; no constructor injection |
| **Weighted Baked Model** | ✅ Working | `SimpleWeightedRandomList<BakedModel>` + `unwrap()` + `getRandomValue()` |
| **Item tinting (color)** | ✅ Working | `TintLayerColors(tintLayers)` from 1.21.4 TintSource; accelerated model path skips when tint layers present |
| **StringRenderOutput** | ✅ Working | Updated to 1.21.4: `r/g/b/a`→packed `color`, `dropShadow`→`drawShadow`, `dimFactor` removed, `finish(float)` no backgroundColor |
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
| **ModernUI compat** | ✅ Working | Updated for ModernUI 3.12.0: `drawText` ordinals + `@Local(index)`, `drawUnderline`/`drawStrikethrough` signature + boolean param |

---

## Mixin Method Reference

### ItemRendererMixin (1.21.4)
Targets `ItemRenderer.renderItem()` wrapping call to `renderModelLists()`:
- `renderItem` is `public static` in 1.21.4 — uses default `remap=true` (public methods get intermediary-mapped)
- `renderModelLists` is `private static` — uses `remap=false` (private methods keep Mojang names)
- Sig: `renderModelLists(BakedModel, int[], int, int, PoseStack, VertexConsumer)`
- Handler: `(BakedModel, int[], int, int, PoseStack, VertexConsumer, Operation<Void>)` — NO ItemRenderer instance
- Uses `instanceof IAcceleratedBakedModel` check before casting
- Only accelerates when `CoreFeature.isRenderingLevel()` is true
- No `require=0` — this is a core feature, failures must be loud

### BakedGlyphMixin (1.21.4)
- `render(boolean, float, float, Matrix4f, VertexConsumer, int, boolean, int)` = `(italic, x, y, matrix, buffer, color, bold, packedLight)`
- Parameters 6-8 are `(int color, boolean bold, int packedLight)` — NOT `(int packedLight, boolean dropShadow, int color)`
- Private method → no explicit `remap` needed (intermediary doesn't map private methods)

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
