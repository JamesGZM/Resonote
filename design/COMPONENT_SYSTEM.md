# Resonote Component System

> 状态：执行基线；06–09 已冻结
> 更新日期：2026-08-14
> 规范范围：06 Core Components、07 Navigation & Feedback、08 Music Components、09 Tabs Shell Bottom Chrome
> Foundation 依赖：[FOUNDATION.md](./FOUNDATION.md)  
> Material 基线：`androidx.compose.material3:material3:1.4.0` 稳定版 Baseline

## 0. 交付原则

- 本文档是组件 Token、尺寸、行为、状态、语义与适配规则的规范源；视觉稿只用于审阅形态和关系。
- 颜色只引用 Foundation Semantic Role，字号只引用 Type Token，圆角、间距、Elevation、Motion 与 State Layer 不在本文另造同义 Token。
- Material 3 原生组件能够满足规范时优先使用；自定义封装只能增加 Resonote 语义与一致默认值，不能破坏 Compose 原生 Semantics。
- 所有交互组件最小 Touch Target 为 48dp，200% 字号下允许高度增长，不得裁切或强制缩字。
- Queue、Full Player、Pager、歌词高亮和播放页布局不属于本文范围；Tabs Shell 使用的 Mini Player 与 Bottom Navigation 组合由 09 定义。

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
- Pressed/Ripple 必须由定义视觉 Container 与 Shape 的交互组件承载。整张卡片是一个 Action 时使用带 `shape` 的 Material3 `Surface(onClick)` 或等价 Design System 封装，不得把裸 `Modifier.clickable` 挂在内部无 Shape 的 Row/Column 上制造矩形 State Layer。
- 禁止在 App Theme 层全局关闭 Material Ripple。个别上游组件内部 State Layer 形状无法配置时，只能在最小组件边界局部关闭，并在其内容子树立即恢复；不得连带移除 Navigation、Button、Icon Button、List Item 等组件的正确反馈。
- 容器内存在独立子 Action 时，主体与子 Action 必须拥有独立 Semantics、Interaction Source 和点击结果；点击子 Action 不得同时触发主体 Action。

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
- Glyph、圆形 State Layer 与 48dp Touch Target 必须保持同心。禁止只对 Glyph 使用 `offset` 修正按钮间距；间距问题必须通过父布局、Action 数量或正确组件 Variant 解决，且相邻 Touch Target 不得重叠。

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
- App 只能在根层持有一个稳定的 `SnackbarHostState`、Controller 与 `SnackbarHost`；切换 Tab、进入子页面或打开 Feature 不得替换、销毁或创建第二个 Host。
- Feature 只能向 App 级 Controller 提交消息，不得持有 `SnackbarHostState`，不得在页面 `Scaffold` 中声明 `snackbarHost`，也不得从页面生命周期直接挂起调用 `showSnackbar()`。
- App 级 Controller 负责调用挂起的 `showSnackbar()`，使消息展示不受触发页面重组、状态清空或退出影响。普通无 Action 反馈默认 `Short`；有 Action 默认 `Indefinite` 并提供 Dismiss。
- Controller 采用 latest-wins 调度：相同消息再次触发时合并为当前一条并从最新触发时刻重新计时；新的不同消息替换当前消息并取消旧任务。任何情况下最多保留一条请求，不保留可导致长时间连续展示的历史队列。需要持久恢复或逐条确认的事件不得用 Snackbar 队列承载。
- 根 Host 使用 `WindowInsets.safeDrawing.exclude(WindowInsets.ime)`。Tab Bar 与 Mini Player 只能向根 Host 上报动态 Bottom Avoidance，不得通过嵌套 Host 解决位置问题。
- Snackbar 与 Mini Player 卡片可见顶边保持 8dp 间距；动态 Bottom Avoidance 必须测量卡片本身边界，不得将 Mini Player 外边距重复计入。
- 实现模式参考 NiA `NiaApp.kt` 的单一 Host、`LocalSnackbarHostState` 与 Safe Drawing 处理；当前固定参考提交为 `7d45eae4f8720a0c77f507712ba2437ff974b6ed`。

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
- 保留 Material3 默认 Handle 的尺寸、位置与可访问 Action。若同版本框架为 Handle 外层生成与视觉形状不匹配的矩形 Ripple，只在该外层作用域关闭 Ripple，并在 Sheet 正文恢复默认配置；不得替换 Handle UI，也不得关闭正文 Button、Icon Button、List Item 的反馈。
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

