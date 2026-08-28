# Network Verification

## 自动化证据

| 范围 | 证据 | 状态 |
|---|---|---|
| Retrofit Path、Query、Body、Header、签名与 Session | [ApiRequestInterceptorsTest](../../core/network/src/test/java/com/resonote/core/network/protocol/ApiRequestInterceptorsTest.kt)、[ApiNetworkDataSourceTest](../../core/network/src/test/java/com/resonote/core/network/retrofit/ApiNetworkDataSourceTest.kt) | 本地 JUnit |
| 特殊协议 Method/Origin/Path、加密与取消 | [protocol tests](../../core/network/src/test/java/com/resonote/core/network/protocol/) | 本地 JUnit |
| Search 业务码认证分类及非 Search 隔离 | [AuthenticationFailureClassifierTest](../../core/network/src/test/java/com/resonote/core/network/AuthenticationFailureClassifierTest.kt)、[ApiNetworkDataSourceTest](../../core/network/src/test/java/com/resonote/core/network/retrofit/ApiNetworkDataSourceTest.kt) | 本地 JUnit |
| DTO 到领域模型与 Repository 错误映射 | [core:data tests](../../core/data/src/test/) | 本地 JUnit |
| 文档入口、相对链接和已删除引用 | 根 `checkDocumentation` | Gradle Verification Task |

测试证据引用稳定的测试文件或目录，不把易变的方法名当作文档协议。文档任务不扫描 Network 源码，也不证明请求行为正确。

## 运行级别

- Fixture：最小化、脱敏的协议回归输入；不能证明上游当前可用。
- Canary：仅在显式环境开关启用，验证匿名可访问能力，不保存完整响应或设备身份。
- 真实账号：只验证必须登录的流程；凭证通过环境或本机安全存储提供，不写入仓库。

`LiveApiSearchCanaryTest` 等 Live 测试默认跳过。历史某次运行结果不能自动升级为当前可用性结论；记录结果时必须写明日期、范围、账号级别和失败限制。

当前协议事实由 Resonote 源码、脱敏 Fixture、单元测试和明确分级的运行验证共同支撑。静态实现不能替代服务可用性验证，也不授权记录敏感协议材料。
