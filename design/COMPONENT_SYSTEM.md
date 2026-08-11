# Resonote Component System

> 状态：执行基线；06–08 已冻结  
> 更新日期：2026-08-10  
> 规范范围：06 Core Components、07 Navigation & Feedback、08 Music Components  
> Foundation 依赖：[FOUNDATION.md](./FOUNDATION.md)  
> Material 基线：`androidx.compose.material3:material3:1.4.0` 稳定版 Baseline

## 0. 交付原则

- 本文档是组件 Token、尺寸、行为、状态、语义与适配规则的规范源；视觉稿只用于审阅形态和关系。
- 颜色只引用 Foundation Semantic Role，字号只引用 Type Token，圆角、间距、Elevation、Motion 与 State Layer 不在本文另造同义 Token。
- Material 3 原生组件能够满足规范时优先使用；自定义封装只能增加 Resonote 语义与一致默认值，不能破坏 Compose 原生 Semantics。
- 所有交互组件最小 Touch Target 为 48dp，200% 字号下允许高度增长，不得裁切或强制缩字。
- Player 专属的 MiniPlayer、Queue、Playback Progress、Pager、歌词高亮和播放页布局不属于本文范围。

## 1. 通用组件合同

每个交互组件必须提供：

| Contract | 要求 |
|---|---|
| Anatomy | Container、Content、Optional Icon、State Layer 与 Focus Indicator 层级明确 |
| State | 至少 Enabled、Pressed、Focused、Disabled；按能力增加 Selected、Loading、Error |
| Semantics | Role、Label、State、Action 与可选 Error/Progress 完整且本地化 |
| Touch | `touchTargetMin / 48dp`，相邻目标不重叠 |
| Typography | 只使用 02A Token；200% 字号时弹性增高和换行 |
| Motion | 只使用 05A Motion Token；Reduced Motion 映射 `motionInstant` |
| Theme | Light、Dark、AMOLED 使用相同 Semantic Role，不在组件内写 Hex |
| Layout | Compact、Medium、Expanded、Large、Extra-large 保持同一语义和操作结果；Large/Extra-large 复用 Expanded 拓扑 |

- Icon + Label 作为一个不可拆分的 Content Row 在容器内整体居中；二者共享垂直中心线，不能只把 Label 居中后再把 Icon 贴到左侧。
- 组件内容必须保留规范 Padding。内容放不下时按组件规则增高、换行、扩大到允许宽度或更换组件，不得越出 Container、覆盖边界或挤入相邻 Target。

## 06 — Core Components

### 06A — Buttons & Actions

#### Button

| Property | Value |
|---|---:|
| Visual min height | 40dp |
| Touch min height | 48dp |
| Min width | 58dp（`ButtonDefaults.MinWidth`） |
| Horizontal padding | 24dp Start/End（无 Icon）；16dp Start / 24dp End（有 Leading Icon） |
| Content gap | `space2 / 8dp` |
| Icon | 18dp（`ButtonDefaults.IconSize`） |
| Label | `labelLarge` |
| Shape | `shapeFull / CornerFull` |

Variant：

| Variant | Container | Content | 用途 |
|---|---|---|---|
| Filled | `primary` | `onPrimary` | 页面唯一或最主要提交操作 |
| Tonal | `secondaryContainer` | `onSecondaryContainer` | 中等强调、可逆操作 |
| Outlined | Transparent + `outline` 1dp | `primary` | 次要操作与并列选择 |
| Text | Transparent | `primary` | 低强调、Dialog/Inline Action |
| Destructive | `error` 或 Transparent | `onError` / `error` | 删除等破坏性操作；必须明确 Label |

- 同一区域最多一个 Filled Primary；多个并列操作按 Tonal → Outlined → Text 降级。
- 默认 Button 使用 Material3 1.4.0 Baseline `CornerFull`。Loading 保持原宽高，Icon 位置显示 Progress，Label 使用明确进行时文案。
- 带 Icon 的 Button 使用 18dp Icon、8dp Gap，并将完整 Content Row 相对 Container 居中；Label 不得越过内容 Padding。
- Icon 不独立提供 `contentDescription`，Button 使用合并后的文字 Label；纯 Icon Action 使用 Icon Button。
- Button 行无法容纳 200% 文本时允许换行或改为纵向 Action Group，不缩小文字。

#### Icon Button

