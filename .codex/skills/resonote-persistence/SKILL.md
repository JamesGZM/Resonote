---
name: resonote-persistence
description: 演进 Resonote Room、DAO、Entity、Schema、Migration、Proto DataStore 与持久化 Repository。用于数据结构、迁移、兼容、账号隔离或删除语义变更；不用于仅存在于网络响应中的 DTO。
---

# Resonote 持久化

用于在不丢失用户数据、不破坏旧版本兼容的前提下修改本地事实源。

## 工作流程

1. 先读根 `AGENTS.md`、`docs/ARCHITECTURE.md`、`docs/DEVELOPMENT.md` 与目标存储实现和测试。
2. 明确事实源、数据寿命、账号作用域、删除语义和版本兼容要求。
3. 先检查现有 Room Schema、Migration、Proto 编解码与 Repository 映射模式。

## 持久化规则

- Room Entity/DAO/Migration 留在 `core:database`；Proto 定义与 Lite 类型留在 `core:datastore-proto`，存储实现留在 `core:datastore`。
- Feature 只能通过 Repository 使用持久数据，不接触 DAO 或 Proto Store。
- Room Schema 变更必须提升版本、导出新 Schema、注册显式 Migration，并验证所有受支持升级路径。
- 不使用 destructive migration 掩盖缺失迁移；删除或重建数据必须符合已确认产品合同。
- Proto 字段号不得复用；枚举和未知值提供安全回退，新增字段保持旧数据可读。
- 账号数据、设备数据、缓存、导入媒体和公开导出文件必须保持各自清理边界。
- 本地导入副本和 K 歌工程文件不是可随缓存清除的临时数据。

## 验证

- Database 变更运行 `./gradlew :core:database:testDebugUnitTest`，并覆盖迁移与 DAO 行为。
- DataStore 变更运行 `./gradlew :core:datastore:testDebugUnitTest`，覆盖默认值、往返编解码、未知值和旧数据。
- 映射变化同时运行 `:core:data:testDebugUnitTest`；对改动模块运行 `spotlessCheck`。
