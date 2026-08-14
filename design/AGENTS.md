# Design 文档规则

本目录遵循根 [工程规则](../AGENTS.md)，并增加以下约束。

- `FOUNDATION.md` 定义 Token 与主题语义，`COMPONENT_SYSTEM.md` 定义可复用组件合同，`PRODUCT_REQUIREMENTS.md` 定义现行产品行为，`VALIDATION.md` 定义验收矩阵。
- 当前 Compose 实现和 Roborazzi 基线是实现证据。旧页面设计图不能覆盖已经人工调整并通过验收的真机实现。
- `approved/` 图片必须被现行 Markdown 明确引用才有规范意义；删除合同后同步删除孤立图片。
- 颜色、Elevation、Shape 与 Typography 必须通过 `MaterialTheme` 或 design-system token 消费，不在业务组件中复制 Hex、阴影或主题判断。
- 修改冻结组件时同步更新合同、测试和必要的截图证据；不要创建临时“计划”文档取代现行合同。
- Roborazzi Golden 只在有意视觉变更且已人工审查时更新；不得为了让 CI 变绿盲目用 `actual` 覆盖基线。
- Linux CI 是跨平台截图验证权威环境。当本机通过而 CI 失败时，先下载 `roborazzi-failure-*` Artifact 审查 actual / compare / report，再区分真实回归与跨平台渲染噪声。
- 公共 `DefaultRoborazziOptions` 只容忍低于 `0.001%` 的跨平台 Skia 噪声；修改该阈值必须提供 CI diff percentage 证据并更新 ADR。