| Property | Value |
|---|---:|
| Touch target | 48dp × 48dp |
| State layer | 40dp × 40dp |
| Glyph | `iconDefault / 24dp` |
| Shape | `shapeFull / CircleShape` |

- Icon-only Action 必须有本地化 `contentDescription` 与 Tooltip；高风险或陌生操作改用带 Label 的 Button。
- Standard、Filled、Tonal、Outlined 只改变 Container/Content，不改变 Glyph、Touch Target 或语义。
- Toggle Icon Button 使用 Selected Container + Filled/Check Indicator 至少两个信号，并暴露 Toggle State。

### 06B — Inputs, Selection & Metadata

#### Text Field

| Property | Value |
|---|---:|
| Min height | 56dp；多行与 200% 字号弹性增长 |
| Horizontal padding | `space4 / 16dp` |
| Vertical padding | `space2 / 8dp` 最小 |
| Shape | `shapeExtraSmall / 4dp` |
| Outline | 1dp Default；2dp Focus/Error |
| Input text | `bodyLarge` |
| Label | `bodySmall` / Floating Label |
| Supporting text | `bodySmall` |
| Icon | 24dp Glyph / 48dp Interactive Target |

- 默认使用 Outlined Text Field；Label 永久可访问，不以 Placeholder 代替 Label。
- Error 使用 Error Outline + Icon/Supporting Text + `error()` Semantics；Supporting Text 说明如何修复。
- Prefix、Suffix 与 Counter 不能挤压主要输入；200% 下移到独立行或隐藏非必要装饰。
- IME Action、Keyboard Type、单/多行、Max Length 与 Validation 时机必须由字段语义明确配置。

#### Chip

| Property | Value |
|---|---:|
| Visual height | 32dp；文字换行时弹性增长 |
| Touch target | 48dp |
| Horizontal padding | 12dp；带 Icon 时前侧 8dp |
| Gap | 8dp |
| Icon | 18dp |
| Label | `labelLarge` |
| Shape | `shapeSmall / 8dp` |

- Assist、Filter、Input、Suggestion 通过行为区分，不靠任意颜色区分。
- Filter Chip 暴露 Selected；Selected 使用 Container + Check/Icon 两个指标。Input Chip 的 Remove 是独立 Action，Target 不与主体重叠。
- Icon + Label 使用 18dp Icon、8dp Gap，并作为完整 Content Row 居中；文字较长时 Chip 扩宽或换行，不裁掉末尾或突破 Container。
- Chip Group 允许换行，不横向压缩 Label；长 Filter 集合优先打开 Filter Sheet。

#### Tag

| Property | Value |
|---|---:|
| Min height | 24dp |
| Padding | 4dp Vertical / 8dp Horizontal |
| Label | `labelSmall` |
| Shape | `shapeExtraSmall / 4dp` |

- Tag 是非交互元数据，不绘制 State Layer、不进入 Focus 顺序。可点击筛选必须使用 Chip。
- Tag 不用纯色区分唯一含义；质量、来源或状态同时提供文字。

#### Badge

| Type | Size | Typography |
|---|---:|---|
| Dot | 6dp | 无文字 |
| Number | Min 16dp 高，Horizontal Padding 4dp | `labelSmall` |

- Badge 附着于宿主并由宿主语义合并朗读；不单独可点击。
- `99+` 为默认数字上限显示，完整数量通过宿主 State Description 提供。
- Dot 只能表示“有更新”等已有上下文含义，不能单独承载错误严重度。

### 06C — Tabs, Segments & App Bar

#### Tabs

| Property | Value |
|---|---:|
| Min height | 48dp |
| Horizontal padding | 16dp |
| Label | `titleSmall` |
| Icon | 24dp；与 Label Gap 8dp |
| Active indicator | 3dp 高；`primary` |

- Primary Tabs 表达同层级内容视图；固定 Tabs 用于少量短 Label，Scrollable Tabs 用于更多或更长 Label。
- Selected 同时使用 Indicator + Selected Semantics；Tab 切换不创建新的 Navigation Back Stack。
- 200% 下优先 Scrollable、Label 换行或切换 Segmented/List 方案，不横向压缩文字。

#### Segmented Control

| Property | Value |
|---|---:|
| Min visual height | 40dp |
| Touch target | 48dp |
| Segment padding | 12dp Horizontal |
| Label | `labelLarge` |
| Outer shape | `shapeFull / CornerFull` |
| Divider | `borderHairline / 1dp` |

