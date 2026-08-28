---
name: resonote-media-playback
description: 修改 Resonote Media3 播放、Queue、MediaSession、音频焦点、桌面歌词、MV、K 歌录制试听导出或媒体生命周期。用于影响播放事实、资源所有权、时序和后台服务的任务。
---

# Resonote 媒体播放

用于保护后台播放、前台视频、悬浮歌词和 K 歌链路各自的资源所有权与生命周期。

## 工作流程

1. 先读根 `AGENTS.md`、`docs/ARCHITECTURE.md`、`docs/DEVELOPMENT.md` 与相关 Playback/Video ADR。
2. 沿 Controller、Service、Repository、Player/Composition 和 Feature 状态追踪完整时序。
3. 修改前定位 Queue、恢复、失败、取消、切歌、前台服务和资源释放测试。

## 媒体规则

- `core:playback:api` 保持 Media3-free；音频 Player、Queue、MediaSession 与恢复由 `core:playback:service` 持有。
- 页面销毁或导航切换不能停止后台音频；Feature 只发送意图并观察 `PlaybackController`。
- MV 是页面持有的前台资源，退出即释放，不加入后台 Queue、不复用短时音频 URL、不提供后台或画中画。
- 桌面歌词服务只消费播放与歌词事实；窗口或权限失败不得改变播放状态。
- K 歌 API 不暴露 Media3；录音前台服务、Composition 试听和 WorkManager 导出留在 `core:karaoke:service`。
- 保持音频焦点、取消、await 顺序、Queue 当前项、切歌与失败恢复语义；非明确需求不改变时间阈值。
- 录音、私有素材、导出文件和删除工程遵循各自存储合同，不把用户文件当缓存清理。

## 验证

- Playback 运行 `./gradlew :core:playback:service:testDebugUnitTest`。
- K 歌按范围运行 Network、Database、Data 与 `:core:karaoke:service:testDebugUnitTest`。
- Feature UI/状态变化运行对应测试和必要 Roborazzi；对所有改动模块运行 `spotlessCheck`。
