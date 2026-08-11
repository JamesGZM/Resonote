# ADR-0002：以 MoeKoeMusic 作为功能参考

- 状态：Accepted
- 日期：2026-08-10
- 决策者：Resonote 项目
- 关联文档：[Resonote Architecture](../ARCHITECTURE.md)

## Context

NIA 能提供 Android 工程架构，但不能覆盖音乐产品的登录、媒体目录、本地导入、歌词、歌单、云盘、识曲和 MV 等功能。上级目录存在三份应用实现，其中桌面 `MoeKoeMusic` 最完整地表达产品功能，两个 Mobile 项目提供 Android、JSON 漂移和协议测试证据。

三个应用仓库均为 GPL-2.0-only；Resonote 当前使用 MIT。桌面仓库中的 `api` 是独立 Git submodule，固定提交使用 MIT，但仍需逐文件确认来源与声明。

## Decision

1. 固定 `MoeKoeMusic@52c9833afe2e7fedcba8d5b23ff8d1f9731af73a` 作为产品功能、用户任务和状态语义参考。
2. 固定 `MoeKoeMusic-Mobile-V2@c4b4f1d56c7484580444cf294914fe0601e120bd` 作为 Android 风险、协议测试场景和迁移教训参考。
3. 固定 `MoeKoeMusic-Mobile@ab71195d4cf3297332490fd37704d1ae8973d4c5` 作为响应字段漂移、搜索映射和登录状态行为的补充参考。
4. 固定 `MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb` 作为 MIT API 能力证据；任何迁移必须保留适用许可和来源，并以 Resonote 架构独立实现。
5. 参考优先级为：Resonote 冻结设计 → 已确认产品/API → Android 官方 → NIA 架构 → MoeKoe 功能。旧项目不能覆盖更高层决策。
6. 首页、发现、我的、搜索、本地音乐、登录、Player、歌单详情、用户资料和设置进入候选功能图；模块名和接口仍须由 API/IA 纵切片确认。
7. 云盘、听歌识曲和 MV 保持 Deferred；每日 VIP、评论/社交、插件、PWA、桌面歌词、Touch Bar、全局快捷键和 Electron 更新不进入默认范围。
8. 按 NIA 的边界把远端 DataSource、Endpoint、Network DTO、签名、Cookie、设备与风控协议统一放入 `:core:network`，通过内部 package 隔离具体协议；不额外建立 `:platform:<provider>`。
9. Mobile V2 的 Ktor + OkHttp 技术栈不迁移。Resonote 保持已批准的 OkHttp3 + Retrofit2；特殊请求可直接使用共享 `Call.Factory`。
10. 不复制 GPL 应用代码、样式、图片、文案或测试。参考功能行为后重新建模、重新设计和独立实现。

## Consequences

### Positive

- 功能版图有固定源码证据，不需要从音乐 App 常识重新猜测。
- NIA 架构、Resonote 设计和旧产品能力各自有清晰权威范围。
- 可以复用旧项目已经暴露的失败场景，同时避免继承 Vue/Electron 或轻量 Android 架构债务。

### Trade-offs

- 旧功能不能直接等同于首版范围，仍需逐项完成 API、合规、设计与依赖准入。
- GPL 参考要求保持独立实现纪律；相似行为必须可由产品需求和公开接口解释，而不是源代码翻译。
- Provider 协议可能随服务变化，参考提交只能证明历史能力，不能证明当前可用性。

## Change policy

新增旧产品功能、启用 Deferred 能力、复制任何参考实现或改变许可策略时必须修订 ADR。单纯补充源码路径、测试证据或 API 当前可用性，可以更新主架构的功能矩阵并记录验证日期。
