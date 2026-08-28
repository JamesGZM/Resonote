---
name: resonote-docs-maintenance
description: 维护 Resonote README、AGENTS、docs、design、ADR、文档结构和链接。用于产品介绍、长期工程合同或文档治理；不用于保留单次 QA 日志、构建产物或本机路径记录。
---

# Resonote 文档维护

用于让长期文档与当前源码、测试和用户已验收行为保持一致。

## 工作流程

1. 先读根 `AGENTS.md`、`README.md` 与 `docs/README.md`。
2. 按主题读取现行产品、设计、架构或协议文档，并搜索所有入站链接。
3. 文档与用户已验收行为冲突时保留现行行为，报告并修正文档，不能借清理回改实现。

## 文档规则

- 根 README 是产品介绍和源码运行入口；详细工程规则放在 `docs/` 或 `design/`。
- 当前合同写入产品、设计、架构或协议文档；重大技术取舍写入 ADR，失效讨论由 Git、Issue 或 Pull Request 保留。
- 不提交临时计划、单次 QA 过程、机器生成对比报告、构建路径或本机绝对路径。
- 文档事实来源限于 Resonote 当前源码、测试、现行合同和用户验收。
- Android 官方文档可用于平台 API 事实；已有第三方代码的 NOTICE 与许可证不得因文档清理删除或弱化。
- 使用相对链接，移动或删除文件前检查引用；设计合同删除时同步处理失去引用的设计资产。

## 验证

- 运行 `./gradlew checkDocumentation` 和 `git diff --check`。
- 只改文档时不运行 Android 单元测试或 Roborazzi。
- 全文检查失效路径、外部开发对照、本机绝对路径和已删除文件名。
