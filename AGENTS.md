# Resonote 工程规则

本文件补充全局协作规则，作用于整个仓库；具体任务同时遵循 `.codex/skills/` 中适用的项目级 Skill。

## 事实来源

1. 用户已在真机上人工调整并明确验收的现行产品行为。
2. 当前 Resonote 源码、测试与现行合同。
3. [Resonote 架构](docs/ARCHITECTURE.md)、[开发指南](docs/DEVELOPMENT.md)、[产品需求](design/PRODUCT_REQUIREMENTS.md)、设计规范与协议文档。

日常开发只以 Resonote 自有源码、测试和现行文档为依据，不从相邻仓库推导架构、产品或协议行为。Android 官方文档可以用于核对平台 API，但不能覆盖 Resonote 已验证的行为。

当人工验收的真机行为与文档或旧截图冲突时，不得以“规范对齐”为由回改实现。应保留已验收实现，将冲突明确报告给用户，得到确认后再更新合同、测试与视觉证据。

## 项目级 Skills

项目级 Skills 位于 `.codex/skills/`，用于让 Codex 在高频、易错任务中自动加载更窄的 Resonote 规则。

- 使用 `$resonote-feature-development`：新增或修改业务功能、页面流程、ViewModel/UI State、Repository 编排或 Navigation 3 入口。
- 使用 `$resonote-compose-ui`：修改 Compose 页面、组件、主题、布局、资源文案、交互或 Roborazzi 基线。
- 使用 `$resonote-network-data`：修改网络请求、特殊协议、Session、DTO、DataSource、Repository 映射或错误分类。
- 使用 `$resonote-persistence`：修改 Room、DAO、Entity、Schema、Migration、Proto DataStore 或持久化兼容行为。
- 使用 `$resonote-media-playback`：修改 Media3、Queue、MediaSession、桌面歌词、MV 或 K 歌媒体链路。
- 使用 `$resonote-docs-maintenance`：维护 README、AGENTS、`docs/`、`design/`、ADR、文档结构或链接。
- 使用 `$resonote-code-review`：审查全仓库、完整工作流、指定模块、Git 范围或本轮修改；只报告风险，等待确认后再修复。

## 架构与实现

- 保持 `app/feature -> core:data -> network/database/datastore` 的依赖方向；Feature 不直接调用 Retrofit 或协议客户端。
- Network 对外暴露语义化 DataSource，Retrofit Service 和 DTO 保持内部可见。
- 请求 Policy 只描述签名、Session、默认参数、认证业务码等真实协议行为；不得加入文档编号或 Endpoint 注册表。
- 公共依赖和构建约定进入 Version Catalog 与单一职责 Convention Plugin；模块特有配置留在模块内。
- 不新增遥测、分析或外部网络调用，除非任务明确要求。
- 对支持下拉刷新的滚动内容：已有内容时仅提供下拉刷新，不在顶部栏或列表内重复提供显式刷新按钮；首次加载失败且没有可展示内容时，保留“重试”按钮作为错误恢复入口。

## 最小验证

- Network 行为：`./gradlew :core:network:testDebugUnitTest`
- Data 映射：`./gradlew :core:data:testDebugUnitTest`
- Compose 外观或截图基线：运行相关 Roborazzi Task；PR 和 `main` 的 Linux CI 必须通过 `verifyRoborazziDebug`。
- 文档治理：`./gradlew checkDocumentation`
- Kotlin/KTS 格式：对所有改动模块运行对应的 `spotlessCheck`。
- 提交前：运行最相关测试、改动模块的 Spotless 检查及 `git diff --check`，并说明未运行的较慢检查。
- 发布前：Tag 只能指向已通过完整 Build 与 Roborazzi 门禁的 `main` 提交；Release 不能是该提交首次执行完整验证的地方。
