# 静态验证报告

## 结果

| 检查 | 结果 |
|---|---:|
| API 模块覆盖 | 164/164 |
| 有 PC 消费证据的接口 | 46 |
| 无字段级响应证据 | 117 |
| 未映射 PC 请求路由 | 0 |
| 外部请求 | 0 |
| 实时验证 | 0 |

以上统计只描述静态文档生成过程。

## Android 运行时 Canary

- 2026-08-11：`API-SEARCH-001` 已到达上游网关，但使用未注册的 `dfid=-` 时被业务代码 `152` 拒绝，因此该端点需要有效设备上下文后才能作为正式搜索验证。
- 2026-08-11：参考 `MoeKoeMusic-Mobile-V2@c4b4f1d56c7484580444cf294914fe0601e120bd` 的无签名匿名搜索 Canary 已通过，确认当前网络、基础 JSON 解析与歌曲字段映射可工作。
- Live Test 必须由 `RESONOTE_RUN_LIVE_API_TESTS=true` 显式启用；没有保存原始响应、账号、Cookie 或设备标识。

## 证据优先级

1. API 模块实际构造和转换：`SOURCE_CONFIRMED`。
2. 固定 PC 应用读取字段：`CONSUMER_CONFIRMED`。
3. V2 固定版本实际读取或测试的行为旁证：`REFERENCE_CONFIRMED`；不得覆盖 Lite 源码。
4. `interface.d.ts` 或现有说明：`DECLARED`。
5. 固定仓库已有脱敏样例：`FIXTURE_CONFIRMED`。
6. 静态推断：`INFERRED`。
7. 无证据：`UNKNOWN`。

## 固有限制

静态源码通常透传上游 Body，`interface.d.ts` 的返回值又多为 `ApiResponse<any>`，因此本基线只能完整证明请求构造，不能完整证明所有响应字段、可空性、枚举全集或当前可用性。未列出字段不代表不存在。

## 未映射请求

- 无

## Fixture 状态

固定基线没有发现可证明为完整上游响应且已脱敏的 JSON Fixture。本次不制造样例；[fixtures/README](fixtures/README.md) 记录了准入规则。
