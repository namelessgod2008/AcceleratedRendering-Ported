# AcceleratedRendering-reFabricated

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

本版本基于 ZhuRuoLing 的 Fabric 1.21.1 移植，参照 Luna5ama 的 NeoForge 1.21.4 移植，更新至 **Minecraft 1.21.4 Fabric**。

---

## 🚀 功能状态

### ✅ 已实现并正常工作

| 功能 | 说明 |
|------|------|
| 实体模型加速 | `ModelPart` compile 加速，GPU 计算着色器并行变换 |
| 实体阴影加速 | 加速方块阴影渲染 |
| 物品加速 (世界) | 掉落物、手持物品加速渲染 |
| 方块加速 | 方块模型渲染加速 (含着色) |
| 文本加速 | `BakedGlyph`/`Font` 加速渲染 (参数语义 + 描述符修复) |
| 模型加速 | Multipart、Weighted、SimpleBakedModel (1.21.4 API 适配) |
| 核心管线 | 缓冲区管理、LevelRenderer 帧图钩子、wrapper 链 |
| 实体/方块实体/容器过滤 | FilterFeature — 可选禁用特定类型加速 |
| Vanilla 渲染层修复 | `LivingEntityRenderer` 渲染状态 API + `HumanoidArmorLayer` 装备渲染 |
| 物品栏实体渲染 | `InventoryScreen` entity rendering (method_64045) |
| GUI 批处理 (fill/blit/slot) | 容器界面填充/位块传输/槽位批处理，热键栏物品批处理 |

### ⚠️ 部分实现

| 功能 | 状态 |
|------|------|
| 手部物品加速 | ❌ 不可行 — 坐标空间不匹配 (见 CLAUDE.md #4b) |
| GUI 槽位高亮批处理 | ⚠️ renderSlotHighlight 拆分为 Back/Front，待 @Shadow |
| GUI 字体批处理 | ⚠️ gui.FontMixin 禁用 — flushBatching 中字符串渲染为存根 |
| StringRenderOutput | ❌ 禁用 — 内部字段重构 |
| Iris 兼容 | ✅ `vanilla.LevelRendererMixin`→`method_62214`，需运行时验证 |
| ModernUI 兼容 | ✅ ModernUI 3.12.0 适配 |
| ImmediatelyFast 兼容 | ⚠️ @Pseudo — 未验证 |
| 其他 mod 兼容 | ⚠️ Create, EMF, Geckolib, TLM, FTB, Trinkets, Tweakmore, Sophisticated |

详细信息参见 [`TODO.md`](TODO.md)。

---

## 🔧 构建项目

### 要求

- **JDK 21+** (推荐 JDK 21 或 24；JDK 25 不兼容 Fabric API)
- **Gradle** (项目自带 wrapper，无需手动安装)
- **Git**

### 映射

| 组件 | 版本 |
|------|------|
| Minecraft | 1.21.4 |
| 映射 | **Mojang Official Mappings** + Parchment |
| Parchment | `parchment-1.21.4:2025.01.19@zip` |
| Fabric Loader | 0.16.11 |
| Fabric Loom | 1.16-SNAPSHOT |

### 构建步骤

```bash
# 克隆仓库
git clone https://github.com/ZhuRuoLing/AcceleratedRendering-reFabricated.git
cd AcceleratedRendering-reFabricated

# 构建
./gradlew build

# 输出位置
# build/libs/acceleratedrendering-<version>.jar
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
