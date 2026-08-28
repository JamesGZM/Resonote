---
name: resonote-code-review
description: 审查 Resonote 全仓库、完整工作流、指定模块、Git 提交范围或 Codex 修改后的代码风险。只提供有证据的风险、优先级和建议，不在 Review 请求中直接修复。
---

# Resonote 代码审查

用于审查行为回归、架构边界、媒体时序、协议、持久化、UI 与测试风险。Review 阶段只读；用户明确要求修复后才编辑。

## 范围

- 全仓库：盘点模块后按 App/Navigation、Feature、Data、Network、Persistence、Media、Design System 和 Build Logic 分区。
- 工作流：从入口沿 ViewModel、Repository/Controller、Source、持久化或服务追踪到最终状态。
- Git 范围：先确认 status、upstream 和 base，再审查对应 diff；没有 upstream 时不得猜 base。
- 本轮修改：结合 diff 和被修改代码的调用方审查，不能只看新增行。

## 审查重点

- 行为与状态：Loading/Empty/Error/Offline、恢复、并发、取消、生命周期、导航和配置变化。
- 架构：Feature/API/Impl、Repository、DTO/Entity/Proto、Controller 与平台实现的依赖方向。
- Network：签名、Session、认证分类、敏感信息、非幂等重试和字段兼容。
- Persistence：Migration、Schema、Proto 兼容、账号隔离和删除语义。
- Media：Queue 当前项、后台生命周期、资源释放、音频焦点、录音/导出时序。
- UI：已验收行为、主题 Token、点击轮廓、Insets、文案资源、可访问性和 Golden 更新依据。
- 验证：行为变化是否有聚焦测试，命令是否覆盖所有改动模块。

## 优先级与输出

- `P1`：崩溃、数据丢失、安全、协议破坏、构建或发布阻断。
- `P2`：高概率回归、状态/时序错误、兼容遗漏、分层违规或关键漏测。
- `P3`：低风险维护性、轻微体验或建议型改进。

先按严重度列出 `文件:行号`、影响与最小修复建议，再说明验证、范围和未覆盖假设。没有明确问题时直接说明未发现阻断风险，不凑数量。等待用户授权后再修复。
