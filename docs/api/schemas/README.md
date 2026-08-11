# 静态 Schema 说明

- `requests.yaml` 合并固定 `interface.d.ts` 声明与模块实际构造字段。
- `responses.yaml` 只列 API 转换代码和固定 PC 消费端能够证明的 Body 路径。
- `type: unknown` 是有意保守结果；在获得合规的脱敏响应 Fixture 前不得收紧。
- `path: "*"` + `UNKNOWN` 表示包装层透传响应且没有字段级静态证据。
