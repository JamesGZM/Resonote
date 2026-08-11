# Resonote Design System Validation

> 状态：规范已冻结；实现证据补充中
> 更新日期：2026-08-11
> 依赖：[FOUNDATION.md](./FOUNDATION.md)、[COMPONENT_SYSTEM.md](./COMPONENT_SYSTEM.md)  
> 证据原则：实现截图、自动化结果与录屏用于证明实现；设计稿不能替代实现证据

## 1. 目标与边界

本文定义 11 Validation Matrix，确保 Resonote 的 Foundation 与 Component System 在主题、字号、窗口、语言、输入方式、内容和状态变化下仍满足同一套规范。

- 本文记录验证维度、用例、通过条件、证据路径和缺陷分级。
- `design/approved/` 与 `design/review/` 中的图片只用于设计审阅，不计作 App 实现证据。
- 当前仓库已有最小 App、独立 Catalog、Design System 与截图测试模块；06A 与 06B-1 已提供组件级自动化实现证据，其余范围继续保持“待实现验证”，不得用合成稿补位。
- “规范已冻结”只表示验证维度、用例、门槛、证据合同与缺陷分级已经确定，不表示任何 V-01–V-10 用例已经执行或通过。
- Player 产品层不属于当前验证范围；Player 接入设计系统后另建产品验证矩阵。

## 2. 验证维度

| 维度 | 必测取值 | 主要风险 |
|---|---|---|
| Theme | Light、Dark、AMOLED | 语义色误用、层级丢失、对比度不足 |
| Font Scale | 1.0、1.3、2.0 | 裁切、重叠、强制缩字、阅读顺序错误 |
| Window | Compact、Medium、Expanded、Large、Extra-large | 导航迁移、内容宽度、Insets、状态丢失 |
| Locale | 简体中文、英文、中英混排、RTL 代表语言 | 截断、错误镜像、硬编码顺序 |
| Interaction | Enabled、Hover、Focused、Pressed、Dragged、Selected、Disabled、Loading、Error、Success、Unavailable | 状态层、优先级、反馈缺失 |
| Input | Touch、Mouse、Keyboard、D-pad、TalkBack、Switch Access | 目标过小、焦点不可见、操作不可达 |
| Motion Scale | 0、1、10 | 关闭动画后不可用、空间转场过度、状态延迟 |
| Content | 正常、超长、缺失、空、错误、离线、权限拒绝 | 布局崩坏、无恢复或退出路径 |

## 3. 执行策略

不机械执行所有维度的笛卡尔积。采用“基线冒烟 + 成对覆盖 + 风险穷举”：

1. 每个组件先在 Light、1.0、Compact、中文、Touch、正常内容下完成基线验证。
2. 主题、字号、窗口、语言和输入方式使用成对覆盖，保证任意两个维度的组合至少出现一次。
3. Text Field、Navigation、Dialog、Bottom Sheet、Snackbar、Album Tile 与 Song Row 对声明的所有持久状态做组件级穷举。
4. 2.0 字号、RTL、TalkBack、Keyboard/D-pad、AMOLED、Motion Scale 0 单独执行高风险专项。
5. 缺失、错误、离线和权限拒绝必须分别验证，不能用同一个通用空态替代。

### 3.1 00B 品牌启动身份

- 自动化合同覆盖两个 Manifest 的 Launcher/Splash Theme、主 App API 26–30 静态回退、API 31+ AVD、Catalog 静态 Mark、Light / Dark 启动色和 `750ms` 参数。
- 设计评审稿覆盖 `16 / 24 / 32 / 48px`、Circle / Rounded / Squircle / Teardrop、Light / Dark 终态与动画分镜；该稿只证明资产设计，不替代真实 Launcher 或启动截图。
- 已补 API 32 Emulator、Light、Motion Scale `1×`、真实 Launcher 点击冷启动录屏和终态截图；AVD 完整进入圆形安全区，Splash 与 Compose 首帧均为 `#FFFBFF`，未观察到异色闪屏。
- 待补真实设备证据：API 26 / 30 / 最新 API 的 cold / warm / hot start，以及 Dark、Motion Scale `0× / 10×` 录屏和首帧闪烁检查。

## 4. 组件验证矩阵

| ID | 范围 | 必测组合 | 通过条件 | 所需证据 | 当前状态 |
|---|---|---|---|---|---|
| V-01 | 全局颜色 | Light / Dark / AMOLED × 主要页面 | Role 映射正确；正文与操作对比度符合 01H | 三主题实现截图 + 对比度报告 | Not Run（00B Light 启动色已有自动化与 API 32 截图） |
| V-02 | Typography | 1.0 / 1.3 / 2.0 × 中英混排 / 超长内容 | 无裁切、重叠、强制缩字；阅读顺序正确 | 实现截图 + Layout Inspector | 待实现验证 |
| V-03 | Adaptive Layout | Compact / Medium / Expanded / Large / Extra-large × 导航与筛选状态 | 导航形态按 03D 切换；Large/Extra-large 复用 Expanded 拓扑但分别验证内容限宽；目的地、查询和筛选不重置 | 五窗口截图 + 状态切换录屏 | 待实现验证 |
| V-04 | Buttons / Icon Buttons | Enabled / Hover / Focused / Pressed / Disabled / Loading | 目标、图标、状态层、焦点环与 04A、05B、06 一致 | 状态截图 + 指针/键盘录屏 | Not Run（06A 已有部分自动化覆盖） |
| V-05 | Inputs / Selection | Empty / Focused / Filled / Error / Disabled × 2.0 | Label、Support Text、错误语义稳定；不会遮挡输入 | 状态截图 + 语义树 | Not Run（06B-1 Text Field 已有部分自动化覆盖） |
| V-06 | Navigation | Bar / Rail × Touch / Keyboard / D-pad / TalkBack | 选中项唯一；焦点可见；顺序稳定；可朗读当前项 | 窗口截图 + 无障碍测试日志 | 待实现验证 |
| V-07 | Feedback | Loading / Empty / Error / Offline / Permission denied | 状态语义不混用；均有明确恢复或退出路径 | 状态截图 + 操作录屏 | 待实现验证 |
| V-08 | Overlays | Dialog / Bottom Sheet / Snackbar × Keyboard / TalkBack | 焦点被正确约束或恢复；Back 与关闭语义一致 | 焦点录屏 + 语义树 | 待实现验证 |
| V-09 | Music Components | Album Tile / Song Row / Section Header / Quality Badge × 缺图 / 超长 / RTL | 资料层级稳定；占位与截断符合 04B、08 | 实现截图 | 待实现验证 |
| V-10 | Motion | Motion Scale 0 / 1 / 10 × Effects / Spatial | 0 时即时到达终态；Spatial 使用 Spring；无关键状态依赖动画传达 | 屏幕录制 + 动画参数日志 | Not Run（00B AVD 参数与 API 32 Light `1×` 已有部分证据） |

