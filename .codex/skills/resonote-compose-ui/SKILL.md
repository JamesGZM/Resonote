---
name: resonote-compose-ui
description: 修改 Resonote Compose 页面、组件、主题、布局、资源文案、交互或截图基线。用于任何用户可见 UI 变化；不用于脱离界面的一般业务、网络或存储实现。
---

# Resonote Compose UI

用于保持 Compose 实现、已验收真机行为、设计合同和 Roborazzi 证据一致。

## 工作流程

1. 先读根 `AGENTS.md` 和目标代码附近的现有组件与测试。
2. 按任务读取 `design/FOUNDATION.md`、`design/COMPONENT_SYSTEM.md` 与 `design/PRODUCT_REQUIREMENTS.md`。
3. 先确认用户已验收行为和现行截图，不能用旧文档或“规范对齐”回改已验收布局。

## UI 规则

- 业务组件消费 `MaterialTheme` 与 design-system token，不复制 Hex、主题分支、Shape 或 Elevation。
- Composable 尽量无状态；业务状态与副作用由页面或 ViewModel 持有。
- 点击反馈匹配可见轮廓：无容器操作使用 `ResonotePlainAction`，圆角容器使用相同 Shape 的可点击 Surface。
- 支持下拉刷新的已有内容不重复放刷新按钮；首次无内容失败保留重试入口。
- 用户可见文案同时维护模块的 `values/strings.xml` 与 `values-zh-rCN/strings.xml`，不在 Kotlin 中硬编码。
- 为 Loading、Empty、Error、Offline、Permission、超长内容和适用的无障碍状态提供稳定行为。
- 截图使用确定性 Fixture。更新 Golden 前先审查 actual/compare，并确认视觉变化已获批准。

## 验证

- 运行目标模块的 `testDebugUnitTest`、相关 Roborazzi 验证任务与 `spotlessCheck`。
- 修改冻结组件时同步合同、行为测试和必要基线；不得用批量重录掩盖回归。
- Linux CI 的 `verifyRoborazziDebug` 是跨平台截图权威门禁。
