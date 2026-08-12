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
- 首页 Canary 验证每日推荐、私人好歌、推荐歌单和新歌速递均至少返回一个可消费项目；播放地址最多尝试 5 个公开推荐候选，至少一个必须由服务原生返回 HTTPS 地址。
- 首页入口 Canary 验证排行榜列表非空、前三个公开榜单至少一个返回可消费歌曲，并验证前三个公开推荐歌单至少一个返回详情和歌曲。
- Canary 不需要账号，不下载或播放音频，也不记录完整响应、Cookie、签名或设备标识。
- 2026-08-12 类型化 DTO 复测确认每日推荐当次把 `relate_goods` 返回为对象；Android 已按 Mobile 的 `Array.isArray` 消费语义处理为“非数组即无可用档位”，并增加该真实结构的协议回归。
- 同日内容复测中，设备注册从解密 Body 或响应 Cookie 取得 `dfid`；每日推荐、私人好歌、推荐歌单、新歌速递、排行榜、榜单歌曲、歌单详情和歌单歌曲均返回可消费数据。
- 同日播放服务对所试候选只返回 HTTP URL；旧实现改写 scheme 后仅断言字符串前缀，不能证明 TLS 或媒体地址可用。该结果现按协议失败处理，不记作 HTTPS Canary 通过。
- 同日一个匿名 VIP 候选返回 `error_code=35104`；该响应按 Mobile 的无地址消费语义映射为候选级 `PlaybackUnavailableReason.Vip`，Canary 继续尝试后续歌曲，不把 VIP 限制本身记作协议失败或整组成功。
- Mobile API 文档明确说明匿名搜索可能返回业务码 `152` 并要求认证 Cookie。本批 Canary 不需要账号，因此该已知响应记为搜索用例跳过。

## 证据优先级

1. API 模块实际构造和转换：`SOURCE_CONFIRMED`。
2. 固定 PC 应用读取字段：`CONSUMER_CONFIRMED`。
3. 固定 Mobile 消费端实际读取或测试的行为旁证：`REFERENCE_CONFIRMED`；不得覆盖 Lite 源码。
4. `interface.d.ts` 或现有说明：`DECLARED`。
5. 固定仓库已有脱敏样例：`FIXTURE_CONFIRMED`。
6. 静态推断：`INFERRED`。
7. 无证据：`UNKNOWN`。

## 固有限制

静态源码通常透传上游 Body；`interface.d.ts` 的 `ApiResponse<T = any>` 可以证明 Node 层统一 HTTP 调用结果（`status/body/headers/cookie`），但默认 `any` 不能证明各端点 Body `T` 的完整字段。因此本基线只能完整证明请求构造与外层传输契约，不能完整证明所有服务端 Body 字段、可空性、枚举全集或当前可用性。未列出字段不代表不存在。

## 未映射请求

- 无

## Fixture 状态

固定基线没有发现可证明为完整上游响应且已脱敏的 JSON Fixture。本次不制造样例；[fixtures/README](fixtures/README.md) 记录了准入规则。
