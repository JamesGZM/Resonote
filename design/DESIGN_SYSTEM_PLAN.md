# Resonote Design System Plan

> 状态：执行基线  
> 更新日期：2026-08-10  
> 规范源：[FOUNDATION.md](./FOUNDATION.md) 及后续分层规范文档  
> 交付格式：Markdown 规则 + 必要的视觉证据  
> 执行方式：先顺序完成规划内规范与必要视觉证据，再统一审阅和冻结  
> Material 基线：`androidx.compose.material3:material3:1.4.0` 稳定版 Baseline；不使用 1.5 Alpha 或 Expressive API

## 1. 目标

建立一套基于 Material 3、参考 Now in Android 组织方式、同时拥有 Resonote 品牌与音乐产品特征的设计系统。

本文件只负责文档架构、实施顺序、交付类型和状态，不承载具体 Token 数值。Foundation 的 Hex、Tone、sp、dp、比率和行为规则统一维护在 [FOUNDATION.md](./FOUNDATION.md)，避免计划与规范产生双重数据源。

Player 属于产品设计层，不属于当前 Foundation 建设。已有 Player 图片暂时保留，但不在本计划中引用或继续展开。

## 2. 文档架构

| 层级 | 规范文档 | 范围 | 当前状态 |
|---|---|---|---|
| Foundation | [FOUNDATION.md](./FOUNDATION.md) | 00–05：Brand、Color、Typography、Shape、Layout、Icon、Motion、States、Accessibility | 已冻结 |
| Component System | [COMPONENT_SYSTEM.md](./COMPONENT_SYSTEM.md) | 06–08：Core、Navigation、Feedback、Music Components | 已冻结 |
| Validation | [VALIDATION.md](./VALIDATION.md) | 11：Theme、字体、Window Size Class、内容与状态矩阵 | 规范已冻结；实现证据待 App 实现 |
| Player Product Design | 暂不建立 | Playback Theme、Player Layout 与播放专属组件 | 不在当前范围 |

文档职责：

- 规范文档记录 Token、数值、行为、适用范围和冻结状态。
- 本计划只记录实施顺序、交付形式和整体进度。
- Approved 图片是辅助视觉证据，不是精确数据源。
- Review 图片只服务当前审阅；否决后删除。
- Material 3 原生组件使用 1.4.0 Baseline 默认 Token；AMOLED、品牌色与音乐业务组件必须明确标记为 `Resonote Extension`。

## 3. 参考层级

1. 已冻结的分层规范文档。
2. 已确认的 Resonote 视觉证据。
3. Material 3 官方体系；官方组件对齐 Compose Material3 1.4.0 公开 API 与同版本 Source JAR Token。若 API 在 1.4.0 中为 internal，则通过文档化的 Resonote Alias 实现，不直接访问内部 API。
4. Now in Android 的组织方式。
5. 其他成熟设计系统。

图片与规范文档冲突时，以规范文档为准。

## 4. 交付策略

### 4.1 文档优先

- 每个设计项必须先有 Markdown 规范。
- Hex、Tone、sp、dp、时长、比率与状态逻辑不得只存在于图片。
- 可被表格或文字无损表达的规则不单独出图。

### 4.2 视觉证据边界

- 需要视觉证据：品牌、色彩关系、Elevation、Adaptive Layout、图标、封面、交互状态、组件与页面布局。
- 不需要独立 PNG：Typography、Shape、Spacing、Accessibility 阈值。
- Motion 使用实现原型或录屏验证，不生成静态规范 PNG。
- Validation 使用实现截图作为证据，结论仍写入规范文档。

### 4.3 交付矩阵

| 项目 | 文档 | 视觉证据 |
|---|---|---|
| 00A Brand IP | 必须 | PNG |
| 01A–01H Color Foundation | 必须 | PNG |
| 02A–02B Typography & Accessibility | 必须 | 不需要 |
| 03A Shape Scale | 必须 | 不需要 |
| 03B Elevation | 必须 | PNG |
| 03C Spacing & Grid | 必须 | 不需要 |
| 03D Adaptive Layout & Insets | 必须 | PNG |
| 04A–04B Iconography & Imagery | 必须 | PNG |
| 05A Motion Tokens | 必须 | 原型或录屏 |
| 05B Interaction States | 必须 | PNG |
| 05C Accessibility Foundation | 必须 | 不需要 |
| 06–08 Component System | 必须 | 必要 PNG |
| 11 Validation Matrix | 必须 | 实现截图 |

## 5. 审核与冻结流程

```text
读取已冻结规范
→ 编写当前章节的 Token、行为和边界
→ 按交付矩阵决定是否制作视觉证据
→ 数据 QA + 必要的视觉 QA
→ 用户确认
→ 冻结规范章节并更新本计划
→ 开始下一章节
```