- 用于 2–5 个互斥、短 Label 选项；每个 Segment 等高，宽度可等分或内容驱动但同组一致。
- Selected 使用 Container + Check/Icon/Weight 至少两个指标，并暴露 Single Selection Semantics。
- Selected Check 使用 18dp，与 Label 保持 8dp Gap，整组内容在 Segment 内居中。
- 选项过多、Label 过长或 200% 无法容纳时改用 Radio List/Sheet。

#### Top App Bar

Top App Bar 使用 Material3 1.4.0 稳定版 `TopAppBar` 默认尺寸、Insets、Typography、Color 与
Scroll Behavior，不复制其内部 Token。Resonote 封装只提供 Title、Navigation Icon、Actions
Slot 与一致的调用约定；交互 Icon 使用 Resonote Icon Button 以保持可访问名称、Tooltip 与
最小 Touch Target。

- 默认 Small Top App Bar；Medium/Large 只用于需要明确页面层级的内容页，不用于高频列表工具栏。
- 最多保留 2 个直接 Action，其他进入 Overflow；标题优先完整显示，超长时允许一行省略且页面正文提供完整标题。
- Container 与 Scrolled Container 使用 Material 默认 Semantic Role；确有产品差异时才增加 Resonote Override。
- App Bar 本身不套 Hover/Pressed；内部 Action 各自应用 05B State。

### 06D — Progress, Snackbar & Modal Surfaces

#### Progress

- Circular 默认 40dp；Inline 可使用 24dp；Linear 高 4dp、占满所属内容宽度。
- Determinate 暴露 `progressBarRangeInfo`；Indeterminate 提供 Loading State Description。
- Reduced Motion 使用静态 Indicator/Label，不以旋转或扫光作为唯一 Loading 信息。

#### Snackbar

| Property | Value |
|---|---:|
| Min height | 48dp；多行弹性增长 |
| Padding | 16dp |
| Shape | `shapeExtraSmall / 4dp` |
| Surface | `inverseSurface` / `inverseOnSurface` |
| Elevation | Level 3 |

- 文案简短说明结果；最多一个 Text Action，可附 Dismiss Icon。关键错误不只放 Snackbar。
- 使用 `LiveRegionMode.Polite`；Action 可聚焦，超时尊重无障碍服务与内容长度。

#### Dialog

| Property | Value |
|---|---:|
| Width | Min 280dp / Max 560dp；Compact 保持 24dp 外边距 |
| Padding | 24dp |
| Section gap | 16–24dp |
| Shape | `shapeExtraLarge / 28dp` |
| Surface / Elevation | `surfaceContainerHigh` / Level 3 |
| Scrim | `scrim` 32% |

- 标题、正文、Action 顺序稳定；主要确认靠近阅读终点，破坏性操作文字明确。
- 200% 或窄宽下 Action 改为纵向；Dialog 内容过长时正文区滚动，标题与 Action 保持可见。
- 提供 `paneTitle`，开启后限制 Focus，关闭后返回触发点；非必要点击 Scrim 不自动丢弃未保存内容。

#### Bottom Sheet

| Property | Value |
|---|---:|
| Compact outer margin | 16dp |
| Max width | 640dp；宽屏居中或改用 Side Sheet/Dialog |
| Top shape | `shapeExtraLarge / 28dp` |
| Content padding | 16dp Compact / 24dp Medium+ |
| Drag handle | 32dp × 4dp |
| Surface / Elevation | `surfaceContainerLow` / Level 1 |
| Scrim | `scrim` 32%（Modal） |

- Modal Sheet 用于短时任务；Persistent Sheet 只有在宽屏主任务确有并行内容时使用。
- Handle 只有可拖动时显示，并提供展开/折叠/关闭的可访问 Action；不能只靠 Drag 手势。
- IME 打开时当前字段和提交操作保持可见；Sheet 状态变化遵循 05A 并支持 Predictive Back。

06 状态：**已冻结**。  
辅助视觉证据：`design/approved/components/06-core-components.png`  
矢量源：`design/approved/components/06-core-components-source.svg`

## 07 — Navigation & Feedback Patterns

### 07A — Adaptive Primary Navigation

