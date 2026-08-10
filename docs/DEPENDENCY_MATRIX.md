# Resonote Dependency Matrix

> 状态：项目基座版本矩阵
> 验证日期：2026-08-10
> 规则：所有坐标统一维护在 `gradle/libs.versions.toml`

## 状态定义

- **Active**：当前模块真实声明并已通过 Debug APK 构建。
- **Reserved**：版本和坐标已登记，但尚未进入生产依赖图；创建对应模块时必须重新验证。
- 登记 Reserved 坐标不代表对应能力或业务接口已经实现。

## 构建与 Active 依赖

| 能力 | 版本 | 状态 | 当前消费者 |
|---|---:|---|---|
| Gradle | `9.4.0` | Active | Wrapper |
| Android Gradle Plugin | `9.0.0` | Active | build-logic、全部 Android 模块 |
| Kotlin / Compose Compiler Plugin | `2.3.0` | Active | build-logic、Compose 模块 |
| Compose BOM | `2025.12.00` | Active | Compose Convention Plugins |
| Material3 | `1.4.0` | Active；显式冻结 | app、catalog、designsystem |
| Activity Compose | `1.12.3` | Active | app、catalog |
| AndroidX Core | `1.18.0` | Active | app、catalog |
| Core Library Desugaring | `2.1.5` | Active | 全部 Android 模块 |

Material3 显式版本优先于 BOM 中的对应约束，防止设计基线被 Compose BOM 升级静默改变。当前构建不使用 Dynamic Color、Material3 Expressive 或 Material3 internal API。

## Reserved 依赖

| 能力 | 版本 | 启用条件 |
|---|---:|---|
| KSP | `2.3.4` | Hilt、Room 或其他代码生成模块创建时 |
| Lifecycle | `2.10.0` | App Shell/ViewModel 接入时 |
| Navigation 3 | `1.0.1` | App Shell 导航边界确认时 |
| Material3 Adaptive | `1.2.0` | Adaptive App Shell 实现时 |
| Hilt | `2.59` | 首个需要 DI 的生产纵切片 |
| Coroutines | `1.10.1` | Data/Domain/Playback API 创建时 |
| kotlinx.serialization | `1.8.0` | Network DTO 契约确认时 |
| OkHttp | `4.12.0` | `core:network` 创建时 |
| Retrofit2 | `2.12.0` | `core:network` 创建时 |
| Room | `2.8.4` | 本地事实源 schema 确认时 |
| DataStore | `1.2.1` | 偏好 schema 确认时 |
| WorkManager | `2.11.2` | Sync/持久后台任务确认时 |
| Coil 3 | `3.5.0` | 网络图片装配时 |
| Media3 | `1.10.1` | Playback ADR 接受后 |
| Robolectric | `4.16` | JVM Android 测试基座创建时 |
| Roborazzi | `1.56.0` | Screenshot Testing 模块创建时 |
| Turbine | `1.2.0` | Flow 行为测试创建时 |
| JUnit | `4.13.2` | 首批单元测试创建时 |
| Truth | `1.4.4` | 首批断言工具引入时 |
| Protobuf / Plugin | `4.29.2` / `0.9.6` | Proto DataStore schema 创建时 |
| Spotless | `8.3.0` | 格式化门禁启用时 |
| Dependency Guard | `0.5.0` | 依赖快照门禁启用时 |

## 当前验证

已执行：

```bash
./gradlew :app:assembleDebug :app-resonote-catalog:assembleDebug
```

该结果只证明当前 Active 构建链和依赖图可编译、可打包。Reserved 依赖尚未形成兼容性结论，V-01–V-10 仍为 `Not Run`。
