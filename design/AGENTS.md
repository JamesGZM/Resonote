# Design 文档规则

本目录遵循根 [工程规则](../AGENTS.md)，并增加以下约束。

- `FOUNDATION.md` 定义 Token 与主题语义，`COMPONENT_SYSTEM.md` 定义可复用组件合同，`PRODUCT_REQUIREMENTS.md` 定义现行产品行为，`VALIDATION.md` 定义验收矩阵。
- 已经用户人工调整并通过真机验收的行为是最高优先级视觉事实。Compose 实现、现行合同和 Roborazzi 应同步记录它；旧文档、旧截图或外部参考不得覆盖它。
- `approved/` 图片必须被现行 Markdown 明确引用才有规范意义；删除合同后同步删除孤立图片。
- 颜色、Elevation、Shape 与 Typography 必须通过 `MaterialTheme` 或 design-system token 消费，不在业务组件中复制 Hex、阴影或主题判断。
- 修改冻结组件时同步更新合同、测试和必要的截图证据；不要创建临时“计划”文档取代现行合同。
- “规范对齐”、格式化、Token 治理或架构整理不授权改变已验收组件的位置、尺寸、层级、内容或交互。如果规范与现行真机基线冲突，先停止修改并提请用户决策。
- Roborazzi Golden 只在有意视觉变更且已人工审查时更新；不得为了让 CI 变绿盲目用 `actual` 覆盖基线。
- 同一提交不得在未提供视觉差异审查的情况下，同时改动冻结组件实现并批量重录其 Golden；基线更新不能作为实现改动正确的证明。
- Linux CI 是跨平台截图验证权威环境。当本机通过而 CI 失败时，先下载 `roborazzi-failure-*` Artifact 审查 actual / compare / report，再区分真实回归与跨平台渲染噪声。
- 公共 `DefaultRoborazziOptions` 只容忍低于 `0.001%` 的跨平台 Skia 噪声；修改该阈值必须提供 CI diff percentage 证据并更新 ADR。