本节是基于 Material3 Color、Typography、Shape、State 与 Accessibility Token 构建的 Resonote 产品扩展，不宣称为 Material3 官方组件。本节定义音乐资料浏览组件及当前播放项的列表态提示，不定义播放控制、进度、队列、歌词或 Player 页面。

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

### 08B — Playlist Item

| Property | Value |
|---|---:|
| Compact grid | 2 列；外边距 16dp；列间距 16dp |
| Artwork | `1:1`；填满 Tile 宽度；网络图片 `Crop`；引用 04B |
| Artwork shape | `artworkShape / 12dp` |
| Artwork → Title gap | 8dp |
| Title | `bodyLarge`；严格 1 行；超出后 End Ellipsis |
| Play-count overlay | 左下 8dp；深色半透明容器；播放图标 + 数量 |
| Touch target | 整个 Tile |

- Loaded 封面从网络加载，容器先按 `1:1` 占位，加载完成不得引发布局跳动；图片等比居中裁切，不拉伸。
- `Loading` 与 `Missing` 使用 04B 的同一 `artworkPlaceholder`：中性正方形底色与两条低对比度水平标记，不再提供唱片、山景或破图变体。
- `Loading` 的标题区域使用一条静态骨架；`Missing` 保留正常标题和交互，视觉证据使用“未收录封面”。两种状态通过相邻内容与 Semantics 区分。
- 标题不得换行；长标题只能在单行末尾省略。播放量覆盖层只在 Loaded 且数据可用时出现。
- Tile 主 Action 打开 Playlist Detail；播放量只是元数据，不建立独立点击目标。

#### Compact 布局与截断合同

1. 页面先扣除 Start/End 各 16dp，再以固定 2 列和 16dp 列间距计算 Tile 宽度；不得因单页数据量或长标题临时改成 3 列。
2. Artwork 的宽和高都等于 Tile 宽度，先建立 `1:1` 容器再发起网络图片请求；Loaded、Loading、Missing 三种状态不得改变 Tile 宽高。
3. Title 位于 Artwork 下方 8dp，使用单个文本测量区域：`maxLines = 1`、`softWrap = false`、`overflow = TextOverflow.Ellipsis`。
4. Title 不得通过减小字号、压缩字距、缩小 Artwork 或扩展到第二行来容纳长文本。
5. Play-count Overlay 只占 Artwork 内部空间，不参与 Title 测量，也不得伸出 Artwork 边界。

### 08C — Music Item / Song Row

| Property | Value |
|---|---:|
| Min height | 80dp；大字号下弹性增长 |
| Content padding | Start / Top / Bottom 8dp；无 More 时 End 8dp |
| Artwork | 64dp × 64dp；`artworkShapeStandard / 12dp` |
| Artwork → Text gap | 12dp |
| Title | `bodyLarge`；严格 1 行；End Ellipsis |
| Supporting | `bodySmall` / `onSurfaceVariant` |
| Artwork badge | 左下 4dp；Quality / VIP 组合紧凑角标 |
| Trailing metadata | Duration 固定 Status Slot；不参与标题测量 |
| Trailing Action | More：48dp Target / 24dp Icon |

