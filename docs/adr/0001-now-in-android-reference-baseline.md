# ADR-0001：以 Now in Android 作为参考架构基线

- 状态：Accepted
- 日期：2026-08-10
- 修订：2026-08-23
- 决策者：Resonote 项目
- 关联文档：[Resonote Architecture](../ARCHITECTURE.md)

## Context

Resonote 已冻结 Material3 `1.4.0` 设计系统规范，但尚未创建 Android/Gradle 工程。项目需要一套可扩展、可测试、适合 Compose 与多模块协作的工程架构，同时不能把资讯类示例应用的业务模型或实验依赖直接复制到音乐产品。

Now in Android 是官方 Android 架构指导的完整参考实现，覆盖分层、UDF、离线优先、feature 模块化、Convention Plugins、Hilt、测试替换、同步和性能基线。决策时使用本地 `../nowinandroid`，其提交为 [`7d45eae4f8720a0c77f507712ba2437ff974b6ed`](https://github.com/android/nowinandroid/tree/7d45eae4f8720a0c77f507712ba2437ff974b6ed)。

该快照包含 AGP `9.0.0`、Kotlin `2.3.0`、Compose Foundation Alpha、Material3 Adaptive RC 和 Adaptive Navigation3 Alpha。直接复制全部版本会与 Resonote 的稳定优先策略及 Material3 `1.4.0` 基线冲突。

## Decision

Resonote 采用“原则全量参考、模块按业务裁剪”的策略：

1. 采用 NIA 的 UI/Data/可选 Domain 分层、UDF、Flow 与 Repository。离线优先和本地单一事实源适用于用户持久状态、设备历史、本地媒体和明确批准缓存的首页快照；搜索、榜单、在线歌单、艺人/专辑、云盘和短时播放地址以远端为事实源，除非后续产品合同与 ADR 明确批准离线能力。
2. 采用 `app` 组合根、core 模块、按需拆分的 feature `api/impl`、test-support 和 included `build-logic` 的模块类型与依赖规则。只有存在需要后台更新的本地事实源时才引入 sync；只有需要被其他 feature 独立导航或调用的稳定公共面才建立 `api`；顶层根页面不为形式统一创建空 `api` 模块。
3. 采用 Hilt、Coroutines/Flow、kotlinx.serialization、OkHttp3、Retrofit2、Room、Proto DataStore、Coil、Navigation 3、Roborazzi/Robolectric/Turbine 等库族；WorkManager 在出现真实后台工作后再接入生产代码。
4. 除冻结的 Material3 `1.4.0` 外，不在文档阶段锁定 Resonote 的最终版本；创建工程前形成完整稳定兼容矩阵。
5. Alpha/RC 依赖默认禁止。只有稳定版无法满足已批准需求时，才能通过独立 ADR 引入。
6. 不采用 NIA 的资讯 feature 名称和模型；“首页 / 发现 / 我的”的内部模块边界等待 API 与产品 IA 明确。
7. 不引入 NIA 的 Firebase、Analytics 或其他遥测。未经明确需求和批准，不增加任何遥测或网络上报。
8. 不直接采用 NIA 的内容通知模块。未来播放通知属于独立 playback 域。
9. 冻结 Media3 playback 的模块与依赖边界：Media3-free 的 playback api、持有 Player/Session 的 service、独立的流媒体 cache、可选 download，以及 Player feature；在 Player 设计和 API 契约完成前不创建模块或冻结接口字段。

## Consequences

### Positive

- 后续实现可直接定位 NIA 的真实源码，不需要重新推导模块职责和依赖方向。
- 架构与 Android 官方指导一致，同时保留音乐业务所需的独立演进空间。
- 稳定优先策略避免 NIA `main` 的实验依赖破坏已冻结设计基线。
- Feature、数据源、播放和 UI 的边界可独立测试与替换。

### Trade-offs

- Resonote 不会与 NIA 保持逐文件或逐版本同步；升级需要主动审阅差异。
- 多模块、Convention Plugins 和 test-support 会增加初期设置成本，因此必须按纵向阶段引入，不能一次创建全部空模块。
- API 未确定前，feature 名称、模型、认证与同步语义保持待定，架构文档不能替代产品/API 设计。

## Reference and divergence policy

- 固定提交是当前可复现参考；上游 `main` 的变化不自动改变本决策。
- 调研 NIA 新实现时，先比较 `settings.gradle.kts`、`gradle/libs.versions.toml`、`build-logic/`、架构文档和相关模块测试。`../nowinandroid` 只表示主 checkout 的默认相邻目录；从 Git worktree 工作时必须解析实际 NIA checkout，不能假定该相对路径仍然成立。
- 下列变化必须新增或修订 ADR：分层/依赖方向变化、Material3 基线升级、引入 Alpha/RC、数据源策略变化、Feature IA 冻结、认证方案、Playback 方案、遥测或第三方网络服务。
- 纯补丁版本升级若不改变公共 API、构建约束和运行行为，可在兼容矩阵与依赖更新提交中记录，无需新 ADR。

## Licensing

NIA 使用 Apache License 2.0，Resonote 使用 MIT License。参考架构思想不改变 Resonote 的项目许可；如果后续直接复制或修改 NIA 源代码、资源或测试，必须识别来源并保留适用的版权和 Apache-2.0 声明，不能仅以本 ADR 代替许可合规检查。
