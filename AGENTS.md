# Resonote 工程规则

本文件补充全局协作规则，作用于整个仓库；子目录的 `AGENTS.md` 可增加更具体的约束。

## 事实来源

1. 当前 Resonote 源码、测试与现行合同。
2. [Resonote 架构](docs/ARCHITECTURE.md) 与 [开发指南](docs/DEVELOPMENT.md)。
3. `../nowinandroid`（固定观察点 `7d45eae4f872`）：仅用于未覆盖问题、升级调研与决策溯源。
4. `../MoeKoeMusic`（固定观察点 `a86cfefb3093`）：PC 功能和实际 API 消费证据。
5. `../MoeKoeMusic-Mobile`（固定观察点 `ab71195d4cf3`）：移动端行为、字段兼容与实际 API 消费证据。

日常开发直接遵循 Resonote 自有架构和开发指南，不要求重复阅读 NiA。参考仓库不能覆盖 Resonote 已验证的行为，也不能直接复制 GPL 项目的实现、样式或资产。升级固定观察点必须单独审查并更新相关 ADR 或文档证据。

## 架构与实现

- 保持 `app/feature -> core:data -> network/database/datastore` 的依赖方向；Feature 不直接调用 Retrofit 或协议客户端。
- Network 对外暴露语义化 DataSource，Retrofit Service 和 DTO 保持内部可见。
- 请求 Policy 只描述签名、Session、默认参数、认证业务码等真实协议行为；不得加入文档编号或 Endpoint 注册表。
- 公共依赖和构建约定进入 Version Catalog 与单一职责 Convention Plugin；模块特有配置留在模块内。
- 不新增遥测、分析或外部网络调用，除非任务明确要求。

## 最小验证

- Network 行为：`./gradlew :core:network:testDebugUnitTest`
- Data 映射：`./gradlew :core:data:testDebugUnitTest`
- 文档治理：`./gradlew checkDocumentation`
- Kotlin/KTS 格式：对所有改动模块运行对应的 `spotlessCheck`。
- 提交前：运行最相关测试、改动模块的 Spotless 检查及 `git diff --check`，并说明未运行的较慢检查。