- Row 主 Action 打开 Song Detail 或执行产品定义的非播放浏览操作；本文不预设 Tap 即播放。
- Title 独占文字列首行，Quality / VIP 不得进入 Title 行；二者合并为封面左下角的紧凑 Artwork Badge。Duration 和 More 固定在尾部。
- Artist/Supporting 独占第二行并允许单行省略；Duration 不进入可点击 More Action Target。
- Loaded 封面从网络加载并按 04B `Crop`，有 Quality / VIP 时在封面内部叠加 Artwork Badge。`Loading` 与 `Missing` 使用完全相同的 `artworkPlaceholder` 封面；`Loading` 同时以静态骨架替代 Title/Supporting，`Missing` 显示正常歌曲信息和时长。
- Playing 与 Selected 是不同状态。Playing 使用 `primaryContainer` 低强调背景、`primary` 标题与均衡器状态标记，但不显示播放进度；Reduced Motion 下均衡器静止。
- Playing 时均衡器状态标记直接替换固定 Duration Slot；当前播放行不得同时显示均衡器和时长。More 仍保留在原固定尾部 Target，均衡器不得移动到 Title/Badge 区。
- Selected 仅表示列表选择/当前上下文，不得复用 Playing 的均衡器或播放语义。
- 批量选择模式暴露 Checkbox/Selected Semantics，不能同时保留与选择冲突的 Row 主 Action。

#### Compact 文字列与尾部宽度合同

水平逻辑顺序固定为：

`Artwork（内含可选 Badge） → Title / Supporting（弹性） || Duration（固定尾部） → More（固定尾部）`

其中 `||` 表示中央文字区与尾部保留区的硬边界。实现必须遵循以下合同：

1. 先为 More 保留完整 48dp Touch Target，再为 Trailing Status Slot 保留固定宽度及规定间距；普通行在该 Slot 显示 Duration，Playing 行在同一 Slot 显示均衡器，二者互斥且不参与 Title 的宽度竞争。
2. Artwork Badge 只占用 Artwork 内部空间，不参与 Title、Supporting、Duration 或 More 的宽度测量，也不得移回 Title 行。
3. Title 和 Supporting 共享弹性中央列，均固定 `maxLines = 1`、`softWrap = false`、`overflow = TextOverflow.Ellipsis`。
4. `TrailingStatus = Duration XOR PlayingIndicator`。设计评审中出现 Badge 位于 Title 或尾部状态区、Duration 被挤压，或 Playing 行同时出现均衡器与 Duration，均直接判定为不符合冻结合同。

### 08D — Section Header

| Property | Value |
|---|---:|
| Min height | 48dp |
| Title | `titleLarge` 或紧凑场景 `titleMedium` |
| Supporting | `bodyMedium` |
| Action | Text Button 或 Icon Button；48dp Target |
| Bottom gap | 8–16dp，由所属布局选择 Token |

- Header 描述后续内容分组，不伪装成可点击 Row。存在“查看全部”时使用明确文字 Action。
- 作为无障碍 Heading 暴露；Sticky Header 不能重复朗读或遮挡焦点内容。

### 08E — Quality Badge

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

#### 08E-1 — Artwork Metadata Badge

Song Row 与 Compact Mini Player 使用封面叠加变体，不使用上述 20dp 标题行变体：

- 位于 Artwork 左下角，内缩 4dp；不得移到 Title 右侧。
- Quality 紧凑化为 `SQ` / `HQ` / `HR`；Quality 和 VIP 同时存在时合并为 `SQ · VIP` 形式的单一角标。
- 角标使用 4dp Shape、水平 3dp / 垂直 1dp Padding、8sp Label 与 10sp Line Height；深色 62% 遮罩承载白色文字。
- 不可解释且超过 3 个字符的未知 Quality 不进入紧凑角标；详细音质信息仍在 Detail 展示。

08 状态：**已冻结**；作为基于 Material3 Foundation Token 构建的 Resonote Extension 验收。

用户确认的冻结视觉基线：

- Music Item / Song Row：`core/designsystem/src/test/screenshots/MusicComponents/MusicItems_light.png`
- Playlist Item：`design/approved/components/08-playlist-item.png`
- 上述两张 PNG 固定 Compact 视觉证据；实现数值、状态和可访问性仍以本文 Markdown 合同为准。Music Item 的 Dark / AMOLED / 200% 字号变体由同目录 Roborazzi 基线共同约束。

## 09 — Mini Player & Bottom Navigation Shell

本节只冻结 Tabs Shell 底部区域，不冻结视觉证据中用于承载它的首页示例内容。

### 09A — Compact Mini Player

