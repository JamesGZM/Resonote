---
name: resonote-network-data
description: 维护 Resonote 网络请求、特殊协议、Session、风控、DTO、DataSource、Repository 映射与错误分类。用于 core:network 或跨到 core:data 的远端数据变更；不用于纯本地持久化。
---

# Resonote 网络与数据

用于以当前源码、脱敏测试和现行协议文档为事实源演进网络能力。

## 工作流程

1. 先读根 `AGENTS.md`、`docs/ARCHITECTURE.md`、`docs/api/PROTOCOL.md` 与相关现有测试。
2. 从当前 DataSource、Repository 消费者、DTO、Fixture 和分级运行验证还原真实链路；证据不足时明确报告，不能猜测生产行为。
3. 新能力只有存在 App 消费者时才进入生产代码和 `docs/api/README.md`。

## 协议与分层

- `core:network` 对外暴露语义化 DataSource；Retrofit Service、DTO 和协议 Client 保持内部可见。
- 普通 JSON 使用 Retrofit；二进制、加密或多阶段流程使用共享 OkHttp 的特殊协议实现。
- Policy/Spec 只描述 Method、Origin/Path、签名、Session、默认参数、认证业务码和响应格式等真实行为。
- 签名基于最终 Query 与 Body；取消原样传播，非幂等写入及协议失败不得被拦截器自动重试。
- 认证业务码只由具体请求显式声明，不能全局按数字误判。
- Repository 在边界把 DTO 映射为 `core:model`，并保留认证、离线、协议和业务拒绝的不同意义。
- 不记录 Token、Cookie、设备身份、签名材料或完整真实响应；Fixture 必须最小化并脱敏。

## 验证

- Network 变更运行 `./gradlew :core:network:testDebugUnitTest`。
- Repository/映射变更运行 `./gradlew :core:data:testDebugUnitTest`。
- 测试覆盖 Method、Host/Path、关键参数、签名、Session、映射、错误分类与取消语义，并对改动模块运行 `spotlessCheck`。
