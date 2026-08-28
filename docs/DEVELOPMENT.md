# Resonote 开发指南

本文把参考项目中适合 Resonote 的工程方法固化为本项目规则。日常开发应直接遵循本文、[架构](ARCHITECTURE.md) 和各级 `AGENTS.md`，不要求反复打开 NiA。

## 1. 开始任务前

1. 阅读目标目录最近的 `AGENTS.md`。
2. 从 `settings.gradle.kts` 和目标模块的 `build.gradle.kts` 确认真实模块与依赖。
3. 使用 `rg` 找到现有接口、实现和测试；不要根据文档猜测类型或路径。
4. 确认行为事实源：产品合同、设计合同、Network 协议或持久化 Schema。
5. 选择最小可验证改动，不预建没有消费者的抽象。

## 2. 代码放置决策

| 变更 | 所属位置 |
|---|---|
| 页面、UI State、ViewModel、Feature 内组件 | `feature:<name>:impl` |
| 跨 Feature 导航 Key 或稳定入口 | `feature:<name>:api` |
| 跨页面业务数据入口与映射 | `core:data` Repository |
| HTTP、DTO、签名、Session、协议实现 | `core:network` |
| 关系型持久数据 | `core:database` |
| 偏好或小型持久状态 | `core:datastore` / `datastore-proto` |
| 文件导入、媒体扫描和本地 metadata | `core:media:local` |
| K 歌私有素材、录音文件和容量检查 | `core:media:karaoke` |
| 跨页面播放合同 | `core:playback:api` |
| 后台音频 ExoPlayer、Queue、MediaSession、播放恢复 | `core:playback:service` |
| K 歌后台会话、试听与导出合同 | `core:karaoke:api` |
| K 歌麦克风前台服务、Composition 试听与 WorkManager 导出 | `core:karaoke:service` |
| 播放详情页内的 K 歌状态与过渡 | `feature:player:impl` |
| 前台 MV Player 与页面生命周期 | `feature:video:impl`，遵循 ADR-0005 |
| 主题 Token 与通用原子组件 | `core:designsystem` |
| 应用级组合、全局导航、Shell | `app` |

如果一段代码只有一个 Feature 使用，优先留在该 Feature。只有出现真实的第二个消费者并且语义所有权明确时才提升到 Core。

## 3. 新增或修改 Feature

- ViewModel 接收 Repository/Controller，不接收 Retrofit Service、DAO 或 Android 文件实现。
- UI State 使用不可变数据类型，页面通过事件方法表达用户意图。
- 导航参数使用类型安全 Key，只携带恢复目标所需的最小稳定参数，不传整份 DTO。
- 仅在其他模块需要导航或调用该 Feature 时创建 `api`；否则保留单一 `impl`。
- Feature 可以依赖另一个 Feature 的 `api`，不得依赖其 `impl`。

## 4. 新增或修改数据能力

```text
Feature -> Repository interface -> Repository implementation
                              -> Network DataSource
                              -> DAO / DataStore / Local Media
```

- 先定义业务可读的 Repository/DataSource 操作，再实现传输细节。
- Network DTO 和 Room Entity 不离开数据边界；映射后的领域模型进入 `core:model`。
- 明确数据的事实源和刷新策略，避免 UI 同时维护网络与数据库副本。
- 错误必须保留业务意义；认证失败、离线、协议失败和内容为空分别表达。
- 修改 Schema 时提供 Migration 和迁移测试；修改 Proto 枚举时处理未知值回退。

## 5. Network 开发规则

- Retrofit 接口保持 `internal`，方法名表达业务能力。
- `ApiRequestPolicy` 与 `ApiEndpointSpec` 只包含会影响真实请求的字段。
- 特殊业务码由具体请求显式声明，Verifier 从请求 Policy/Spec 读取；不得按人工 Endpoint ID 查表。
- 新增请求必须测试 Method、Host/Path、关键参数、签名、Session、响应映射与错误分类。
- 未被 App 消费的上游接口不进入生产代码或主 API 文档。
- Live 测试默认关闭，凭证和真实身份只能通过安全的本机配置提供。

## 6. Compose 与设计系统