V-06 默认范围与 Material Adaptive Navigation Suite、NiA 基线一致，只验证 Bar / Rail。
Drawer 在产品显式引入对应 IA 与 Layout Type 策略后，作为条件性扩展单独补充验证；未引入时为 N/A。

## 5. 无障碍门槛

| 检查 | 通过条件 |
|---|---|
| Touch Target | 常规交互目标至少 48 × 48dp；例外必须提供等效可达区域 |
| Text Contrast | 普通文字至少 4.5:1；大文字至少 3:1 |
| Non-text Contrast | 关键控件边界、状态与焦点指示至少 3:1 |
| Font Scale | 200% 下不裁切、不重叠、不强制缩字，弹性高度和换行生效 |
| Semantics | 名称、角色、状态、值和可用操作完整；装饰内容不重复朗读 |
| Focus | 顺序与视觉/阅读顺序一致；焦点清晰可见；Overlay 关闭后回到触发点 |
| Alternatives | 手势、拖动和长按均有可发现的等效操作 |

对比度计算使用 WCAG 相对亮度公式；颜色取自实现解析后的最终像素值，不使用设计工具面板里的近似预览值。

## 6. 证据目录与命名

实现阶段建立以下目录；在没有真实证据前不创建占位图片：

```text
design/validation/
├── screenshots/
├── recordings/
├── semantics/
└── reports/
```

命名规则：

```text
{case-id}_{scope}_{theme}_{font}_{window}_{locale}_{state}.{ext}
```

示例：`v-05_text-field_dark_200_compact_zh_error.png`。

每条证据必须能追溯到 App 版本、设备/API、测试日期和用例 ID。截图不得裁掉系统栏或关键上下文；涉及动态行为时同时提交录屏，不能只提交终态截图。

06A 与 06B-1 自动化回归基线保存在 `core/designsystem/src/test/screenshots/`，通过以下命令录制和验证：

```bash
./gradlew :core:designsystem:recordRoborazziDebug
./gradlew :core:designsystem:verifyRoborazziDebug
```

06A Golden 覆盖 Light、Dark、AMOLED、200% 字号、Disabled、Loading 与 Toggle。06B-1 Golden 覆盖 Text Field 持久状态、Metadata/Action、Light、Dark、AMOLED、100%/130%/200% 字号、RTL 与五档窗口成对矩阵；200% Metadata/Action 基线验证 Prefix/Suffix 独立行以及 Error、Disabled、Read-only 和 Trailing Action。行为测试另覆盖状态提升、IME Action、单/多行、Password Semantics、Unicode Code Point 长度限制、错误优先级、24dp 装饰图标和 48dp Trailing Action。

00B 的 API 32 Light `1×` 实现证据保存在 `design/validation/recordings/v-10_resonote-splash_api32_light_1x.mp4` 与 `design/validation/screenshots/v-10_resonote-splash_api32_light_1x_frame.png`；环境和结果见 `design/validation/reports/00b-startup-evidence.md`。

这些结果属于组件级自动化回归证据；它们不替代真实 IME、TalkBack、外接键盘、设备语义树和录屏，也不构成 V-04 或 V-05 Pass。00B 的应用模块资源合同测试同样不替代真实 Launcher Mask、启动截图与 Motion Scale 录屏，也不构成 V-01 或 V-10 Pass。

## 7. 结果记录与缺陷分级

| 级别 | 定义 | 发布处理 |
|---|---|---|
| Blocker | 崩溃、核心路径不可完成、辅助技术完全不可操作 | 阻止发布 |
| Critical | 文字不可读、关键操作不可见/不可达、状态导致误操作 | 阻止发布 |
| Major | Token、布局、焦点或恢复路径明显不符合规范 | 修复后回归 |
| Minor | 不影响任务完成的轻微视觉偏差 | 记录并排期 |

结果只允许 `Pass`、`Fail`、`Blocked`、`Not Run`。`Blocked` 必须写明环境或前置条件；不能把未执行标为通过。

## 8. 完成条件

- V-01–V-10 全部执行，Blocker、Critical 和 Major 为零。
- Light、Dark、AMOLED，1.0 与 2.0 字号，Compact、Medium、Expanded、Large、Extra-large 均有真实实现证据。
- TalkBack、Keyboard/D-pad、RTL 与 Motion Scale 0 专项通过。
- 所有证据路径有效并可追溯；设计稿未被当作实现截图。
- 当前章节状态：**规范已冻结；实现证据补充中。V-01、V-04、V-05 与 V-10 已有部分自动化覆盖，但完整用例仍为 Not Run**。
