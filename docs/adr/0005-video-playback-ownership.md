# ADR-0005：前台 MV 播放资源所有权

- 状态：Accepted
- 日期：2026-08-23
- 决策者：Resonote 项目
- 关联文档：[Resonote Architecture](../ARCHITECTURE.md)

## Context

MV 是独立前台页面：进入前先暂停音乐，获准进入后自动播放；退出后音乐保持暂停。V1 不支持后台视频、MediaSession、画中画、下载或跨页面续播，全屏只是同一 Video Player 的横屏展示状态。

后台音频必须由 `MediaSessionService` 持有，以承接音频焦点、通知、锁屏和系统控制。把 MV 放入同一个 Queue 或 Service 会混合两种生命周期与系统能力；反过来，要求短生命周期 MV 也通过后台音频 Controller 会增加无用的 Session 状态和跨进程协调。

## Decision

1. `:core:playback:api` 与 `:core:playback:service` 只拥有后台音频播放，不拥有 MV。
2. `:feature:video:impl` 是 V1 前台 MV Player 的唯一所有者，可以依赖 Media3 Video UI 和 ExoPlayer。
3. Video Player 只在解析出可播放 URL 且页面仍在组合树中时创建；URL 变化或页面退出时立即移除监听并释放 Player。
4. Video ViewModel 只负责通过 `VideoRepository` 解析地址并表达 Loading、Unavailable、Failed、Ready；不保存 `Player`、`MediaItem` 或 `PlaybackException`。
5. App 负责进入 MV 前向音频 `PlaybackController` 发送暂停意图，以及协调显式全屏方向；退出 MV 不恢复音频。
6. MV 不进入音频 Queue、不复用音频 Session、不持久化播放位置，也不在后台继续播放。
7. 如果未来批准后台视频、画中画、跨页面续播或统一媒体队列，必须以新 ADR 引入独立 `core:video` 合同或扩展媒体 Service，不能在 Feature 内增量模拟后台生命周期。

## Consequences

### Positive

- 音频与 MV 的生命周期、Queue 和系统能力保持分离。
- 前台 MV 资源随页面确定性释放，不创建不需要的 MediaSession。
- Video ViewModel 仍可在不依赖 Media3 的情况下进行 JVM 测试。

### Trade-offs

- Video Compose 测试需要传入 `Player` 边界或使用截图 Fixture，不能直接用后台音频 Fake Controller 覆盖播放控件。
- 未来若增加后台视频能力，需要迁移 Player 所有权，现有 Feature 所有权不能直接扩张。

## Verification

- ViewModel 测试覆盖 URL 解析、重试、不可用和类型化失败。
- 页面测试覆盖 Loading、Unavailable、Failed、Ready 与全屏控制。
- 真机验收覆盖进入 MV 暂停音乐、退出不恢复、全屏方向恢复和离开页面后视频停止。
