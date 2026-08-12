# 歌词 API

本页记录 1 个 Lite 包装模块。所有结论均来自固定静态源码；“上游请求”才是 Android 直连需要实现的协议。

[返回 API 文档首页](../README.md)

<a id="api-lyrics-001"></a>
## API-LYRICS-001 · 获取歌词（需先调用 /search/lyric 获取 id 和 accesskey）

| 属性 | 值 |
|---|---|
| 模块 | <code>lyric.js</code> |
| Node 包装路由 | <code>/lyric</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | <code>SOURCE_CONFIRMED</code> |
| Cookie 回写 | 未发现 |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/lyric.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://lyrics.kugou.com</code> | <code>/download</code> | <code>GET</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>accesskey</code> | <code>string</code> | 是 | <code>query</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>charset</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>client</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>'android'</code> | <code>SOURCE_CONFIRMED</code> |
| <code>decode</code> | <code>boolean &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>fmt</code> | <code>'lrc' &#124; 'krc'</code> | 否/未知 | <code>query</code> | <code>'krc'</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>id</code> | <code>string</code> | 是 | <code>query</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>ver</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

| Body 路径 | 证据 |
|---|---|
| <code>content</code> | <code>SOURCE_CONFIRMED</code> |
| <code>decodeContent</code> | <code>SOURCE_CONFIRMED</code> |
| <code>contenttype</code> | <code>SOURCE_CONFIRMED</code> |
| <code>status</code> | <code>CONSUMER_CONFIRMED</code> |

这里只列出源码或固定 PC 消费端能够证明的字段；未列出的字段不代表不存在。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>lyric</code> |
| Request DTO | <code>ApiLyricRequest</code> |
| Response DTO | <code>NetworkApiLyricResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code>, <code>ApiResponseDecoder</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiLyricResponse |
