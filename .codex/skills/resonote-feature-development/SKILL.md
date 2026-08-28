---
name: resonote-feature-development
description: 构建或修改 Resonote Android 业务功能。用于页面流程、ViewModel/UI State、Repository 编排、Navigation 3 入口或跨层用户工作流；纯视觉、网络协议、持久化和媒体底层任务应同时使用对应专业 Skill。
---

# Resonote 功能开发

用于按 Resonote 当前架构、源码、测试和产品合同实现业务功能。

## 工作流程

1. 先读仓库根 `AGENTS.md`、`docs/ARCHITECTURE.md` 与 `docs/DEVELOPMENT.md`。
2. 行为或范围不明确时读 `design/PRODUCT_REQUIREMENTS.md`，并从当前源码与测试确认实现状态。
3. 从 `settings.gradle.kts`、目标模块和现有测试确认真实入口、依赖与本地模式。
4. 沿 `UI -> ViewModel -> Repository/Controller -> Source` 追踪完整链路，再选择最小改动。

## 边界

- 页面、不可变 UI State、ViewModel 和 Feature 内组件放在 `feature:<name>:impl`。
- 只有存在跨 Feature 消费者时才创建或扩展 `feature:<name>:api`；导航 Key 只携带恢复目标所需的稳定参数。
- Feature 不直接调用 Retrofit、DAO、Proto Store 或文件实现，也不依赖其他 Feature 的 `impl`。
- UI 通过事件方法表达意图，不维护 Repository 或 Controller 已拥有的业务事实副本。
- Loading、Empty、Offline、认证、协议失败和业务限制保持独立语义；已有内容刷新失败时保留可用内容。
- 跨多个 Repository、被多个 ViewModel 复用或有独立测试价值时才引入领域操作。

## 验证

- 行为变更补充或更新目标 Feature 的聚焦测试。
- 运行目标模块 `testDebugUnitTest` 和 `spotlessCheck`，最后运行 `git diff --check`。
- 涉及 UI、Network、Persistence 或 Media 时，按对应专业 Skill 增加验证。
