# Resonote Foundation

> 状态：执行基线；00–05 已冻结  
> 更新日期：2026-08-17
> 规范范围：Brand、Color、Typography、Shape、Elevation、Layout、Iconography、Imagery、Motion、States、Accessibility  
> 规范权威：本文件中的 Token、数值、行为与边界  
> Material 基线：`androidx.compose.material3:material3:1.4.0` 稳定版 Baseline；排除 1.5 Alpha 与 Expressive Token

## 1. 使用方式

本文件是 Resonote Foundation 的规范源。实现必须直接引用这里记录的 Hex、Tone、sp、dp、时长、比率与行为规则，不得从视觉稿中识别或推测数值。

- Markdown 记录可执行规则与精确数据。
- `design/approved/` 图片只作为辅助视觉证据。
- 图片与本文档冲突时，以本文档为准，并修正或撤销对应视觉证据。
- 未冻结章节不得直接作为生产实现依据。

## 2. 参考依据

- [Android Color](https://developer.android.com/design/ui/mobile/guides/styles/color)
- [Android Themes](https://developer.android.com/design/ui/mobile/guides/styles/themes)
- [Material 3 Color Roles](https://m3.material.io/styles/color/roles)
- [Material 3 Color System](https://m3.material.io/styles/color/system/overview)
- [Material 3 Typography](https://m3.material.io/styles/typography/type-scale-tokens)
- [Material 3 States](https://m3.material.io/foundations/interaction/states/overview)
- [Android Accessibility](https://developer.android.com/design/ui/mobile/guides/foundations/accessibility)
- [Android Adaptive Layout](https://developer.android.com/design/ui/mobile/guides/layout-and-content/adapt-layout)
- [Compose Material 3 1.4.0](https://developer.android.com/jetpack/androidx/releases/compose-material3)

参考冲突时按职责判断：Material3 官方组件使用锁定版本 `1.4.0` 的公开 API 与 Source JAR Token → 本文档组件映射 → 已确认视觉证据；Resonote Extension 使用本文档已冻结规则 → 已确认视觉证据 → Material3 Foundation → 其他参考。视觉证据在任何情况下都不覆盖数值规范。

## 3. Foundation 规范
### 00 — Brand IP

#### 00A — Design Principles & Brand IP

- 品牌原则：温暖但不甜腻、沉浸但不封闭、有节奏但不躁动、现代但不冰冷。
- Signal Signature：以 `R + 波形` 为核心识别，不使用普通音符作为 Logo。
- 视觉证据覆盖字标、单色、小尺寸、App Icon 和品牌图形边界。
- 状态：**已冻结**。
- Wordmark Source：`design/approved/foundation/00-resonote-wordmark-source.svg`
- 辅助视觉证据：`design/approved/foundation/00-design-principles.png`

#### 00B — Launcher & Startup Identity

Signal Signature 的 Android 启动身份以 00A 为唯一概念来源；本节只冻结实现合同，不改变品牌图形含义。

Canonical Mark：

- Canonical Mark 使用 `108 × 108` 坐标系；Launcher 派生 Vector 围绕 `54 / 54` 中心缩放至 `0.62×`，Splash 派生保持 `0.72×`。两者使用独立光学尺寸，不改变母版几何。
- 以 `R + 波形` 的开放结构为唯一 Mark；基准 Stroke 为 `6`、Round Cap、Round Join，不增加文字、音符、阴影、渐变或装饰角标。
- 主 App 使用白色 Mark + `brandPrimary / Pulse Rose #B83252` 纯色背景；Catalog 伴生版使用白色 Mark + `catalogAccent / Harmonic Violet #66558F` 纯色背景。
- Launcher 必须同时提供 Adaptive、Round 与 Android 13+ Monochrome 图层，并验证 Circle、Rounded Square、Squircle、Teardrop Mask 与 `16 / 24 / 32 / 48px` 小尺寸。

System Splash：

| Variant | Light Background / Mark | Dark Background / Mark | Motion |
|---|---|---|---|
| Main App | `#FFFBFF / #AE2A4B` | `#201A1B / #FFB2BC` | API 31+ AVD，`trimPathEnd 0→1`，`750ms`，单次播放 |
| Catalog | `#FFFBFF / #66558F` | `#201A1B / #D0BCFE` | 静态终态 |

- 使用 AndroidX Core SplashScreen 与系统 Splash，不创建 Splash Activity 或额外 Compose 品牌页。
- Launcher 使用 `0.62×` 派生以平衡桌面同列图标的视觉重量；Splash 使用 `0.72×` 派生确保系统圆形内的启动识别度。两者均不修改 Canonical Mark 几何。
- API 26–30 使用完整 Mark 的静态 VectorDrawable；API 31+ 主 App 使用 AVD。启动动画不循环、不阻塞内容初始化，也不承担状态或进度信息。
- API 31+ 在系统显示 Splash 时，主 App 必须保留 Splash 至 AVD 完整播放后再进入内容页。Motion Scale `0×` 必须直接得到完整终态且不增加停留；热启动未显示 Splash 时，不额外补播动画。
- Splash 只跟随系统 Light / Dark。AMOLED 从 Compose 首帧开始；在主题偏好能够于 Activity 创建前读取之前，不宣称 AMOLED Splash。
- 状态：**已冻结；Android 实现完成，V-01 / V-10 仅部分自动化覆盖**。
- Canonical Source：`design/approved/foundation/00-signal-signature-source.svg`
- 派生源稿：`design/approved/foundation/00b-main-app-icon-source.svg`、`design/approved/foundation/00b-catalog-app-icon-source.svg`
- 评审证据：`design/approved/foundation/00b-launcher-splash-review.png`
- 实现依据：[SplashScreen guide](https://developer.android.com/develop/ui/views/launch/splash-screen)、[Core SplashScreen 1.2.0](https://developer.android.com/jetpack/androidx/releases/core)

### 01 — Color Foundation

颜色严格按以下链路产生：

```text
Brand Key Colors
→ Material Color Utilities Tonal Palettes
→ Light / Dark Semantic Schemes
→ Surface Hierarchy
→ Resonote AMOLED Extension
→ Contrast Validation
→ Component Color Mapping
```

通用约束：

- 文档色板展示的 13 个采样 Tone 为 `0 / 10 / 20 / 30 / 40 / 50 / 60 / 70 / 80 / 90 / 95 / 99 / 100`。完整 Tonal Palette 保留 `0–100` 范围，Semantic Surface Role 可引用 T4、T6、T12、T17等扩展 Tone。
- Tonal Palette 必须由 Material Color Utilities 逻辑生成，不手工猜色。
- Light 与 Dark 只能从同一组 Palette 映射语义角色。
- AMOLED 是 Resonote 对 Dark Scheme 的扩展，不是 Material 3 标准 Scheme。
- 业务组件只消费 `MaterialTheme.colorScheme` 的语义角色；禁止读取 Hex 后二次设色，也禁止根据主题模式在组件内自行分支。

#### 01A — Brand Key Colors

| Token | 名称 | Hex | 用途 |
|---|---|---|---|
| `brandPrimary` | Pulse Rose | `#B83252` | 主品牌锚点与 Primary Palette 来源 |
| `brandSecondary` | Echo Rose | `#70585B` | 与主品牌同色相的低彩度 Secondary Palette 来源 |
| `brandTertiary` | Beat Amber | `#855300` | 节奏强调与 Tertiary Palette 来源 |
| `catalogAccent` | Harmonic Violet | `#66558F` | Catalog 伴生版身份色；不进入主 App Secondary Palette |

- 状态：**已冻结**。
- 首页排行榜入口继续通过 Resonote Brand Extension 使用 Harmonic Violet；Light/Dark 保持原有 Violet 角色，Dynamic Color 下跟随平台 Secondary。该扩展只保护已验收首页入口，不派生主 App 的 Tonal Button、Chip 或状态容器。
- 可复现生成入口：`design/theme-generator`。`npm ci && npm run check` 校验冻结种子与提交的 Tonal Palette 产物一致；运行时不依赖 Material Color Utilities。
- 辅助视觉证据：`design/approved/foundation/01a-brand-key-colors.png`

#### 01B — Accent Tonal Palettes

| Palette | Key | T0 | T10 | T20 | T30 | T40 | T50 | T60 | T70 | T80 | T90 | T95 | T99 | T100 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Primary / Pulse Rose | `#B83252` | `#000000` | `#400012` | `#670022` | `#8D0D35` | `#AE2A4B` | `#D04463` | `#F15E7B` | `#FF869A` | `#FFB2BC` | `#FFD9DD` | `#FFECED` | `#FFFBFF` | `#FFFFFF` |
| Secondary / Echo Rose | `#70585B` | `#000000` | `#281719` | `#3F2B2E` | `#574144` | `#70585B` | `#8A7174` | `#A58A8D` | `#C1A4A7` | `#DEBFC2` | `#FBDBDE` | `#FFECEE` | `#FFFBFF` | `#FFFFFF` |
| Tertiary / Beat Amber | `#855300` | `#000000` | `#2A1700` | `#472A00` | `#653E00` | `#855300` | `#A26B1D` | `#C08535` | `#DE9F4D` | `#FDB965` | `#FFDDB8` | `#FFEEDE` | `#FFFBFF` | `#FFFFFF` |

- 状态：**已冻结**。
- 辅助视觉证据：`design/approved/foundation/01b-accent-tonal-palettes.png`

#### 01C — Neutral & Error Tonal Palettes

| Palette | 来源 | T0 | T10 | T20 | T30 | T40 | T50 | T60 | T70 | T80 | T90 | T95 | T99 | T100 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Neutral | Pulse Rose | `#000000` | `#201A1B` | `#362F2F` | `#4D4545` | `#655C5D` | `#7E7575` | `#988E8F` | `#B4A9A9` | `#CFC4C4` | `#ECE0E0` | `#FBEEEE` | `#FFFBFF` | `#FFFFFF` |
| Neutral Variant | Pulse Rose | `#000000` | `#24181A` | `#3A2D2E` | `#524344` | `#6B5A5C` | `#847374` | `#9F8C8E` | `#BAA6A8` | `#D7C1C3` | `#F4DDDF` | `#FFECED` | `#FFFBFF` | `#FFFFFF` |
| Error | Material 3 Error | `#000000` | `#410002` | `#690005` | `#93000A` | `#BA1A1A` | `#DE3730` | `#FF5449` | `#FF897D` | `#FFB4AB` | `#FFDAD6` | `#FFEDEA` | `#FFFBFF` | `#FFFFFF` |

- 状态：**已冻结**。
- 辅助视觉证据：`design/approved/foundation/01c-neutral-error-tonal-palettes.png`

#### 01D — Light Semantic Scheme

| Group | Role | Tone | Hex |
|---|---|---:|---|
| Primary | `primary` | 40 | `#AE2A4B` |
| Primary | `onPrimary` | 100 | `#FFFFFF` |
| Primary | `primaryContainer` | 90 | `#FFD9DD` |
| Primary | `onPrimaryContainer` | 10 | `#400012` |
| Secondary | `secondary` | 40 | `#70585B` |
| Secondary | `onSecondary` | 100 | `#FFFFFF` |
| Secondary | `secondaryContainer` | 90 | `#FBDBDE` |
| Secondary | `onSecondaryContainer` | 10 | `#281719` |
| Tertiary | `tertiary` | 40 | `#855300` |
| Tertiary | `onTertiary` | 100 | `#FFFFFF` |
| Tertiary | `tertiaryContainer` | 90 | `#FFDDB8` |
| Tertiary | `onTertiaryContainer` | 10 | `#2A1700` |
| Error | `error` | 40 | `#BA1A1A` |
| Error | `onError` | 100 | `#FFFFFF` |
| Error | `errorContainer` | 90 | `#FFDAD6` |
| Error | `onErrorContainer` | 10 | `#410002` |
| Fixed Primary | `primaryFixed` | 90 | `#FFD9DD` |
| Fixed Primary | `primaryFixedDim` | 80 | `#FFB2BC` |
| Fixed Primary | `onPrimaryFixed` | 10 | `#400012` |
| Fixed Primary | `onPrimaryFixedVariant` | 30 | `#8D0D35` |
| Fixed Secondary | `secondaryFixed` | 90 | `#FBDBDE` |
| Fixed Secondary | `secondaryFixedDim` | 80 | `#DEBFC2` |
| Fixed Secondary | `onSecondaryFixed` | 10 | `#281719` |
| Fixed Secondary | `onSecondaryFixedVariant` | 30 | `#574144` |
| Fixed Tertiary | `tertiaryFixed` | 90 | `#FFDDB8` |
| Fixed Tertiary | `tertiaryFixedDim` | 80 | `#FDB965` |
| Fixed Tertiary | `onTertiaryFixed` | 10 | `#2A1700` |
| Fixed Tertiary | `onTertiaryFixedVariant` | 30 | `#653E00` |
| Neutral | `background` | 99 | `#FFFBFF` |
| Neutral | `onBackground` | 10 | `#201A1B` |
| Neutral | `surface` | 98 | `#FFF8F7` |
| Neutral | `onSurface` | 10 | `#201A1B` |
| Surface | `surfaceDim` | 87 | `#E3D7D7` |
| Surface | `surfaceBright` | 98 | `#FFF8F7` |
| Surface | `surfaceContainerLowest` | 100 | `#FFFFFF` |
| Surface | `surfaceContainerLow` | 96 | `#FEF1F1` |
| Surface | `surfaceContainer` | 94 | `#F8EBEB` |
| Surface | `surfaceContainerHigh` | 92 | `#F2E5E5` |
| Surface | `surfaceContainerHighest` | 90 | `#ECE0E0` |
| Neutral Variant | `surfaceVariant` | 90 | `#F4DDDF` |
| Neutral Variant | `onSurfaceVariant` | 30 | `#524344` |
| Neutral Variant | `outline` | 50 | `#847374` |
| Neutral Variant | `outlineVariant` | 80 | `#D7C1C3` |
| Inverse/System | `inverseSurface` | 20 | `#362F2F` |
| Inverse/System | `inverseOnSurface` | 95 | `#FBEEEE` |
| Inverse/System | `inversePrimary` | 80 | `#FFB2BC` |
| Inverse/System | `surfaceTint` | 40 | `#AE2A4B` |
| Inverse/System | `scrim` | 0 | `#000000` |
| Inverse/System | `shadow` | 0 | `#000000` |

- 状态：**已冻结**。
- 辅助视觉证据：`design/approved/foundation/01d-light-semantic-scheme.png`
- 可复现源稿：`design/approved/foundation/01d-light-semantic-scheme-source.svg`

#### 01E — Dark Semantic Scheme

| Group | Role | Tone | Hex |
|---|---|---:|---|
| Primary | `primary` | 80 | `#FFB2BC` |
| Primary | `onPrimary` | 20 | `#670022` |
| Primary | `primaryContainer` | 30 | `#8D0D35` |
| Primary | `onPrimaryContainer` | 90 | `#FFD9DD` |
| Secondary | `secondary` | 80 | `#DEBFC2` |
| Secondary | `onSecondary` | 20 | `#3F2B2E` |
| Secondary | `secondaryContainer` | 30 | `#574144` |
| Secondary | `onSecondaryContainer` | 90 | `#FBDBDE` |
| Tertiary | `tertiary` | 80 | `#FDB965` |
| Tertiary | `onTertiary` | 20 | `#472A00` |
| Tertiary | `tertiaryContainer` | 30 | `#653E00` |
| Tertiary | `onTertiaryContainer` | 90 | `#FFDDB8` |
| Error | `error` | 80 | `#FFB4AB` |
| Error | `onError` | 20 | `#690005` |
| Error | `errorContainer` | 30 | `#93000A` |
| Error | `onErrorContainer` | 90 | `#FFDAD6` |
| Fixed Primary | `primaryFixed` | 90 | `#FFD9DD` |
| Fixed Primary | `primaryFixedDim` | 80 | `#FFB2BC` |
| Fixed Primary | `onPrimaryFixed` | 10 | `#400012` |
| Fixed Primary | `onPrimaryFixedVariant` | 30 | `#8D0D35` |
| Fixed Secondary | `secondaryFixed` | 90 | `#FBDBDE` |
| Fixed Secondary | `secondaryFixedDim` | 80 | `#DEBFC2` |
| Fixed Secondary | `onSecondaryFixed` | 10 | `#281719` |
| Fixed Secondary | `onSecondaryFixedVariant` | 30 | `#574144` |
| Fixed Tertiary | `tertiaryFixed` | 90 | `#FFDDB8` |
| Fixed Tertiary | `tertiaryFixedDim` | 80 | `#FDB965` |
| Fixed Tertiary | `onTertiaryFixed` | 10 | `#2A1700` |
| Fixed Tertiary | `onTertiaryFixedVariant` | 30 | `#653E00` |
| Neutral | `background` | 10 | `#201A1B` |
| Neutral | `onBackground` | 90 | `#ECE0E0` |
| Neutral | `surface` | 6 | `#181212` |
| Neutral | `onSurface` | 90 | `#ECE0E0` |
| Surface | `surfaceDim` | 6 | `#181212` |
| Surface | `surfaceBright` | 24 | `#3F3738` |
| Surface | `surfaceContainerLowest` | 4 | `#120D0D` |
| Surface | `surfaceContainerLow` | 10 | `#201A1B` |
| Surface | `surfaceContainer` | 12 | `#241E1F` |
| Surface | `surfaceContainerHigh` | 17 | `#2F2829` |
| Surface | `surfaceContainerHighest` | 22 | `#3A3334` |
| Neutral Variant | `surfaceVariant` | 30 | `#524344` |
| Neutral Variant | `onSurfaceVariant` | 80 | `#D7C1C3` |
| Neutral Variant | `outline` | 60 | `#9F8C8E` |
| Neutral Variant | `outlineVariant` | 30 | `#524344` |
| Inverse/System | `inverseSurface` | 90 | `#ECE0E0` |
| Inverse/System | `inverseOnSurface` | 20 | `#362F2F` |
| Inverse/System | `inversePrimary` | 40 | `#AE2A4B` |
| Inverse/System | `surfaceTint` | 80 | `#FFB2BC` |
| Inverse/System | `scrim` | 0 | `#000000` |
| Inverse/System | `shadow` | 0 | `#000000` |

- 状态：**已冻结**。
- 辅助视觉证据：`design/approved/foundation/01e-dark-semantic-scheme.png`
- 可复现源稿：`design/approved/foundation/01e-dark-semantic-scheme-source.svg`

Fixed Roles 在 Light 与 Dark 中保持同一 Tone/Hex：`Fixed = T90`、`FixedDim = T80`、`OnFixed = T10`、`OnFixedVariant = T30`。它们用于需要跨主题保持色调不变的容器，不替代常规 Primary/Secondary/Tertiary Role。

01D/01E 覆盖 Material3 1.4.0 `ColorScheme` 的全部 48 个属性；额外的固定系统色通过 `ResonoteSystemColors` 暴露：`shadow = #000000`，黑色媒体遮罩的前景使用 `onScrim = #FFFFFF`，视频画布使用 `mediaCanvas = #000000` / `onMediaCanvas = #FFFFFF`。`onScrim` 只能与 `scrim` 或等价的黑色媒体遮罩配对；视频画布不参与主题 Surface 层级，也不得用于普通页面或卡片。

#### 01F — Surface Hierarchy

Tonal Surface 优先表达层级，不用厚重阴影替代 Surface Token。所有 Tone/Hex 只在 01D/01E 完整 Semantic Scheme 中定义；本节只记录层级顺序和用法，避免第二数据源。

| Theme | 由低到高的 Surface 层级 |
|---|---|
| Light | `surfaceContainerLowest` → `surfaceContainerLow` → `surfaceContainer` → `surfaceContainerHigh` → `surfaceContainerHighest` |
| Dark | `surfaceContainerLowest` → `surfaceContainerLow` → `surfaceContainer` → `surfaceContainerHigh` → `surfaceContainerHighest` |

- `surfaceDim` / `surface` / `surfaceBright` 表达页面基础面的明暗范围；Container 序列表达容器相对层级。
- 视觉证据中的 Tone/Hex 为 01D/01E 表格的派生展示，不构成新的规范定义。

- 状态：**已冻结**。
- 辅助视觉证据：`design/approved/foundation/01f-surface-hierarchy.png`
- 可复现源稿：`design/approved/foundation/01f-surface-hierarchy-source.svg`

#### 01G — AMOLED Extension

AMOLED 基于 Dark Scheme 派生。`background`、`surfaceDim`、`surface` 与 `surfaceContainerLowest` 合并为纯黑基础层，其余层级保持有限分离。

| Role | Tone | Hex | 说明 |
|---|---:|---|---|
| `amoledBase` | 0 | `#000000` | 映射 Background、Surface Dim、Surface、Surface Container Lowest |
| `surfaceContainerLow` | 4 | `#120D0D` | 最低可见抬升层 |
| `surfaceContainer` | 6 | `#181212` | 默认容器层 |
| `surfaceContainerHigh` | 10 | `#201A1B` | 高容器层 |
| `surfaceContainerHighest` | 17 | `#2F2829` | 最高容器层 |
| `surfaceBright` | 24 | `#3F3738` | AMOLED 最亮 Surface |

- 状态：**已冻结**。
- 辅助视觉证据：`design/approved/foundation/01g-amoled-extension.png`

#### 01G-1 — Runtime Theme Policy

| 设置 | 生效 Scheme | 约束 |
|---|---|---|
| 跟随系统 | 品牌 Light / Dark | 默认值；跟随系统明暗 |
| 浅色 | 品牌 Light | 不受系统明暗影响 |
| 深色 | 品牌 Dark | 不受系统明暗影响 |
| AMOLED | Resonote AMOLED Extension | 与 Dynamic Color 互斥 |
| Dynamic Color | Android 12+ 平台 Dynamic Light / Dark | 开启时使用平台完整 Scheme，不与品牌 Accent 混合 |

- 默认偏好为 `SYSTEM + Brand`；主题模式和 Dynamic 开关必须持久化。
- Android 12 以下隐藏 Dynamic Color 入口，并使用对应的品牌 Scheme。
- 开启 Dynamic Color 时若当前为 AMOLED，模式切回 `SYSTEM`；选择 AMOLED 时自动关闭 Dynamic Color。互斥规则由偏好 Repository 保证，组件不得自行修正状态。
- Dynamic Color 关闭时，System / Light / Dark 必须使用由 01A 冻结种子生成的完整品牌 Scheme。
- 状态：**已冻结**。

#### 01H — Color Contrast Validation

- 普通文字最低对比度：`4.5:1`。
- 大文字与关键非文字元素最低对比度：`3.0:1`。
- `LIMITED` 组合不得用于普通文字；`FAIL` 组合不得用于文字或关键图形。

| 场景 | 前景 | 背景 | 比率 | 结论 | 允许用途 |
|---|---|---|---:|---|---|
| Light Body | `#201A1B` | `#FFF8F7` | `16.34:1` | PASS | 普通文字 |
| Light Primary | `#FFFFFF` | `#AE2A4B` | `6.50:1` | PASS | 普通文字 |
| Dark Body | `#ECE0E0` | `#181212` | `14.38:1` | PASS | 普通文字 |
| Dark Primary | `#670022` | `#FFB2BC` | `7.72:1` | PASS | 普通文字 |
| AMOLED Body | `#ECE0E0` | `#000000` | `16.31:1` | PASS | 普通文字 |
| Beat Amber / AMOLED | `#855300` | `#000000` | `3.23:1` | LIMITED | 仅大文字与关键图形 |
| Pulse Rose / Container | `#B83252` | `#FFD9DD` | `4.49:1` | FAIL | 不用于普通文字 |
| Catalog Violet / Dark | `#66558F` | `#181212` | `2.88:1` | FAIL | 仅用于 Catalog 品牌容器，不用于文字或关键图形 |

- 比率使用 WCAG 相对亮度公式重新计算并保留两位小数。
- 状态：**已冻结**。
- 辅助视觉证据：`design/approved/foundation/01h-color-contrast-validation.png`
- 可复现源稿：`design/approved/foundation/01h-color-contrast-validation-source.svg`

### 02 — Typography & Accessibility

字体家族统一使用 Android System Sans，单位为 `sp`。字号、行高、字重和字距以本节为唯一规范，不再维护 Typography 图片。

#### 02A — Type Scale

| Group | Token | Size | Line height | Weight | Tracking |
|---|---|---:|---:|---:|---:|
| Display | `displayLarge` | 57sp | 64sp | 400 | -0.25sp |
| Display | `displayMedium` | 45sp | 52sp | 400 | 0sp |
| Display | `displaySmall` | 36sp | 44sp | 400 | 0sp |
| Headline | `headlineLarge` | 32sp | 40sp | 400 | 0sp |
| Headline | `headlineMedium` | 28sp | 36sp | 400 | 0sp |
| Headline | `headlineSmall` | 24sp | 32sp | 400 | 0sp |
| Title | `titleLarge` | 22sp | 28sp | 400 | 0sp |
| Title | `titleMedium` | 16sp | 24sp | 500 | 0.15sp |
| Title | `titleSmall` | 14sp | 20sp | 500 | 0.10sp |
| Body | `bodyLarge` | 16sp | 24sp | 400 | 0.50sp |
| Body | `bodyMedium` | 14sp | 20sp | 400 | 0.25sp |
| Body | `bodySmall` | 12sp | 16sp | 400 | 0.40sp |
| Label | `labelLarge` | 14sp | 20sp | 500 | 0.10sp |
| Label | `labelMedium` | 12sp | 16sp | 500 | 0.50sp |
| Label | `labelSmall` | 11sp | 16sp | 500 | 0.50sp |

- `Caption` 不是 Material 3 独立角色；Resonote 将其定义为 `bodySmall` 的语义别名，不新增 Token。
- 业务组件必须引用 Type Token，不得复制一套局部字号。
- 状态：**文档已冻结**。

#### 02B — Large Text & Accessibility

- 必须在 `fontScale = 1.0` 与 `fontScale = 2.0` 下验证正文、标题、标签和重要说明。
- `sp` Token 保持不变，由 Android 字体缩放产生有效字号；例如 `16sp` 在 200% 下为 `32sp` 有效尺寸。
- 文本容器使用弹性高度，允许内容自然换行并向下扩展。
- 放大后保持语义与阅读顺序，不用视觉重排破坏读屏顺序。
- 重要文字必须完整可见，不使用强制省略号隐藏关键信息。
- 禁止固定高度导致裁切，禁止文字相互遮挡，禁止为了塞入容器而缩小字体。
- 控件和布局必须随文字扩展；具体组件在 Component System 中分别验证 200% 字体。
- 状态：**文档已冻结**。

### 03 — Shape, Elevation & Layout

#### 03A — Shape Scale

| Token | Radius | 默认映射 |
|---|---:|---|
| `shapeNone` | 0dp | Edge-to-edge、分割面与明确的直角结构 |
| `shapeExtraSmall` | 4dp | Outlined Text Field、Snackbar、紧凑容器 |
| `shapeSmall` | 8dp | Chip、小型内嵌强调 |
| `shapeMedium` | 12dp | Card、Album Artwork、常规内容容器 |
| `shapeLarge` | 16dp | 大型内容容器 |
| `shapeExtraLarge` | 28dp | Dialog、Bottom Sheet 顶角与大型面板 |
| `shapeFull` | 50% / `CircleShape` | Button、Segmented Button、Icon Button、头像、状态点与官方要求的活动指示器 |

- Material3 1.4.0 原生组件使用 Baseline 默认 Shape：Button 与 Segmented Button 使用 `shapeFull`，Chip 使用 `shapeSmall`，Outlined Text Field 使用 `shapeExtraSmall`。
- `shapeFull` 不是任意自定义容器的通用圆角；Card 和列表项继续使用对应的非 Full Token。
- 同一组件的状态变化不得改变圆角 Token，避免视觉跳动。
- 状态：**已冻结**。
- 交付：仅文档，不生成独立 Shape PNG。

#### 03B — Elevation

Material 3 将 Elevation 分为 `tonalElevation` 与 `shadowElevation`。Resonote 默认通过 Tonal Surface 表达层级，阴影只用于真实重叠且仅靠色调不足以分离的表面。实现依据：[Material Design 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3#elevation)。

| Token | Level | Tonal elevation | 首选 Surface Role | 默认 Shadow | Shadow 上限 | 默认用途 |
|---|---:|---:|---|---:|---:|---|
| `elevationLevel0` | 0 | 0dp | `surface` | 0dp | 0dp | 页面基础层、嵌入式内容 |
| `elevationLevel1` | 1 | 1dp | `surfaceContainerLow` | 0dp | 1dp | 静止 Card、Modal Bottom Sheet、Modal Drawer |
| `elevationLevel2` | 2 | 3dp | `surfaceContainer` | 0dp | 3dp | 滚动后 App Bar、Navigation Bar、Menu |
| `elevationLevel3` | 3 | 6dp | `surfaceContainerHigh` | 0dp | 6dp | Snackbar、Dialog、FAB 与临时抬升控件 |
| `elevationLevel4` | 4 | 8dp | `surfaceContainerHighest` | 0dp | 8dp | Dragged Surface 等官方 Token 明确要求的高层级状态 |
| `elevationLevel5` | 5 | 12dp | `surfaceContainerHighest` | 0dp | 12dp | 保留的最高阶 Level；Material3 1.4.0 常用组件不默认映射到此级 |

规则：

- `tonalElevation` 与首选 Surface Role 表达同一层级意图；组件应优先使用已定义 Surface Role，不能叠加出新的未记录颜色。
- `shadowElevation` 默认统一为 `0dp`；只有表面真实覆盖其他内容且 Tonal Surface 无法建立边界时，才可在表中上限内启用。
- 组件规范可以把某个真实悬浮表面的 Shadow 固定到该 Level 上限；这是组件级例外，不改变其他同级组件的默认值。Compact Mini Player 按 09A 固定使用 Level 3 的 `6dp` Shadow。
- 阴影不是 `zIndex`。绘制顺序必须由布局或 `Modifier.zIndex` 明确控制。
- Light、Dark 与 AMOLED 使用相同 Level 语义，不分别发明 Elevation Token。
- AMOLED 的阴影不可作为唯一层级信号，必须同时使用已冻结的 AMOLED Surface 层级、边界或 Scrim。
- 禁止厚重投影、彩色发光、Neumorphism、内阴影和仅为装饰增加的 Elevation。
- 同一组件的 Pressed/Focused 状态不得通过剧烈升降产生跳动；具体状态差异在 05B 定义。
- 状态：**已冻结**。
- 辅助视觉证据：`design/approved/foundation/03b-elevation.png`
- 可复现源稿：`design/approved/foundation/03b-elevation-source.svg`

#### 03C — Spacing & Grid

4dp 是最小布局单位，8dp 是页面与组件的主要节奏。所有布局间距使用 `dp`；文字尺寸与行高继续使用 `sp`。

| Token | Value | 默认用途 |
|---|---:|---|
| `space0` | 0dp | 无间距、显式重置 |
| `space1` | 4dp | 微间距、紧密图形与辅助标记 |
| `space2` | 8dp | 图标与文字、紧凑组件内部间距 |
| `space3` | 12dp | 强关联内容、紧凑容器 Padding |
| `space4` | 16dp | 标准组件 Padding、Compact 页面基础边距 |
| `space6` | 24dp | 内容组与小节内部间距 |
| `space8` | 32dp | 独立内容区块间距 |
| `space10` | 40dp | 大型内容组过渡，谨慎使用 |
| `space12` | 48dp | 大区块分隔与最小触控尺寸基准 |
| `space16` | 64dp | 页面级强分隔和大面积留白 |

Border 与触控 Token：

| Token | Value | 用途 |
|---|---:|---|
| `borderHairline` | 1dp | Divider、Outline 与非交互边界 |
| `borderStrong` | 2dp | Focus、Selected、Error 等需要强化的边界 |
| `touchTargetMin` | 48dp | 所有触控元素的最小可聚焦宽度与高度 |

规则：

- 组件内部以 4dp Grid 对齐，页面布局优先使用 8dp 节奏。
- 同一层级的同类间距必须复用同一 Token，不用接近但不同的任意值制造视觉噪声。
- 1dp 与 2dp 只用于 Border，不进入常规布局间距刻度。
- 24dp 图标可以通过 Padding 或最小尺寸获得 48dp 触控区；不得为了满足触控区而无条件放大图形本身。
- 相邻触控区不得重叠，且不能被父容器裁切；视觉边界小于 48dp 时仍须保留完整可聚焦区域。
- 负间距只允许用于已文档化的视觉重叠，不作为压缩布局的常规手段。
- 页面断点、列数、Gutter、Window Insets 与各尺寸页面边距由 03D 定义，不在本节重复。
- 触控区依据：[Android Accessibility](https://developer.android.com/design/ui/mobile/guides/foundations/accessibility)。
- 状态：**已冻结**。
- 交付：仅文档，不生成独立 Spacing PNG。

#### 03D — Adaptive Layout & Insets

布局判断基于当前 App Window，而不是设备型号、物理屏幕或横竖屏。窗口在分屏、自由窗口或折叠状态变化时必须重新计算。

官方宽度分类与 Resonote 内容布局映射：

| Android Width Size Class | Window width | Resonote Layout Mode | 说明 |
|---|---:|---|---|
| Compact | `< 600dp` | Compact | 单栏内容为默认 |
| Medium | `600–839dp` | Medium | 单栏或任务驱动双栏 |
| Expanded | `840–1199dp` | Expanded | 优先利用双栏 |
| Large | `1200–1599dp` | Expanded | 保持 Expanded 拓扑并限制内容宽度 |
| Extra-large | `≥ 1600dp` | Expanded | 内容居中，不无限拉伸列宽 |

Primary Navigation 不从上述宽度表单独推导。实现使用 Material3 Adaptive
`WindowAdaptiveInfo` 与 `NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo()`，由稳定版
Material 规范综合 Window Width、Window Height 与 Posture 选择 Navigation Bar 或 Navigation
Rail；例如 Compact Height 与 Tabletop Posture 即使宽度较大也可使用 Navigation Bar。

Resonote Grid：

| Layout Mode | Columns | Outer margin | Gutter | Max body width | Body pattern |
|---|---:|---:|---:|---:|---|
| Compact | 4 | 16dp | 16dp | 窗口可用宽度 | 单栏 |
| Medium | 8 | 24dp | 24dp | 窗口可用宽度 | 单栏或任务驱动双栏 |
| Expanded | 12 | 最少 32dp | 24dp | 1200dp | 优先 List–Detail 或 Supporting Pane |

- Large 与 Extra-large 的实际水平外边距为 `max(32dp, (availableWidth - 1200dp) / 2)`。
- 阅读型连续文本使用独立 `720dp` 最大宽度，不把正文拉满 1200dp。
- 五档 Width 分类服务 Grid、内容限宽与验证；Primary Navigation 同时响应 Height 与 Posture。
- 同一 Size Class 内仍需弹性伸缩，不能将页面锁死为单一画板尺寸。
- Primary Navigation 的目的地、选中状态和语义在模式切换前后保持一致。
- 折叠设备存在遮挡式 Hinge/Fold 时，不让文字、触控目标或单个内容 Pane 跨越遮挡区域；可将 Hinge 作为 Pane 分隔。

Compact 竖屏页面设计稿画板：

| Item | Design evidence value | 规则 |
|---|---:|---|
| Canvas width | `390dp` | V1 Compact 竖屏页面设计稿固定宽度；不得为容纳内容改变画板宽度 |
| Fixed-page canvas | `390 × 844dp` | 仅用于内容本身不滚动且能够完整容纳的页面 |
| Scroll-page canvas | `390 × Auto` | 最小高度 `844dp`；内容按真实区块和间距向下完整展开，不压缩、不裁切、不为凑齐一屏删减内容 |
| Status-bar evidence region | `44dp` | 设计稿顶部必须包含完整系统状态栏区域；它不属于 Top App Bar |
| Bottom gesture-safe evidence region | `34dp` | 设计稿底部必须包含完整手势安全区 |
| Gesture indicator | `134 × 5dp` | 水平居中、距底部 `8dp`、使用 `shapeFull`；Dark/AMOLED 或图片背景使用白色，Light 使用 `onSurface` |

- `design/approved/player/player-cover-page.png` 是上述系统区域与完整画板构成的评审参考；它不冻结 Player 的视觉、内容层级或组件实现。
- 固定页面以 `390 × 844dp` 交付；滚动页面使用 `390 × Auto` 长画板，直到全部设计内容、页面底部固定区域与手势安全区完整结束。
- 滚动长图只在顶部展示一次 Status Bar，只在末尾展示一次底部 App Chrome 与手势安全区，避免用重复系统栏污染页面评审。
- 长图中的单次展示只是设计交付表达；运行时 Status Bar、Navigation Suite、Mini Player 等固定区域仍按各自产品合同固定在 Window。Compact Mini Player 按 09A 作为滚动内容上方的悬浮 Overlay，中间滚动状态允许内容从其后方经过；滚动容器通过末尾 Content Padding 保证最后一个可聚焦 Item 能完整滚动到 Mini Player 上方，并继续避让 Navigation Suite 与系统 Insets。
- `44dp`、`34dp` 与 `134 × 5dp` 只规范 Compact 设计证据的统一画板，不得作为 Android 运行时系统 Insets 常量。实现必须继续读取真实 `WindowInsets`。

Edge-to-edge 与 Insets：

| Insets | 使用范围 |
|---|---|
| `WindowInsets.safeDrawing` | 保护重要文字、控件和不可被系统 UI 遮挡的内容 |
| `WindowInsets.safeGestures` | 保护 Carousel、Sheet、拖拽区等与系统手势冲突的交互 |
| `WindowInsets.safeContent` | 同时需要视觉与手势安全的页面区域 |
| `WindowInsets.ime` | Text Field、搜索与输入流程，随键盘动画调整 |
| `WindowInsets.displayCutout` | 刘海、挖孔与折叠设备切口；关键内容必须避让 |
| `WindowInsets.systemBars` | 需要分别处理状态栏、标题栏或导航栏的组件 |

规则：

- 所有页面启用 Edge-to-edge；Android 15 / API 35+ 的强制 Edge-to-edge 不能通过硬编码系统栏背景规避。
- 背景色、图片和非交互装饰可以延伸到系统栏后方；重要文字与触控目标必须处于相应安全区域。
- 不硬编码 Status Bar、Navigation Bar、Caption Bar、Cutout 或 IME 高度。
- 使用 Material 3 `Scaffold` 的 `innerPadding` 后，不再在同一层重复应用 `safeDrawingPadding()`；每类 Insets 只由一个明确层级消费。
- 接收 `Scaffold` `innerPadding` 的内容容器必须按 `.padding(innerPadding).consumeWindowInsets(innerPadding)` 的顺序应用两者；只应用 `padding` 会让嵌套 Scaffold 再次读取同一系统 Insets，禁止省略消费步骤。
- 嵌套 Scaffold 必须声明 Insets 所有者。外层 Navigation/App Scaffold 已消费的 Bottom/System Insets，Feature Scaffold 不得再次消费；Feature 只处理尚未由父层处理的 Insets。
- 输入页面启用 `adjustResize` 并处理 `ime`；当前焦点与提交控件不得被键盘遮挡。
- 手势排除区保持最小，只为确实冲突的局部交互申请，不屏蔽整条屏幕边缘。
- Insets 变化必须参与动画和布局重算，不在旋转、分屏、键盘开合时跳帧或遗留空白。
- Bottom Navigation 的 Insets 验证必须覆盖 Gesture、Two-button 与 Three-button Navigation；页面内容底边到 Navigation Bar 顶边不得出现第二份系统安全区空白。
- 实现基线参考 NiA `NiaApp.kt` 的 `padding(padding).consumeWindowInsets(padding)`，当前固定参考提交为 `7d45eae4f8720a0c77f507712ba2437ff974b6ed`。
- 依据：[Window Size Classes](https://developer.android.com/develop/ui/views/layout/use-window-size-classes)、[Edge-to-edge](https://developer.android.com/develop/ui/compose/system/setup-e2e)、[Window Insets](https://developer.android.com/develop/ui/compose/system/insets)。
- 状态：**已冻结**。
- 辅助视觉证据：`design/approved/foundation/03d-adaptive-layout-insets.png`

### 04 — Iconography, Artwork & Imagery

#### 04A — Icon System

Resonote 系统图标以 Material Symbols Outlined 为基础，不另造一套常用操作图标。图标家族与绘制规范依据：[Material 3 Icons](https://m3.material.io/styles/icons/overview)、[Designing icons](https://m3.material.io/styles/icons/designing-icons)；变量轴依据：[Material Symbols Guide](https://developers.google.com/fonts/docs/material_symbols)。

- 标准图标必须直接使用 Google Fonts、Google Material Symbols Repository 或 Android 官方依赖分发的 Material Symbols 资产；禁止从截图描摹、由生成模型重绘或使用非官方近似路径。

基础配置：

| Property / Axis | Token / Value | 规则 |
|---|---|---|
| Family | `Material Symbols Outlined` | 默认系统图标来源；不与 Rounded、Sharp 混用 |
| Viewport | `24dp × 24dp` | 标准设计与导出画板 |
| Live area | `20dp × 20dp` | 四周默认保留 2dp 光学空间 |
| `FILL` | `0` | 默认、未选中与普通操作状态 |
| `FILL` | `1` | 仅用于官方提供匹配形态的 Selected/Active 状态 |
| `wght` | `400` | 全局默认，不用粗细跳变表达 Pressed |
| `GRAD` | `0` | Light、Dark、AMOLED 使用相同默认 Grade |
| `opsz` | 与实际 Icon Token 一致 | 20/24/40/48dp 分别使用对应 Optical Size |
| Optical shift | `≤ ±1dp` | 仅为修正视觉重心，不改变 Viewport |

尺寸 Token：

| Token | Glyph size | 默认用途 |
|---|---:|---|
| `iconSmall` | 20dp | 紧凑辅助图标、密集信息区 |
| `iconDefault` | 24dp | 导航、标准操作和组件默认图标 |
| `iconLarge` | 40dp | 高强调操作与较大空态辅助图形 |
| `iconDisplay` | 48dp | 低密度展示用途，不直接等同触控区 |
| `iconTouchTarget` | 48dp | 所有可点击图标的最小容器宽高 |

风格与状态：

- 图标默认单色，通过组件提供的 Semantic Color 着色；不在系统图标内嵌渐变、多色或阴影。
- Default 使用 `FILL 0`；Selected 可使用 `FILL 1`，但还必须叠加颜色、容器、标签或位置中的至少一个视觉指标。
- Pressed/Focused 不改变图标几何、Viewport 或 `wght`，状态反馈由 05B 定义。
- 方向性图标（Back、Forward、Undo、Redo）随 RTL 自动镜像；非方向性图标（Search、Favorite、Settings、Play）不镜像。
- 同一语义在不同页面复用同一 Symbol，不用近义图标制造局部风格。
- 普通音符可以表达明确的音乐功能，但不得替代 Resonote Signal Signature 或充当品牌标志。

自定义图标边界：

- 仅当 Material Symbols 没有准确语义时新增自定义图标。
- 自定义图标继续使用 24dp Viewport、20dp Live Area、默认 2dp Stroke 和与 `wght 400` 相当的视觉重量；外角默认 2dp，Outlined 内角保持方角，2dp 或更窄的 Stroke 不使用圆形端点。
- 允许最多 ±1dp 光学位移；禁止非等比缩放、强行塞满 Viewport 或使用细碎装饰。
- 自定义资产必须保留矢量源，并提供唯一语义名称，不按页面复制变体。

可访问性：

- 有语义的 `Icon` 必须提供本地化 `contentDescription`；纯装饰图标才使用 `null`。
- Icon-only Action 必须具有 48dp 触控区，并为长按或悬停提供 Tooltip。
- 不熟悉、不可唯一识别或高风险操作的图标必须同时显示文字标签。
- 依据：[Compose Icon API](https://developer.android.com/reference/kotlin/androidx/compose/material3/Icon)、[Android Tooltips](https://developer.android.com/develop/ui/views/components/tooltips)。
- 状态：**已冻结**。
- 辅助视觉证据：`design/approved/foundation/04a-icon-system.png`
- 已确认矢量源：`design/approved/foundation/04a-icon-system-source.svg`

#### 04B — Album Artwork

Album Artwork 规范图片容器与媒体处理，不规定 Album Tile、Song Row 等组件的最终布局尺寸。组件尺寸由 08 Music Components 在复用本节规则的前提下定义。

基础 Token：

| Token | Value | 规则 |
|---|---:|---|
| `artworkAspectRatio` | `1:1` | 所有标准专辑封面容器保持正方形 |
| `artworkShape` | `shapeMedium / 12dp` | Grid、Card 与常规封面的默认圆角 |
| `artworkShapeCompact` | `shapeSmall / 8dp` | 仅供紧凑列表缩略图使用 |
| `artworkShapeHero` | `shapeLarge / 16dp` | 独立大尺寸展示；不用于 Player 页面规范 |
| `artworkContentScale` | `Crop` | 等比填满容器，不拉伸、不挤压 |
| `artworkAlignment` | `Center` | 无焦点元数据时默认中心裁切 |
| `artworkFocalAlignment` | Optional | 数据源提供可信焦点时才覆盖默认对齐 |
| `artworkOverlayInset` | `space8 / 8dp` | 必须叠加状态或 Badge 时与边缘保持距离 |
| `artworkPlaceholder` | 中性底色 + 两条低对比度水平标记 | Loading、Missing、Error 与 Unavailable 共用同一确定性封面占位，不因状态更换图形 |

比例与裁切：

- UI 容器始终先占据 `1:1` 比例，图片加载不得引发布局跳动。
- 正方形源图等比填充；横图与竖图使用 `Crop`，从中心或可信焦点向外裁切，不使用非等比缩放。
- 不以模糊放大、镜像延展或自动生成内容补齐边缘；这些处理会篡改封面作品。
- 默认不使用 Letterbox。只有数据源明确要求完整展示且业务同时提供专用容器时，才允许改用 `Fit`；该变体不得伪装成标准 Album Artwork。
- 圆角由外层容器统一裁切；图片、占位与状态层必须共享同一 Shape，禁止出现方形加载层或双重圆角。

资源质量：

- 请求尺寸应匹配实际容器像素尺寸与设备密度；优先选择不小于目标尺寸的最近档资源，避免无意义下载原始大图。
- 不把低分辨率缩略图放大作为大尺寸封面。资源不足时仍保持容器稳定，并使用缺失/错误占位，不使用锐化伪造细节。
- 缓存命中、网络加载与本地文件使用同一裁切和 Shape 规则，不能因来源不同改变构图。

状态模型：

| State | Visual | 行为 |
|---|---|---|
| `Loading` | `artworkPlaceholder` | 保持 1:1 与最终 Shape；加载语义和相邻内容骨架由所属组件表达，不在封面内显示旋转、破图或错误图标 |
| `Loaded` | 原始封面 | 仅执行等比裁切和必要的色彩空间转换，不叠加默认品牌滤镜 |
| `Missing` | `artworkPlaceholder` | 使用与 Loading 完全相同的封面占位；相邻标题与元数据正常显示 |
| `Error` | `artworkPlaceholder` | 可在数据层重试；不在封面内暴露 URL、异常码或技术文案 |
| `Unavailable` | `artworkPlaceholder` | 可由相邻文本说明不可用原因；不只依赖降低透明度表达状态 |

- Loading → Loaded 的过渡遵循 05A Motion Tokens；在 05A 冻结前不在本节重复定义时长。
- Reduced Motion 下不使用 Shimmer、扫光或循环缩放；静态占位必须能够独立表达加载中。
- 加载失败不得导致容器消失、列表重排或点击目标缩小。
- Loading 与 Missing 共用封面占位图；两者由相邻内容和 Semantics 区分，禁止再增加唱片、山景、破图等第二套缺省封面图形。

叠加内容与颜色：

- 标准封面不内嵌标题、歌手名、播放按钮或品牌水印；文字信息放在组件的独立文本区域。
- 必须叠加 Badge 或状态时，使用独立容器、`artworkOverlayInset` 和可验证对比度；不得假设封面任意区域都能承载可读文字。
- Artwork 颜色不反向修改 Foundation Semantic Color。基于封面的动态主题属于 Product/Player 层，不在当前 Foundation 中启用。
- 不改变封面饱和度、色相或明暗来“匹配主题”；Dark 与 AMOLED 主题继续显示同一媒体内容。

可访问性与语义：

- 封面旁已显示专辑名与歌手名时，Artwork 作为装饰图像，`contentDescription = null`，避免屏幕阅读器重复朗读。
- Artwork 独立承担内容识别时，描述使用本地化的“{专辑名}，{歌手名}，专辑封面”；不得朗读文件名、URL 或缓存键。
- 缺失与错误占位不单独朗读“图片加载失败”；若错误需要用户处理，由相邻状态文本或可操作控件表达。
- 封面本身只有在承担明确操作时才可点击；点击目标与焦点语义由所属组件提供，不给纯图片添加伪按钮语义。

验收条件：

- 正方形、超宽、超高源图都在同一 1:1 容器内稳定显示，且无拉伸。
- Loading、Missing、Error 与 Loaded 共享尺寸、Shape 和布局占位。
- Light、Dark、AMOLED 下占位符均使用对应 Semantic Color，不写死视觉稿颜色。
- 200% 字体缩放不改变 Artwork 比例，也不会遮挡相邻标题或状态文案。
- 状态：**已冻结**。
- 辅助视觉证据：`design/approved/foundation/04b-album-artwork.png`
- 已确认矢量源：`design/approved/foundation/04b-album-artwork-source.svg`

### 05 — Motion, States & Accessibility

#### 05A — Motion Tokens

Resonote 使用 Material 3 Standard Motion Physics，不再把旧版 Easing + Duration 作为主要动画模型。Material3 1.4.0 内部组件已使用 Standard Motion Scheme，但 `MotionScheme`、`MaterialTheme.motionScheme` 与其六类 Spec 在该版本仍是 `internal`；Resonote 因此提供同语义的公开 Alias，并使用 1.4.0 Standard Motion Token 参数构建 Spring。组件只能引用 Alias，不能访问内部 API 或自行散落参数。

依据：[M3 Motion Physics System](https://m3.material.io/styles/motion/overview/how-it-works)、[M3 Motion Specs](https://m3.material.io/styles/motion/overview/specs)、[Compose Material3 1.4.0](https://developer.android.com/jetpack/androidx/releases/compose-material3)、[Customize animations](https://developer.android.com/develop/ui/compose/animation/customize)。公开 `MotionScheme` API 从 1.5 Alpha 才提供，不属于当前实现基线。

Motion Scheme：

| Property | Value | 规则 |
|---|---|---|
| Product scheme | `ResonoteMotionScheme.standard` | Resonote 公开 Alias；参数镜像 Material3 1.4.0 Standard Motion Token |
| Spatial motion | Spring | 位置、尺寸、边界、Shape、旋转等空间变化 |
| Effects motion | Spring without overshoot | 颜色、Alpha 与不改变边界的视觉属性 |
| Interruption | Preserve current value and velocity | 新目标从当前帧继续，不先复位再播放 |
| Initial velocity | Gesture velocity when available | Drag、Fling、Predictive Back 不丢弃用户输入速度 |
| Arbitrary delay | `0ms` | 用户操作反馈禁止人为等待；Stagger 必须由组件规范明确授权 |

标准 Token：

| Resonote Token | Stable 1.4.0 implementation | QA 参考完成时间 | 默认用途 |
|---|---|---:|---|
| `motionInstant` | `snap()` | `0ms` | 初始同步、不可动画的状态切换、Reduced Motion |
| `motionEffectsFast` | `spring(dampingRatio = 1.0f, stiffness = 3800f)` | `150ms` | Pressed/Selected 颜色、Icon Tint、短 Alpha 变化 |
| `motionEffectsDefault` | `spring(dampingRatio = 1.0f, stiffness = 1600f)` | `200ms` | 内容淡入淡出、容器颜色与 Artwork Crossfade |
| `motionEffectsSlow` | `spring(dampingRatio = 1.0f, stiffness = 800f)` | `300ms` | 大范围内容或全屏效果变化 |
| `motionSpatialFast` | `spring(dampingRatio = 0.9f, stiffness = 1400f)` | `350ms` | Button、Switch、Chip、Indicator 等小组件的尺寸或 Shape 变化 |
| `motionSpatialDefault` | `spring(dampingRatio = 0.9f, stiffness = 700f)` | `500ms` | Sheet、局部容器、Navigation Rail 等部分屏幕变化 |
| `motionSpatialSlow` | `spring(dampingRatio = 0.9f, stiffness = 300f)` | `750ms` | 全屏或大范围空间转换 |

QA 参考完成时间来自 M3 Standard Spring 的跨平台转换表，只用于录屏检查、性能预算和非 Spring 平台近似；Compose 实现必须调用对应 `FiniteAnimationSpec`，不得把表中毫秒数直接改写成 `tween()`。Spring 没有固定曲线终点，实际完成时间可随距离、初速度、设备类别和中断发生变化。

场景映射：

| 场景 | Spatial | Effects | 规则 |
|---|---|---|---|
| Immediate state sync | `motionInstant` | `motionInstant` | 首帧恢复、权限结果与不可见状态同步，不播放入场表演 |
| Component feedback | `motionSpatialFast` | `motionEffectsFast` | 高频小组件；按下反馈在输入帧立即开始 |
| Content replacement | — | `motionEffectsDefault` | Artwork、文本或同一区域内容替换；新旧内容语义不能同时可聚焦 |
| Container reveal | `motionSpatialDefault` | `motionEffectsDefault` | Sheet、Dialog、展开容器；空间与效果使用同一 Transition 协调 |
| Page transition | `motionSpatialSlow` | `motionEffectsSlow` | 仅在确实移动全屏内容时使用；简单目的地切换优先 Default |
| Gesture settle/cancel | `motionSpatialDefault` | 按需 | 手势期间直接跟随 Progress，释放后从当前值和速度收敛 |
| Continuous progress | 组件专用 | 组件专用 | 只用于不确定进度；不得用装饰性无限动画替代真实状态 |

进入、退出与可逆性：

- 同一对象在屏幕内移动或变形时保持视觉连续性，不交叉复制两个可见实例。
- Enter 与 Exit 必须共享同一状态机；退出结束后再移除内容，避免 Alpha 为 0 的元素继续占据焦点或读屏顺序。
- 临时离屏内容应能沿相同空间关系返回；永久删除可以更快结束，但不得早于状态确认或撤销入口建立。
- Predictive Back 在手势阶段由系统 Progress 驱动，不运行独立 Tween；取消或提交后使用当前值继续收敛。
- 动画中再次点击、切换目的地或改变窗口尺寸时，立即 Retarget；禁止等待上一段动画完成。
- Insets、IME 与窗口尺寸变化参与同一布局状态更新，不以第二套延迟动画追赶最终布局。

属性与实现边界：

- 只动画能解释状态变化的属性。Position、Size、Shape 使用 Spatial；Color、Alpha 使用 Effects，不可混用来获得额外弹跳。
- Scale 只用于不影响可读性的短暂组件反馈；正文不得以缩放代替排版重算。
- 文本发生位置、缩放或旋转时使用 `TextMotion.Animated`；静止后仍需保持清晰可读。
- 使用 Alpha 隐藏内容时必须同步处理 Composition 与 Semantics；不可见但仍可点击或读屏视为缺陷。
- 同一时刻避免无层级关系的多处运动竞争注意力；页面级 Motion 优先于局部装饰 Motion。
- 不使用弹跳表达 Error、危险操作或系统阻断；这些状态以明确文案、颜色和 Icon 表达。

Reduced Motion 与系统设置：

- 尊重 Compose `MotionDurationScale`。`scaleFactor = 1f` 按正常速度；大于 `1f` 按系统倍率延长；`0f` 时 Motion 在下一帧到达终态。
- 当系统动画缩放为 `0` 或未来应用级 Reduced Motion 开启时，Spatial、Effects、Crossfade、Shimmer 与装饰性循环统一映射为 `motionInstant`。
- Reduced Motion 不能删除信息、跳过确认、改变导航结果或缩短内容可读时间；它只移除运动过程。
- 不确定进度在 Reduced Motion 下使用静态 Progress/Loading 状态与读屏语义，不使用旋转、扫光或脉冲。
- 自动播放的非必要动画禁止默认循环；必要循环必须在内容离屏、生命周期停止或任务完成时终止。

实现与依赖：

- 由单一 `ResonoteMotionScheme` 提供六类 `FiniteAnimationSpec`；组件不得各自硬编码 Spring、Tween 或 Duration。
- Alias 参数以 `material3-android-1.4.0-sources.jar` 中的 `StandardMotionTokens` 为锁定依据；升级 Material3 时必须重新核对，不能静默继承新版行为。
- 未来只有在项目升级到提供公开 `MotionScheme` 的稳定版并完成录屏回归后，才可将 Alias 实现切换到 `MaterialTheme.motionScheme`；当前不得依赖 1.5 Alpha API。
- Legacy Easing + Duration 只用于无法使用 Spring 的外部平台或既有过渡兼容，不得与 Compose Spring 在同一交互中叠加。

验证矩阵：

- 在系统动画缩放 `0× / 1× / 10×` 下验证终态、可操作性和读屏顺序。
- 在运动进行中连续 Retarget、快速返回、切换窗口尺寸和开合 IME，检查无跳回起点、无闪烁和无残留层。
- 检查 60Hz 与高刷新率设备上的掉帧；Motion 不得阻塞输入、图片解码或导航状态提交。
- 录屏验证 Small / Partial-screen / Full-screen 的相对速度满足 Fast < Default < Slow，Effects 不产生 Overshoot。
- Motion 不生成静态规范 PNG；在组件实现阶段使用可交互原型或录屏作为验证证据。
- 状态：**已冻结**。

#### 05B — Interaction States

Interaction State 分为瞬时交互状态与持续业务状态。瞬时状态通过 State Layer 表达；Selected、Loading、Error 等持续状态必须改变组件内容、容器、图标、标签或语义中的至少两项，不能只改变颜色。

依据：[M3 States Overview](https://m3.material.io/foundations/interaction/states/overview)、[M3 State Layers](https://m3.material.io/foundations/interaction/states/state-layers)、[Applying states](https://m3.material.io/foundations/interaction/states/applying-states)。

State Layer Token：

| Token | Opacity | Layer Color | 规则 |
|---|---:|---|---|
| `stateHover` | 8% | 当前内容色 / On Color | 鼠标或精确指针悬停 |
| `stateFocus` | 10% | 当前内容色 / On Color | Keyboard、D-pad 或 Voice Focus |
| `statePressed` | 10% | 当前内容色 / On Color | Tap、Click、Keyboard Activate 的 Ripple/Overlay |
| `stateDragged` | 16% | 当前内容色 / On Color | 可拖动对象移动期间 |
| `stateLayerIconSize` | 40dp | — | Icon Button 的圆形或匹配 Shape 状态层 |
| `stateTouchTarget` | 48dp | — | 状态层不等同最小交互目标 |
| `focusIndicatorWidth` | 2dp | `primary` | Resonote Accessibility Extension；State Layer 之外的键盘/D-pad 可见焦点环 |
| `focusIndicatorOffset` | 2dp | — | 焦点环不遮挡组件边界与内容 |

- State Layer 位于 Container 之上、Content 之下；颜色来自当前 Icon 或 Label 的内容色。
- 同一组件同一时刻只绘制一个 State Layer。多个瞬时状态并存时按 `Dragged > Pressed > Focused > Hovered` 选择 Overlay；Focus Ring 可与 Pressed/Hovered 同时保留。
- State Layer 必须裁切到组件 Shape；Ripple 可有界或无界，但不得侵入相邻组件的可点击区域。
- Overlay 的出现与消失使用 `motionEffectsFast`；输入帧立即开始，不添加 Delay。

持续状态：

| State | Visual Indicators | Behavior / Semantics |
|---|---|---|
| `Enabled` | 标准 Container + Content | 可聚焦、可操作，暴露正确 Role 与 Action |
| `Selected` | Selected Container/Content + Indicator/Icon/Label 至少一项 | 暴露 `selected`、`toggleableState` 或 `stateDescription`，不只换颜色 |
| `Disabled` | `onSurface` 38% Content；有容器组件使用 `onSurface` 12% Container | 不响应 Hover/Focus/Press/Drag，不进入 Focus 顺序；若原因重要，优先保留可用控件并解释原因 |
| `Loading` | 保持原尺寸 + Progress 或明确 Loading Label | 阻止重复提交；暴露 Progress/State Description；完成前不伪装成 Disabled |
| `Error` | Error Color + Icon/Supporting Text/Outline 至少一项 | 使用 `error()` Semantics；说明问题及恢复方式，不只显示红色 |
| `Success` | Confirm Icon/Message + 状态文字或稳定结果 | 仅在结果无法从内容本身理解时显示，不自动抢走焦点 |
| `Unavailable` | 明确说明 + 替代操作或返回路径 | 与权限、离线或资源缺失区分，不能统一做成 Disabled 灰色 |

Disabled 边界：

- Disabled 仅用于当前上下文确实不可操作且用户无需立即知道原因的 Action、Selection、Input 控件。
- Navigation、Dialog、Sheet、Badge、Tooltip 等结构或沟通组件不整体套用 Disabled；其内部不可用 Action 单独处理。
- 主要操作若不可用原因可以解决，优先保持控件可聚焦/可点击，在激活后展示校验与修复信息，而不是静默禁用。
- Disabled 不要求达到 Enabled 的对比度，但必须与背景和 Enabled 状态可区分；不可通过 `alpha` 让文本完全不可读。

Focus、Input 与组合状态：

- Keyboard/D-pad Focus 必须同时具有 `stateFocus` 与 2dp Focus Indicator，不能只依赖 Ripple 或系统光标。
- Hover 不改变布局、Shape 或文本；同一窗口只有指针所在元素显示 Hover。
- Pressed 反馈先于业务结果，操作失败也必须结束 Pressed 状态并进入 Error/Message 状态。
- Selected 是持久状态，允许叠加 Hover、Focus 或 Pressed；瞬时 Overlay 使用 Selected 状态下的 Content Color。
- Drag 只应用于明确可拖动的 Card、Chip、List Item 或 Slider；不可拖动组件不得仅为视觉趣味进入 Dragged 状态。
- Touch、Mouse、Keyboard、D-pad、Voice 触发同一 Action 时，业务结果一致；只允许瞬时反馈方式不同。

Loading 与异步操作：

- Loading 不改变组件宽高，不把按钮文字替换成宽度不稳定的省略号。
- 单次提交进入 Loading 后屏蔽重复提交，但保留当前焦点与可读状态；允许取消时必须提供独立 Cancel Action。
- 不确定进度使用 Progress Indicator + `stateDescription`；确定进度提供 `progressBarRangeInfo`。
- 超过局部可感知等待时间时显示上下文文案；失败后进入 Error/Retry，不无限保持 Loading。
- Reduced Motion 下使用静态 Progress/Label，不运行旋转、脉冲或 Shimmer。

验收条件：

- 每个交互组件至少验证 Enabled、Pressed、Focused、Disabled；可选择组件增加 Selected，可异步组件增加 Loading/Error。
- Light、Dark、AMOLED 下 Overlay 使用当前 On Color 计算，不直接复用视觉稿 RGBA。
- Focus Indicator 在所有主题、图片背景和 200% 字号下清晰可见且不被裁切。
- Disabled 节点不出现在操作焦点顺序；Loading、Error 与 Selected 的 Semantics 与视觉状态同步。
- 状态切换不改变触控区、不导致文字跳动、不留下透明但可点击的节点。
- 状态：**已冻结**。
- 辅助视觉证据：`design/approved/foundation/05b-interaction-states.png`
- 已确认矢量源：`design/approved/foundation/05b-interaction-states-source.svg`

#### 05C — Accessibility Foundation

Accessibility 是 Foundation 的跨章节约束，不是组件完成后的附加检查。本节引用已冻结的 Color、Typography、Layout、Icon 与 Motion Token，并定义实现必须保留的语义和输入路径。

核心阈值：

| Requirement | Threshold / Rule | 规范源 |
|---|---|---|
| Normal text contrast | WCAG AA `≥ 4.5:1` | 01H |
| Large text contrast | WCAG AA `≥ 3:1` | 01H；Large = 18pt Regular 或 14pt Bold 及以上 |
| UI component / graphic contrast | `≥ 3:1` | 边界、Focus、Icon 与必要图形 |
| Touch target | `≥ 48dp × 48dp` | 02B / 04A；视觉 Glyph 可小于 Target |
| Font scale | 支持 `200%` | 02B；不裁切、不重叠、不强制缩字 |
| Motion scale | `0× / 1× / 10×` 可用 | 05A |
| Color independence | 至少第二视觉或文字指标 | 01H / 05B |

Semantics：

- 优先使用 Material/Compose 原生组件与 `clickable`、`toggleable`、`selectable` 等带语义的 Modifier；自定义组件必须补齐 Role、Action、State 与 Label。
- 一个逻辑控件对应一个清晰 Semantics Node。Icon + Label Button 合并朗读，不分别创建重复焦点；独立 Action 不得错误合并。
- 装饰图片和重复 Icon 使用 `contentDescription = null`；有语义图片使用本地化、面向任务的描述，不朗读文件名或视觉细节清单。
- Toggle、Selection、Expansion、Progress、Loading 与 Error 使用 `stateDescription`、`selected`、`toggleableState`、`progressBarRangeInfo` 或 `error()` 表达真实状态。
- Dialog、Sheet 与自定义 Pane 提供 `paneTitle`；Snackbar 与非阻断更新使用 `LiveRegionMode.Polite`。`Assertive` 只用于必须立即中断朗读的安全或时间敏感事件。
- Live Region 不用于高频计时、滚动位置或逐帧播放进度，避免持续打断用户。

阅读顺序与焦点：

- Semantics 顺序遵循视觉与任务顺序：页面标题 → 主要内容 → 主要操作 → 次要/危险操作；RTL 下逻辑顺序随布局方向，而不是硬编码坐标。
- 新页面将焦点放在页面标题或首个主要内容，不自动跳到危险操作；Dialog/Sheet 打开后 Focus 被约束在当前 Pane，关闭后返回触发控件。
- Keyboard、D-pad、Switch Access 与 TalkBack 必须能到达所有操作；不可见、Disabled、被 Modal 遮挡或 Alpha 为 0 的节点不得保留焦点。
- Focus Traversal 默认依赖结构顺序；只有布局结构无法表达正确顺序时才显式覆盖，不用大量手工序号修补错误层级。
- 焦点外观遵循 05B Focus Indicator；焦点移动不得自动触发破坏性操作、提交或导航。

输入与手势替代：

- 所有 Tap Action 提供 TalkBack/Keyboard 等价路径；双击、长按、拖动、滑动等复杂手势必须提供可发现的按钮、菜单或 `CustomAccessibilityAction`。
- 不依赖 Hover 才能发现必要信息；Tooltip 同时支持键盘 Focus 与长按，并不替代永久可见的关键 Label。
- 目标区域不因视觉尺寸变小。相邻 Touch Target 重叠时优先调整布局，不通过事件优先级隐藏冲突。
- Drag & Drop 提供开始、移动、放置、取消的可访问操作及位置反馈；仅装饰性排序动效不能成为唯一状态线索。

文字、内容与布局：

- 使用 02A Type Token 与 `sp`，尊重系统 Font Scale；正文允许换行，组件高度随内容增长。
- 200% 下不截断关键信息；省略号只用于可从 Detail/Tooltip 完整访问的非关键摘要，不用于错误、确认、价格或主要操作。
- 中英文混排、长专辑名、长艺术家名和无空格字符串必须经过 Compact/Medium/Expanded 验证。
- 屏幕旋转、分屏、IME 与 Insets 变化不丢失焦点、输入内容、错误说明或当前滚动上下文。

反馈、时间与恢复：

- 成功、错误、权限、离线和加载状态同时提供文字/语义与视觉指标；不以颜色、音效、震动或动画作为唯一反馈。
- Snackbar 等临时信息若包含 Action，显示时间必须足够并由 TalkBack 可访问；关键错误不得只存在于自动消失的 Snackbar。
- 超时、会话失效或自动关闭必须提前提示，并在合理情况下允许延长；媒体播放时长不属于本 Foundation 的超时控件规范。
- 破坏性操作提供确认或可撤销路径；焦点回到可继续任务的位置，而不是页面顶部。

验证：

- 使用 TalkBack、Switch Access、外接 Keyboard/D-pad、Accessibility Scanner 与 Layout Inspector 检查真实 Semantics Tree。
- 自动化测试覆盖 Role、Label、State、Action、Focus Order、Disabled/Hidden 节点和 Progress/Error Semantics；测试 Unmerged Tree 仅用于诊断。
- 执行 11 Validation Matrix：三主题、200% Font Scale、五种 Window Size、RTL、中英文长内容、Motion 0× 与全部交互状态。
- 不生成独立静态 PNG；可访问性证据来自组件实现截图、Semantics 测试与辅助技术实测记录。
- 依据：[Compose Semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)、[Make composables accessible](https://developer.android.com/guide/topics/ui/accessibility/composables)、[Inspect and debug accessibility](https://developer.android.com/develop/ui/compose/accessibility/inspect-debug)。
- 状态：**已冻结**。