目录约定：

- `design/review/`：当前待确认的视觉稿，可被替换。
- `design/approved/`：已确认的辅助视觉证据。
- 被否决的 review 稿不保留，也不作为后续参考。

## 6. 实施顺序

### Foundation — 00–05

具体规则、Token 和章节状态见 [FOUNDATION.md](./FOUNDATION.md)。

当前顺序：

1. 00A Brand IP — 已冻结。
2. 01A–01H Color Foundation — 已冻结。
3. 02A–02B Typography & Accessibility — 文档已冻结。
4. 03A Shape Scale — 已冻结。
5. 03B Elevation — 已冻结。
6. 03C Spacing & Grid — 已冻结。
7. 03D Adaptive Layout & Insets — 已冻结。
8. 04A Icon System — 已冻结。
9. 04B Album Artwork — 已冻结。
10. 05A Motion Tokens — 已冻结，不生成静态 PNG。
11. 05B Interaction States — 已冻结，保留辅助视觉证据。
12. 05C Accessibility Foundation — 已冻结，不生成静态 PNG。

### Component System — 06–08

具体规则、Token 和章节状态见 [COMPONENT_SYSTEM.md](./COMPONENT_SYSTEM.md)。06–08 已完成审核并冻结。

- 06 Core Components：Button、Icon Button、Text Button、Text Field、Chip、Tag、Badge、Tabs、Segmented Control、Top App Bar、Loading、Snackbar、Dialog、Bottom Sheet。
- 07 Navigation & Feedback：Bottom Navigation、页面进入/返回、搜索、筛选、Dialog、Sheet、Snackbar、空态、错误态、加载态和权限请求。
- 08 Music Components：Album Tile、Song Row、Section Header、Quality Badge。
- MiniPlayer、Queue、Progress、Pager 和歌词高亮属于 Player 产品层，暂不纳入。

当前状态：06 Core Components、07 Navigation & Feedback 与 08 Music Components 均已冻结。07 使用三张辅助视觉证据；08 明确作为 Resonote Extension 验收，不宣称为 Material3 官方组件。

### Validation — 11

验证规则与证据要求见 [VALIDATION.md](./VALIDATION.md)，覆盖：

- Light / Dark / AMOLED。
- 正常字号 / 200%。
- Compact / Medium / Expanded / Large / Extra-large；Large/Extra-large 复用 Expanded 拓扑但单独验证限宽。
- 中英文混排与极端长度。
- 全部交互状态与无障碍路径。

当前状态：验证规范已冻结；真实实现证据须在 Android App 可运行后补齐，设计稿不得替代。规范冻结不表示 V-01–V-10 已执行或通过。

## 7. 当前状态

| 项目 | 交付 | 状态 |
|---|---|---|
| 00A Design Principles & Brand IP | 文档 + PNG | 已冻结 |
| 01A Brand Key Colors | 文档 + PNG | 已冻结 |
| 01B Accent Tonal Palettes | 文档 + PNG | 已冻结 |
| 01C Neutral & Error Tonal Palettes | 文档 + PNG | 已冻结 |
| 01D Light Semantic Scheme | 文档 + PNG | 已冻结 |
| 01E Dark Semantic Scheme | 文档 + PNG | 已冻结 |
| 01F Surface Hierarchy | 文档 + PNG | 已冻结 |
| 01G AMOLED Extension | 文档 + PNG | 已冻结 |
| 01H Color Contrast Validation | 文档 + PNG | 已冻结 |
| 02A Type Scale | 文档 | 已冻结 |
| 02B Large Text & Accessibility | 文档 | 已冻结 |
| 03A Shape Scale | 文档 | 已冻结 |
| 03B Elevation | 文档 + PNG | 已冻结 |
| 03C Spacing & Grid | 文档 | 已冻结 |
| 03D Adaptive Layout & Insets | 文档 + PNG | 已冻结 |
| 04A Icon System | 文档 + PNG | 已冻结 |
| 04B Album Artwork | 文档 + PNG | 已冻结 |
| 05A Motion Tokens | 文档；实现阶段使用原型或录屏验证 | 已冻结 |
| 05B Interaction States | 文档 + PNG | 已冻结 |
| 05C Accessibility Foundation | 文档 | 已冻结 |
| 06 Core Components | 文档 + PNG | 已冻结 |
| 07 Navigation & Feedback | 文档 + 3 PNG | 已冻结 |
| 08 Music Components | 文档 + PNG | 已冻结；Resonote Extension |
| 11 Validation Matrix | 文档；实现阶段补截图、录屏与报告 | 规范已冻结；实现证据待 App 实现 |
