# Design 文档规则

本目录遵循根 [工程规则](../AGENTS.md)，并增加以下约束。

- `FOUNDATION.md` 定义 Token 与主题语义，`COMPONENT_SYSTEM.md` 定义可复用组件合同，`PRODUCT_REQUIREMENTS.md` 定义现行产品行为，`VALIDATION.md` 定义验收矩阵。
- 当前 Compose 实现和 Roborazzi 基线是实现证据。旧页面设计图不能覆盖已经人工调整并通过验收的真机实现。
- `approved/` 图片必须被现行 Markdown 明确引用才有规范意义；删除合同后同步删除孤立图片。
- 颜色、Elevation、Shape 与 Typography 必须通过 `MaterialTheme` 或 design-system token 消费，不在业务组件中复制 Hex、阴影或主题判断。
- 修改冻结组件时同步更新合同、测试和必要的截图证据；不要创建临时“计划”文档取代现行合同。