- 业务组件消费 `MaterialTheme.colorScheme`、Typography、Shape 和 design-system token，不复制 Hex 或手写主题分支。
- Navigation、Bottom Bar 等 Material 组件优先使用语义默认映射；品牌覆盖应发生在 Theme，而不是页面单点覆色。
- 真实悬浮层使用统一 Elevation Token；装饰渐变不能冒充组件阴影规范。
- 点击反馈必须与组件的视觉轮廓一致：无容器操作使用 `ResonotePlainAction` 的内容淡化反馈；圆角容器优先使用带相同 `shape` 的 `Surface(onClick)`；只有真实的边到边列表行可以使用完整矩形状态层。不得在圆角 `Surface` 外层或无容器内容上直接添加默认矩形 `clickable`。
- Composable 尽量无状态，状态提升到页面或 ViewModel；预览与截图使用稳定 Fixture。
- 修改冻结组件时同步更新设计合同、行为测试和必要的 Roborazzi 基线。
- 代码审计、Token 治理或“规范对齐”不得顺带重排已通过真机验收的视觉布局；文档落后时应报告冲突并更新文档，不得默认回改代码。
- 重录 Roborazzi Golden 前必须先审查 actual / compare，并能指向已批准的视觉变更；不得将实现回归和批量基线更新合并成无审查的“对齐”提交。

## 7. Build Logic 原则

- 通用构建配置进入单一职责、可组合的 Convention Plugin。
- 模块特有配置留在模块的 `build.gradle.kts`，不要为单模块行为创建“通用”插件。
- 依赖版本只在 Version Catalog 维护。
- Verification Task 声明输入/输出、使用 `verification` group，并在适合时启用缓存。
- 文档任务检查文档结构；源码行为由编译器、Lint 和测试验证，不使用字符串扫描伪造类型安全。

## 8. 测试策略

| 改动 | 最小验证 |
|---|---|
| Network 请求/响应 | `./gradlew :core:network:testDebugUnitTest` |
| Repository 与映射 | `./gradlew :core:data:testDebugUnitTest` |
| DataStore / Database | 对应模块的 `testDebugUnitTest` 与迁移测试 |
| Playback | `./gradlew :core:playback:service:testDebugUnitTest` |
| K 歌协议、存储、DSP 与导出 | `:core:network:testDebugUnitTest`、`:core:database:testDebugUnitTest`、`:core:karaoke:service:testDebugUnitTest` |
| Feature 状态/交互 | 对应 Feature 的 `testDebugUnitTest` |
| 设计组件 | `:core:designsystem:testDebugUnitTest` 和相关 Roborazzi Task |
| 文档 | `./gradlew checkDocumentation` |
| Kotlin/KTS 格式 | 对所有改动模块运行对应的 `spotlessCheck` |

优先使用 Fake 和确定性输入验证状态与输出。Mock 调用次数只在调用本身就是合同的时候使用。提交前至少运行最相关测试、改动模块的 Spotless 检查与 `git diff --check`。

## 9. Release 发布门禁

1. 合并所有发布改动，确认目标 `main` SHA 已通过 GitHub `Build`；该 Build 必须包含 Assemble、Lint 和 `verifyRoborazziDebug`。
2. 确认 `versionName` / `versionCode` 与待创建的 `v<versionName>` 一致，且远程不存在同名 Tag 或 Release。
3. 只在已验证的 `main` SHA 上创建 annotated Tag。Release 工作流用于复验、签名、打包和创建 Draft Release，不承担首次回归检查。
4. 工作流成功后核对 APK、AAB、`SHA256SUMS.txt` 和 Tag 目标，再人工发布 Draft。
5. 已公开或已被外部消费的 Tag 不得移动。只有在 Release 未创建、无发布附件且已确认失败的情况下，才可删除同名 Tag 后在新的已验证 SHA 上重建。

Roborazzi 与 Release 门禁的根因和取舍见 [ADR-0004](adr/0004-roborazzi-release-gate.md)。

## 10. 何时查看参考仓库

以下情况才需要回看固定参考：

- 当前 Resonote 文档没有覆盖的新架构问题。
- 计划升级 build-logic、Compose、Navigation 或测试组织方式。
- 需要核对 PC/Mobile 对某个真实 API 字段或业务行为的消费证据。
- ADR 审查或许可证来源追溯。

参考优先级始终是：Resonote 当前源码与测试 → Resonote 现行合同 → Resonote 架构/开发指南 → 固定外部证据。外部结构不能直接覆盖已经成立的本地设计。
