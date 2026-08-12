# ADR-0003：后台播放基础与 Controller 合同

- 状态：Accepted
- 日期：2026-08-13
- 决策者：Resonote 项目
- 关联文档：[Resonote Architecture](../ARCHITECTURE.md)

## Context

首页、搜索、歌单、专辑、歌手、榜单、云盘和听歌识曲已经能产生统一的歌曲播放意图，但 App 仍由 Compose 内的 `PrototypePlaybackState` 模拟队列、暂停和切歌。它没有解析真实歌曲 URL、音频焦点、后台播放、系统媒体控制或准确进度，也无法作为 Full Player、Queue 和歌词的事实源。

Android 官方要求后台播放把 Player 与 MediaSession 放入 `MediaSessionService`，界面通过 `MediaController` 通信。Resonote 的 Feature 还需要 JVM 可测试的公共合同，不能依赖或暴露 Media3 类型。

## Decision

1. 创建 `:core:playback:api`，只依赖 `core:model` 与 Coroutines/Flow。公共合同包含播放条目、队列、当前索引、状态、位置、时长、缓冲、模式、类型化问题与控制命令，不暴露 `Player`、`MediaItem`、`PlaybackException`、`SessionToken` 或 Android Service。
2. 创建 `:core:playback:service`。只有 `ResonotePlaybackService` 创建、持有和释放 ExoPlayer 与 MediaSession；标准媒体通知、音频焦点、耳机变得嘈杂事件和系统播放控制由 Media3 承接。
3. `DefaultPlaybackController` 位于 service 模块，通过真实 `SongPlaybackRepository` 解析在线歌曲，通过 `MediaController` 向 Service 发送媒体项和命令，并把 Player 事件映射回 `PlaybackState`。
4. Controller 维护唯一语义 Queue。显式“播放全部”替换 Queue；单曲点击跳转已有条目或插入当前条目之后；追加按歌曲 hash 去重。每个条目保留在线或云盘来源语义，分别交给 `SongPlaybackRepository` 或 `CloudRepository` 解析；云盘入口可把已经验证的当前直链附加到选中条目。
5. 只解析当前选中歌曲，不为整张歌单提前请求短时播放 URL。新的播放目标使用递增 load generation；旧请求返回后不得覆盖新目标。暂停、识曲或进入 MV 会使当前解析代际失效，之后保持暂停。
6. 第一阶段 MediaSession 只承载已经解析的当前媒体项，系统 play/pause、metadata、通知和音频焦点立即可用。把完整 Queue 逐项交给 Session、系统 next/previous、错误自动跳过、队列持久化、随机历史、音质切换与恢复快照留给后续 playback service 原子切片；UI 不得在此期间建立第二份 Queue 补偿。
7. App Scaffold 只消费 `PlaybackState` 并发送 Controller 命令。Mini Player 继续使用既有设计系统外观，但播放状态与进度改为真实 Player 事实；解析和播放失败通过类型化问题给出用户反馈。

## Consequences

### Positive

- 当前所有歌曲入口首次共享真实 URL 解析、后台播放、音频焦点、系统通知和进度事实源。
- Full Player、Queue 与歌词可以在不接触 Media3 的情况下继续实现，并能使用 fake Controller 做 JVM/Compose 测试。
- 选择歌曲只产生当前歌曲的一次 URL 请求，避免大歌单提前解析造成请求放大和短时 URL 过期。
- MV 与识曲暂停不再受并发 URL 请求返回影响。

### Trade-offs

- 第一阶段的系统 Session 队列只有当前媒体项，蓝牙/系统 next/previous 要等待完整 Session Queue 切片。
- Queue 尚未持久化，进程死亡后不会恢复。
- 随机模式目前只保证不会在有候选项时立即重复当前曲目，尚未保留可逆随机历史。
- 云盘条目会保留完整 `CloudTrack` 供后续切歌解析，因此公共 Queue 条目比只含 `OnlineSong` 的模型更大；它换取了来源正确性，且不把 provider DTO 泄漏到播放域。

## Verification

- 纯状态机测试覆盖插播、播放全部去重、云盘来源升级、顺序边界、列表循环和随机不立即重复。
- App ViewModel 测试覆盖普通歌曲、云盘选中直链、云盘追加及 MV/识曲暂停委托。
- 合并 Manifest 必须包含 `FOREGROUND_SERVICE`、`FOREGROUND_SERVICE_MEDIA_PLAYBACK` 和 `MediaSessionService` 声明。
- 真机验收必须覆盖通知栏/锁屏控制、耳机拔出暂停、后台连续播放、来电/导航音频焦点及真实 CDN URL；JVM 测试不能替代这些证据。

## References

- [Background playback with a MediaSessionService](https://developer.android.com/media/media3/session/background-playback)
- [Connect to a media app](https://developer.android.com/media/media3/session/connect-to-media-app)
- [Control and advertise playback using a MediaSession](https://developer.android.com/media/media3/session/control-playback)
