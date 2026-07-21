# AcceleratedRendering-Ported

**Minecraft Fabric 1.21.4 移植版** — GPU 计算着色器实体渲染加速模组。

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 📦 上游项目

本模组是以下项目的 Fabric 移植：

| 项目 | 作者 | 平台 | 版本 |
|------|------|------|------|
| [AcceleratedRendering](https://github.com/Argon4W/AcceleratedRendering) | **Argon4W** | NeoForge | 1.21.1 (原始) |
| [AcceleratedRendering-reFabricated](https://github.com/ZhuRuoLing/AcceleratedRendering-reFabricated) | **ZhuRuoLing** | Fabric | 1.21.1 (Fabric 移植) |
| [AcceleratedRendering (1.21.4-port)](https://github.com/Luna5ama/AcceleratedRendering) | **Luna5ama** | NeoForge | 1.21.4 (NeoForge 移植参考) |

本版本（**AcceleratedRendering-Ported**，维护者 namelessgod2008）基于 ZhuRuoLing 的 Fabric 1.21.1 移植，参照 Luna5ama 的 NeoForge 1.21.4 移植，更新至 **Minecraft 1.21.4 Fabric**。Java 包名已迁移至 `com.namelessgod2008`（mod id 仍为 `acceleratedrendering`）。

---

## 🚀 功能状态

### ✅ 已实现并正常工作

| 功能 | 说明 |
|------|------|
| 实体模型加速 | `ModelPart` compile 加速，GPU 计算着色器并行变换 |
| 实体阴影加速 | 加速方块阴影渲染 |
| 物品加速 (世界) | 掉落物、手持物品加速渲染 |
| 方块加速 | 方块模型渲染加速 (含着色) |
| 文本加速 | `BakedGlyph`/`Font`/`StringRenderOutput` 加速渲染；告示牌文字已修复（2026-07-18: `endLastBatch` ordinal + `FastColorCompat` sign + 11 参构造注入） |
| 模型加速 | Multipart、Weighted、SimpleBakedModel (1.21.4 API 适配) |
| 核心管线 | 缓冲区管理、LevelRenderer 帧图钩子、wrapper 链 |
| 实体/方块实体/容器过滤 | FilterFeature — 可选禁用特定类型加速 |
| Vanilla 渲染层修复 | `LivingEntityRenderer` 渲染状态 API + `HumanoidArmorLayer` 装备渲染 |
| 物品栏实体渲染 | `InventoryScreen` entity rendering (method_64045) |
| GUI 批处理 (fill/blit/slot) | 容器界面填充/位块传输/槽位批处理，热键栏物品批处理 |
| GUI 物品/字体/高亮批处理 | `ItemStackRenderState` + `font.drawInBatch` + `submitBlit` with renderTypeGetter |
| ModernUI 兼容 | ModernUI 3.12.0.3 适配（**编译依赖必须与运行时匹配**） |
| Mixin 重映射 | `fabric-loom-remap` 静态重映射注解为 intermediary（refMap 已废弃） |
| Xaero 兼容 | `compat/xaero/` — Tweakeroo 灵魂出窍 NPE 防御（2026-07-18） |
| 灵魂出窍 HUD 文字消失 | ✅ 已修复 (2026-07-21): `GuiMixin` 注入点从 HEAD→方法体内 INVOKE AFTER，方法体被取消时永不触发 |

### ⚠️ 部分实现 / 待验证

| 功能 | 状态 |
|------|------|
| 手部物品加速 | ❌ 不可行 — 坐标空间不匹配（见 CLAUDE.md #4b） |
| Iris 兼容 | ✅ 注入点已验证（method_62214），🟡 需运行时验证 |
| ImmediatelyFast 兼容 | ⚠️ @Pseudo — 未验证 |
| 其他 mod 兼容 | ⚠️ Create, EMF, Geckolib, TLM, FTB, Trinkets, Tweakmore, Sophisticated（compat JSON 在 resources 但未注册） |

详细信息参见 [`TODO.md`](TODO.md)。

---

## 🔧 构建项目

### 要求

- **JDK 21+** (编译 target 21；`gradle.properties` 的 `org.gradle.java.home` 指向 GraalVM JDK 24.0.2。运行推荐 JDK 21 或 24；JDK 25 不兼容 Fabric API)
- **Gradle** (项目自带 wrapper，无需手动安装)

### 映射

| 组件 | 版本 |
|------|------|
| Minecraft | 1.21.4 |
| 映射 | **Mojang Official Mappings** + Parchment |
| Parchment | `parchment-1.21.4:2025.03.23@zip` |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.119.4+1.21.4 |
| Loom 插件 | `net.fabricmc.fabric-loom-remap` 1.17-SNAPSHOT（remapJar 静态重映射 mixin 注解，无需 refMap/useLegacyMixinAp） |

### 构建步骤

```bash
# 项目位置（本地副本，非 git 仓库）
cd "D:/Programs/MC/1.21.4/AcceleratedRendering-Ported"

# 构建
./gradlew build

# 输出位置
# build/libs/acceleratedrendering-ported-<version>.jar
```

### 运行 (开发环境)

```bash
./gradlew runClient
```

---

## 🤖 Vibe Coding 声明

本项目的 **1.21.4 Fabric 移植** 由 AI 辅助 (Claude Code) 完成。

- **人工监督**: 所有代码变更由人工审查和测试
- **参照实现**: 移植过程中参照了 Luna5ama 的 NeoForge 1.21.4-port 和原版 Argon4W 项目
- **易错点**: 参见 [`CLAUDE.md`](CLAUDE.md) 中的详细开发注意事项

---

## 📄 许可证

MIT License — 参见 [LICENSE](LICENSE) 文件。

原始模组版权 © Argon4W。