| Property | Value |
|---|---:|
| Container | 悬浮卡片；`surfaceContainer` + Level 3 Shadow；颜色与 Bottom Navigation 一致 |
| Outer spacing | Start 16dp / End 16dp / 到 Bottom Navigation 顶部 16dp |
| Min height | 72dp；200% 字号下弹性增长 |
| Shape | `shapeLarge / 16dp` |
| Elevation | Level 3；`surfaceContainer` + `6dp` Shadow；阴影不得侵入与 Bottom Navigation 之间的 16dp 可见间距 |
| Artwork | 56dp × 56dp；`artworkShapeStandard / 12dp`；引用 04B |
| Artwork → Text gap | 12dp |
| Title | `bodyLarge`；严格 1 行；End Ellipsis |
| Supporting | `bodySmall`；严格 1 行；End Ellipsis |
| Quality / VIP | 复用 08E-1；合并显示在 Artwork 左下角 |
| Playback actions | Pause/Play、Queue；各自 48dp Touch Target；Compact 不显示 Next |
| Progress | 2dp；位于卡片内部底边，不越出 Container |

- Mini Player 是独立悬浮卡片，不能与 Bottom Navigation 贴合、共边、融合或重叠。左右与下方三处 16dp 间距必须在 Compact 视觉和实现中同时成立。
- 卡片与 Bottom Navigation 均使用 Material Navigation Bar 默认映射的 `surfaceContainer`；两者颜色必须一致，并继续由 16dp 页面背景带明确分隔。
- Compact Tabs Shell 中 Mini Player 位于滚动内容之上的独立 Overlay 层。列表内容在滚动过程中可以从卡片后方经过；Mini Player 不结束列表、不切断外层容器，也不要求内容层在卡片上方保留永久空白带。
- 滚动容器末尾必须提供足够的 Bottom Content Padding，使最后一个可聚焦 Item 能完整滚动到 Mini Player 上方。该 Padding 只保证末项可达，不改变中间滚动状态允许内容位于悬浮层后方的层级合同。
- Mini Player Container 使用带 `shape` 的可点击 Surface 承担主体 Action；除内部独立 Icon Button 外，点击卡片任意区域均直接打开 Full Player。Surface 的 State Layer 必须按卡片圆角裁切，内部播放控制不得触发主体 Action。
- `queue_music` 是独立 Queue Action，直接打开当前 Queue 的 Modal Bottom Sheet，不要求先进入 Full Player。
- Title 独占信息列首行，Quality / VIP 只能作为 Artwork 左下角的组合角标。可用宽度不足时 Title 执行 End Ellipsis；Pause/Play 和 Queue 入口不得换行、隐藏或越界。Compact 不提供 Next，避免三个连续 Icon Button 压缩主信息。
- 尚无当前媒体时整个 Mini Player 不显示，Bottom Navigation 保持原位置且不保留空占位。

#### Compact 信息行宽度合同

- Mini Player 先保留 Pause/Play、Queue 两个独立 Touch Target，剩余宽度交给 Title / Artist 信息列；Artwork Badge 不参与信息列测量。
- Title 使用单行 End Ellipsis；Quality / VIP 必须保持在 Artwork 内，不得因文字或操作区宽度变化而移回 Title 行。
- Artist 独占第二行并单行 End Ellipsis。Title 或 Artist 变长不得移动、隐藏或缩小右侧两项操作。
- Queue 必须使用明确的播放列表图标与本地化 `contentDescription`，点击打开当前 Queue；不得用 More 菜单代替该入口。

### 09B — Compact Bottom Navigation

