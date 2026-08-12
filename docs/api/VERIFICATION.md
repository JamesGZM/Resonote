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

- `LiveApiSearchCanaryTest` 默认跳过，仅在 `RESONOTE_RUN_LIVE_API_TESTS=true` 时运行。
- 首页 Canary 验证每日推荐、私人好歌、推荐歌单和新歌速递均至少返回一个可消费项目；播放地址最多尝试 5 个公开推荐候选，至少一个必须解析出 HTTPS 地址。
- Canary 不需要账号，不下载或播放音频，也不记录完整响应、Cookie、签名或设备标识。
- 2026-08-12 实测首页四组内容接口均非空；播放地址的 5 个当次候选均被服务端判为无授权，因此严格播放 canary 当次失败，保留该失败以反映真实服务状态。

## 证据优先级

1. API 模块实际构造和转换：`SOURCE_CONFIRMED`。
2. 固定 PC 应用读取字段：`CONSUMER_CONFIRMED`。
3. 固定 Mobile 消费端实际读取或测试的行为旁证：`REFERENCE_CONFIRMED`；不得覆盖 Lite 源码。
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