Resonote 使用 Material3 Adaptive Navigation Suite 1.4.0 稳定版作为 Primary Navigation 基线。
`WindowAdaptiveInfo` 综合 Width、Height 与 Posture，默认在 Navigation Bar 与 Navigation Rail
之间切换；容器尺寸、Indicator、Icon、Label、Insets 与交互行为沿用同版本 Material Token，
不在 Resonote 重复冻结内部数值。

- Design System 提供无状态 Item DSL，不持有 Destination ID、Selected State 或 Back Stack，也不校验目的地数量。
- 同一组 Destination、Label、Icon 与 Selected State 在窗口变化前后保持一致；窗口只改变呈现形态。
- Navigation Bar 通常适合少量顶层目的地；Rail 可承载更宽松的信息架构。具体数量属于 App IA 决策，不是组件运行时合同。
- Selected 使用 Material 默认 Indicator、Color 与 Selected Semantics；Resonote 只映射 Semantic Color，不覆盖内部布局。
- Label 默认提供且本地化，不能只靠 Icon 猜测主导航含义；装饰 Icon 不重复提供 Content Description。
- Drawer、不同窗口目的地集合、“更多”与自动裁剪属于显式产品 IA 扩展，第一阶段不由 Design System 推断。
- Player、Queue 与 Lyrics 不作为 Foundation Primary Destination 预设；是否进入主导航由 Product IA 决定。

### 07B — Page Entry, Back & Search

| Search Bar Property | Value |
|---|---:|
| Collapsed height | 56dp |
| Container | `surfaceContainerHigh` |
| Elevation | Level 3 |
| Shape | `shapeFull / CornerFull` |
| Input / Placeholder | `bodyLarge` |
| Leading / Trailing Icon | 24dp Glyph / 48dp Interactive Target |

- Root Destination 之间切换保持各自 Scroll/Filter 状态；层级内 Detail 使用标准 Back，并支持 Predictive Back。
- Back 不等于 Close：返回上一层使用 Back，退出临时 Modal 使用 Close，取消编辑使用明确 Cancel/Discard 流程。
- Search Entry 使用 Top App Bar Action 或 56dp、`shapeFull` 的 Material3 Search Bar；搜索页面获得明确标题、Back、Clear 和 Submit/IME Action。
- 输入采用 300ms Debounce 仅限网络/数据库查询触发，不延迟本地字符显示、Clear 或键盘反馈。
- 空查询显示 Recent/Suggestion，查询中显示 Loading，零结果显示 Query + Clear/Filter Action，错误显示 Retry。
- Filter 使用 Chip 作为摘要，复杂筛选进入 Sheet/Dialog；应用后保留可见条件和清除全部入口。

### 07C — Feedback & Recovery

| Situation | Primary Pattern | 必须包含 |
|---|---|---|
| Inline validation | Supporting Text | 问题、修复方式、Error Semantics |
| Reversible result | Snackbar | 结果 + Undo/Action |
| Blocking decision | Dialog | 清晰影响 + Confirm/Cancel |
| Short task / choices | Bottom Sheet | Title、Selection、Close/Apply |
| Page empty | Empty State | 原因 + 下一步；不只插画 |
| Page error | Error State | 可理解原因 + Retry/Alternate Path |
| Page loading | Skeleton/Progress | 稳定布局 + Loading Semantics |
| Permission | Contextual Rationale → System Prompt | 用途、拒绝后影响与替代路径 |

- Permission Prompt 只在用户触发相关功能时请求；首次拒绝后不循环弹窗，永久拒绝提供 Settings 路径。
- Offline、Empty、No Result、Error 与 Permission Denied 是不同状态，文案和恢复操作不能共用“Something went wrong”。
- 页面级状态保留 Top App Bar 与 Primary Navigation，使用户始终有离开和恢复路径。
- 加载成功/失败不改变主要布局骨架；异步结果与当前 Query/Filter 绑定，过期结果不得覆盖新请求。

07 状态：**已冻结**。  
辅助视觉证据 A-1：`design/approved/components/07a-1-compact-medium-navigation.png`  
矢量源 A-1：`design/approved/components/07a-1-compact-medium-navigation-source.svg`  
辅助视觉证据 A-2：`design/approved/components/07a-2-expanded-navigation.png`  
矢量源 A-2：`design/approved/components/07a-2-expanded-navigation-source.svg`  
辅助视觉证据 B：`design/approved/components/07b-search-recovery.png`  
矢量源 B：`design/approved/components/07b-search-recovery-source.svg`

