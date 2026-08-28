# ADR-0004：将 Roborazzi 前移到发布前门禁

- 状态：Accepted
- 日期：2026-08-14
- 决策者：Resonote 项目
- 关联文档：[开发指南](../DEVELOPMENT.md)
- 关联实现：[Build workflow](../../.github/workflows/build.yml)、[Release workflow](../../.github/workflows/release.yml)、[DefaultRoborazziOptions](../../core/screenshot-testing/src/main/java/com/resonote/core/screenshottesting/ScreenshotHelper.kt)

## Context

`v0.1.0` 首次发布时，PR 与 `main` Build 只执行 Assemble / Lint，全项目 Roborazzi 验证直到 Tag 触发 Release 后才首次在 Linux Runner 上运行。两张在 macOS 本机通过的截图在 Ubuntu 上各产生约一个像素的差异，`diff_percentage` 为 `1.7677933E-6`。当时公共 `changeThreshold` 为 `0`，因此 Release 被阻断；失败工作流又没有保留 actual / compare，导致第一次修复误判为交互动画时序问题。

## Decision

1. GitHub `Build` 在所有 PR 和 `main` push 上执行 Assemble、Lint 与全项目 `verifyRoborazziDebug`。
2. Build 与 Release 失败时上传 `roborazzi-failure-${github.run_id}`，只保留 Roborazzi outputs、reports 与 test-results，不上传 Keystore 或通用环境文件。
3. Linux CI 是跨平台验证权威环境。Golden 仍是必须人工审查的版本化证据，不自动用 Runner actual 覆盖。
4. `DefaultRoborazziOptions.changeThreshold` 固定为 `0.00001`（`0.001%`）。该值高于本次单像素噪声，但比 Roborazzi 文档中 `0.01`（`1%`）的示例严格 1000 倍。
5. Tag 只能指向已通过完整 GitHub Build 的 `main` SHA。Release 只做复验、签名、打包、校验与 Draft Release 创建。

## Rejected alternatives

- **盲目重录两张 Golden**：会把基线绑定到特定 Runner，且无法解决未来的极少跨平台抗锯齿差异。
- **移除 Release 中的 Roborazzi**：会失去对 Tag 源码的最终复验。
- **大幅放宽阈值**：可能隐藏真实布局、字体、颜色或状态回归。
- **继续只在 Release 执行**：反馈太晚，并使 Tag 处理与调试耦合。

## Consequences

- 截图回归在合并前被发现，Release 不再是第一道完整门禁。
- PR 与 `main` Build 耗时增加，但可使用 Gradle 缓存，并显著降低发布时的返工成本。
- 失败 Artifact 保留 7 天，足以调试且不长期占用存储。
- 阈值只解决极少像素的跨平台噪声；所有有意视觉变更仍需更新 Golden、人工审查并通过 CI。

## Recovery rule

不得移动已公开或已被外部消费的 Tag。如果 Tag 触发后工作流失败，必须先确认没有 Release、没有发布附件、目标范围精确，才可删除并在新的已验证 `main` SHA 上重建同名 Tag。
