# 静态验证报告

## 结果

| 检查 | 结果 |
|---|---:|
| API 模块覆盖 | 164/164 |
| 有 PC 消费证据的接口 | 46 |
| 无字段级响应证据 | 118 |
| 未映射 PC 请求路由 | 0 |
| 外部请求 | 0 |
| 实时验证 | 0 |

## 证据优先级

1. API 模块实际构造和转换：`SOURCE_CONFIRMED`。
2. 固定 PC 应用读取字段：`CONSUMER_CONFIRMED`。
3. `interface.d.ts` 或现有说明：`DECLARED`。
4. 固定仓库已有脱敏样例：`FIXTURE_CONFIRMED`。
5. 静态推断：`INFERRED`。
6. 无证据：`UNKNOWN`。

## 固有限制

静态源码通常透传上游 Body，`interface.d.ts` 的返回值又多为 `ApiResponse<any>`，因此本基线只能完整证明请求构造，不能完整证明所有响应字段、可空性、枚举全集或当前可用性。未列出字段不代表不存在。

## 未映射请求

- 无

## Fixture 状态

固定基线没有发现可证明为完整上游响应且已脱敏的 JSON Fixture。本次不制造样例；[fixtures/README](fixtures/README.md) 记录了准入规则。