## 08 — Music Components / Resonote Extension

本节是基于 Material3 Color、Typography、Shape、State 与 Accessibility Token 构建的 Resonote 产品扩展，不宣称为 Material3 官方组件。本节只定义音乐资料浏览组件，不定义播放控制、进度、队列、歌词或 Player 页面。

### 08A — Album Tile

| Property | Value |
|---|---:|
| Artwork | 1:1；引用 04B |
| Artwork shape | `artworkShape / 12dp` |
| Artwork → Title gap | 12dp |
| Title | `titleMedium`，最多 2 行 |
| Artist/Metadata | `bodyMedium`，最多 2 行 |
| Text gap | 4dp |
| Touch target | 整个 Tile；内部 More Action 独立 48dp Target |

- Grid 决定 Tile 宽度：Compact 默认 2 列，Medium 3–4 列，Expanded 4–6 列；Artwork 不小于 120dp，不大于 240dp。
- Tile 主 Action 打开 Album Detail；More Action 不嵌套在同一个 Clickable Semantics 中。
- Missing/Loading Artwork 继承 04B；长标题不覆盖 Artwork，不把 Dynamic Artwork Color 用作 Tile 背景。

### 08B — Song Row

| Property | Value |
|---|---:|
| One-line min height | 72dp |
| Two-line/large text | 88dp 起，弹性增长 |
| Horizontal padding | 16dp |
| Artwork | 56dp × 56dp；`artworkShapeCompact / 8dp` |
| Artwork → Text gap | 12dp |
| Title | `bodyLarge` |
| Supporting | `bodyMedium` / `onSurfaceVariant` |
| Trailing Action | 48dp Target / 24dp Icon |

- Row 主 Action 打开 Song Detail 或执行产品定义的非播放浏览操作；本文不预设 Tap 即播放。
- Title、Artist、Album、Duration 等信息按任务优先级显示；Duration 不进入可点击 More Action Target。
- Selected 仅表示列表选择/当前上下文，不等同 Playing；禁止用均衡器动画或播放进度暗示 Foundation 状态。
- 批量选择模式暴露 Checkbox/Selected Semantics，不能同时保留与选择冲突的 Row 主 Action。

### 08C — Section Header

| Property | Value |
|---|---:|
| Min height | 48dp |
| Title | `titleLarge` 或紧凑场景 `titleMedium` |
| Supporting | `bodyMedium` |
| Action | Text Button 或 Icon Button；48dp Target |
| Bottom gap | 8–16dp，由所属布局选择 Token |

- Header 描述后续内容分组，不伪装成可点击 Row。存在“查看全部”时使用明确文字 Action。
- 作为无障碍 Heading 暴露；Sticky Header 不能重复朗读或遮挡焦点内容。

### 08D — Quality Badge

| Property | Value |
|---|---:|
| Height | 20dp |
| Horizontal padding | 6dp |
| Shape | `shapeExtraSmall / 4dp` |
| Label | `labelSmall` |
| Color | `tertiaryContainer` / `onTertiaryContainer` |

- Label 使用可理解缩写，如 `LOSSLESS`、`HI-RES`；不能只用颜色或无法解释的符号。
- Badge 是非交互元数据，与 Song/Album 的合并描述一起朗读；筛选入口使用 Chip，不让 Badge 可点击。
- 同一项最多显示一个主要 Quality Badge，更多技术信息进入 Detail，避免列表噪声。

08 状态：**已冻结**；作为基于 Material3 Foundation Token 构建的 Resonote Extension 验收。  
辅助视觉证据：`design/approved/components/08-music-components.png`  
矢量源：`design/approved/components/08-music-components-source.svg`

## 9. Component System 验收

- 每个组件在 Light、Dark、AMOLED，1.0×/2.0× 字号，Compact/Medium/Expanded/Large/Extra-large 下验证。
- Enabled、Pressed、Focused、Disabled 以及组件声明的 Selected/Loading/Error 均与 05B 一致。
- TalkBack、Keyboard/D-pad、Switch Access 能完成所有 Action；Modal Focus、Back 与恢复路径正确。
- 文档中不存在 Player 专属布局、播放进度、Queue、Pager 或歌词高亮 Token。
- 06–08 状态：**已冻结**；08 的“通过”表示符合 Resonote Extension 合同，不表示其为 Material3 官方组件。