- 使用 07A Material3 Navigation Bar 合同，目的地固定为“首页、发现、我的”，首页为 App 默认选中项。
- Container 使用 Material Navigation Bar 默认映射的 `surfaceContainer`。禁止传入自定义 `containerColor`、写死白色、附加透明度、渐变或手写 Shadow；主题变化只通过当前 `ColorScheme` 生效。
- Navigation Bar 消费底部系统 Insets；其 Container 延伸覆盖手势安全区，Gesture Indicator 使用 03D 设计证据规则。
- Navigation/App Scaffold 将 `innerPadding` 交给页面内容时，必须同时执行 `.padding(innerPadding).consumeWindowInsets(innerPadding)`；禁止让发现、我的等 Feature Scaffold 再次消费 Navigation Bar 已处理的底部 Insets。
- 三键/两键虚拟系统导航启用时，System Navigation Bar 使用同一 `surfaceContainer` 实色与匹配的图标明暗，不允许平台默认对比遮罩在底部产生第二条异色容器。
- Gesture、Two-button、Three-button 三种模式下，页面可用内容区域必须连续结束于 Navigation Bar 顶边；两者之间不得出现等于系统导航栏高度的额外空白带。
- Mini Player 出现或消失不得改变三个 Destination 的尺寸、选中状态、Tab 状态或 Back Stack。
- Mini Player 与 Navigation Bar 均映射 `surfaceContainer`，Mini Player 额外使用 Level 3 Shadow；二者之间必须露出 16dp 页面 `background`。
- 三个 Destination 等分可用宽度；Icon、Active Indicator 与 Label 使用 Material3 Navigation Bar 的内部 Token，不因 Mini Player 出现而上移、压缩或改变选中态。
- Compact Destination 保持冻结的 Resonote Icon/Label/Color/Ripple 视觉结构；Item 内容层固定为 64dp，完整点击区域必须覆盖同一 64dp，不得扩展到页面内容或底部系统 Insets。禁止在 Item 上使用 `clipToBounds()` 或裁切 Ripple/State Layer。

09 状态：**已冻结**。2026-08-14 已按真机 Light 基线验收 Bottom Shell；后续主题色调整必须修改完整 Scheme 并重新验证 Light / Dark / AMOLED / Dynamic，不得为 Bottom Navigation 增加单点颜色覆盖。
用户确认的冻结视觉基线：`app/src/test/screenshots/TabsShell/BottomShell_Light.png`。同目录的 Dark / AMOLED / Dynamic 基线共同约束主题变体。
这些 PNG 只冻结 Mini Player、Bottom Navigation、系统手势区及其相互间距；图中上方内容不是首页视觉基线。首页对 Overlay 层级的实际组合以当前 Compose 实现与 Roborazzi 基线为准。

## 冻结组件快速索引

| 组件 | 规范性合同 | 冻结视觉证据 | 不得回归 |
|---|---|---|---|
| Music Item / Song Row | 08C | `core/designsystem/src/test/screenshots/MusicComponents/MusicItems_light.png` | Title 换行；Quality/VIP 离开封面左下角；Playing 同时显示均衡器和 Duration；Artwork 不是 64dp；Loading 与 Missing 使用不同 Placeholder |
| Playlist Item | 08B | `design/approved/components/08-playlist-item.png` | Compact 改为 3 列；Artwork 不是 1:1；Title 换行；Loaded/Loading/Missing 导致布局跳动 |
| Compact Mini Player | 09A | `app/src/test/screenshots/TabsShell/BottomShell_Light.png` | 与 Navigation Bar 贴合；Quality/VIP 不在封面左下角；长标题挤压播放操作；缺少 Queue 入口 |
| Compact Bottom Navigation | 09B、07A | `app/src/test/screenshots/TabsShell/BottomShell_Light.png` | 与 Mini Player 融合；缺少 16dp 间距；Container 颜色不一致；目的地不是首页/发现/我的 |

后续线程开始页面设计或 Android 实现前，必须先读取本索引对应章节；不能仅凭 PNG 重新推断布局规则。PNG 与 Markdown 不一致时，以本文的尺寸、测量、截断、状态和可访问性合同为准，并提请重新评审视觉证据。

## 9. Component System 验收

- 每个组件在 Light、Dark、AMOLED，1.0×/2.0× 字号，Compact/Medium/Expanded/Large/Extra-large 下验证。
- Enabled、Pressed、Focused、Disabled 以及组件声明的 Selected/Loading/Error 均与 05B 一致。
- TalkBack、Keyboard/D-pad、Switch Access 能完成所有 Action；Modal Focus、Back 与恢复路径正确。
- 文档中不存在 Full Player 专属布局、Queue 内容布局、Pager 或歌词高亮 Token；Mini Player 只使用 09 声明的列表级播放进度与 Queue 入口。
- 06–09 状态：**已冻结**；08–09 的“通过”表示符合 Resonote Extension 合同，不表示其为 Material3 官方组件。
