# Resonote Dependency Matrix

所有版本与坐标以 `gradle/libs.versions.toml` 为唯一事实源。下表描述当前工程已声明的主要能力，不再维护与源码脱节的“未来 Reserved”清单。

| 能力 | 当前版本 | 主要用途 |
|---|---:|---|
| Gradle / Android Gradle Plugin | `9.4.0` / `9.0.0` | 构建与 Android 插件 |
| Kotlin | `2.3.0` | Kotlin 与 Compose Compiler |
| Compose BOM / Material 3 | `2025.12.00` / `1.4.0` | UI 与设计语义 |
| Navigation 3 / Material Adaptive | `1.0.1` / `1.2.0` | 类型安全导航与自适应布局 |
| Hilt / AndroidX Hilt | `2.59` / `1.3.0` | 依赖注入 |
| Coroutines / Serialization | `1.10.1` / `1.8.0` | 异步流与 JSON |
| OkHttp / Retrofit | `4.12.0` / `2.12.0` | 网络传输与私有 Service |
| Room / DataStore | `2.8.4` / `1.2.1` | 数据库与偏好持久化 |
| Media3 | `1.10.1` | 播放、Session 与媒体数据源 |
| Coil | `3.4.0` | 图片加载 |
| Roborazzi / Robolectric | `1.56.0` / `4.16` | 截图与 Android JVM 测试 |
| JUnit / Truth / Turbine | `4.13.2` / `1.4.4` / `1.2.0` | 单元与 Flow 测试 |
| Spotless | `8.3.0` | 格式化约束 |

Material 3 显式冻结到 `1.4.0`，避免 BOM 更新静默改变主题和组件基线。更新依赖时应先运行最相关模块测试；主题、导航、序列化、数据库和播放依赖升级还需验证对应截图或契约测试。
