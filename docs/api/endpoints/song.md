# 歌曲 API

本页记录 12 个 Lite 包装模块。所有结论均来自固定静态源码；“上游请求”才是 Android 直连需要实现的协议。

[返回 API 文档首页](../README.md)

<a id="api-song-001"></a>
## API-SONG-001 · 获取音乐相关信息

| 属性 | 值 |
|---|---|
| 模块 | <code>audio.js</code> |
| Node 包装路由 | <code>/audio</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/audio.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>http://kmr.service.kugou.com</code> | <code>/v1/audio/audio</code> | <code>POST</code> | <code>kmr.service.kugou.com</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>appid</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>clienttime</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>clientver</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>Content-Type</code> | <code>unknown</code> | 否/未知 | <code>header</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>cookie</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>dfid</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>&lt;redacted&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>hash</code> | <code>string</code> | 是 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>key</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>token</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>0</code> | <code>SOURCE_CONFIRMED</code> |
| <code>userid</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>0</code> | <code>SOURCE_CONFIRMED</code> |
| <code>x-router</code> | <code>unknown</code> | 否/未知 | <code>header</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>audio</code> |
| Request DTO | <code>ApiAudioRequest</code> |
| Response DTO | <code>NetworkApiAudioResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code>, <code>ApiDeviceIdentity</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiAudioResponse |

<a id="api-song-002"></a>
## API-SONG-002 · 获取最佳伴奏信息

| 属性 | 值 |
|---|---|
| 模块 | <code>audio_accompany_matching.js</code> |
| Node 包装路由 | <code>/audio/accompany/matching</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | <code>SOURCE_CONFIRMED</code> |
| Cookie 回写 | 未发现 |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/audio_accompany_matching.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://nsongacsing.kugou.com</code> | <code>/sing7/accompanywan/json/v2/cdn/optimal_matching_accompany_2_listen.do</code> | <code>GET</code> | <code>-</code> | <code>none</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>appid</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>fileName</code> | <code>string</code> | 是 | <code>query</code> | <code>''</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>hash</code> | <code>string</code> | 是 | <code>query</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>isteen</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>mixid</code> | <code>string</code> | 是 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>mixId</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>platform</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>usemkv</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>version</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>audioAccompanyMatching</code> |
| Request DTO | <code>ApiAudioAccompanyMatchingRequest</code> |
| Response DTO | <code>NetworkApiAudioAccompanyMatchingResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiSession</code>, <code>ApiResponseDecoder</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiAudioAccompanyMatchingResponse |

<a id="api-song-003"></a>
## API-SONG-003 · 获取音乐 K 歌数量（参数来自 /audio/accompany/matching）

| 属性 | 值 |
|---|---|
| 模块 | <code>audio_ktv_total.js</code> |
| Node 包装路由 | <code>/audio/ktv/total</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | <code>SOURCE_CONFIRMED</code> |
| Cookie 回写 | 未发现 |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/audio_ktv_total.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://acsing.service.kugou.com</code> | <code>/sing7/listenguide/json/v2/cdn/listenguide/get_total_opus_num_v02.do</code> | <code>GET</code> | <code>-</code> | <code>none</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>appid</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>isteen</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>platform</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>singerName</code> | <code>string</code> | 是 | <code>query</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>songHash</code> | <code>string</code> | 是 | <code>query</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>songId</code> | <code>string</code> | 是 | <code>query</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>usemkv</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>version</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>audioKtvTotal</code> |
| Request DTO | <code>ApiAudioKtvTotalRequest</code> |
| Response DTO | <code>NetworkApiAudioKtvTotalResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiSession</code>, <code>ApiDeviceIdentity</code>, <code>ApiResponseDecoder</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiAudioKtvTotalResponse |

<a id="api-song-004"></a>
## API-SONG-004 · 获取更多版本音乐

| 属性 | 值 |
|---|---|
| 模块 | <code>audio_related.js</code> |
| Node 包装路由 | <code>/audio/related</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | <code>SOURCE_CONFIRMED</code> |
| Cookie 回写 | 未发现 |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/audio_related.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://listkmrp3cdnretry.kugou.com</code> | <code>dynamic: !show_detail ? '/v3/album_audio/related' : '/v2/audio_related/total'</code> | <code>GET</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>album_audio_id</code> | <code>string</code> | 是 | <code>module</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>page</code> | <code>number</code> | 否/未知 | <code>module</code> | <code>1</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>pagesize</code> | <code>number</code> | 否/未知 | <code>module</code> | <code>30</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>show_detail</code> | <code>0 &#124; 1</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>show_type</code> | <code>boolean &#124; string</code> | 否/未知 | <code>module</code> | <code>0</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>sort</code> | <code>AudioRelatedSort</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>type</code> | <code>string</code> | 否/未知 | <code>module</code> | <code>0</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>audioRelated</code> |
| Request DTO | <code>ApiAudioRelatedRequest</code> |
| Response DTO | <code>NetworkApiAudioRelatedResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>OkHttp Call.Factory</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code>, <code>ApiResponseDecoder</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiAudioRelatedResponse |

<a id="api-song-005"></a>
## API-SONG-005 · 获取歌曲对应的 MV

| 属性 | 值 |
|---|---|
| 模块 | <code>kmr_audio_mv.js</code> |
| Node 包装路由 | <code>/kmr/audio/mv</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/kmr_audio_mv.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/kmr/v1/audio/mv</code> | <code>POST</code> | <code>openapi.kugou.com</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>album_audio_id</code> | <code>string</code> | 是 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>data</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>fields</code> | <code>string</code> | 否/未知 | <code>body</code> | <code>''</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>KG-TID</code> | <code>unknown</code> | 否/未知 | <code>header</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>x-router</code> | <code>unknown</code> | 否/未知 | <code>header</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>kmrAudioMv</code> |
| Request DTO | <code>ApiKmrAudioMvRequest</code> |
| Response DTO | <code>NetworkApiKmrAudioMvResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiKmrAudioMvResponse |

<a id="api-song-006"></a>
## API-SONG-006 · 获取音乐专辑/歌手信息

| 属性 | 值 |
|---|---|
| 模块 | <code>krm_audio.js</code> |
| Node 包装路由 | <code>/krm/audio</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/krm_audio.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/kmr/v2/audio</code> | <code>POST</code> | <code>openapi.kugou.com</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>album_audio_id</code> | <code>string</code> | 是 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>data</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>fields</code> | <code>string</code> | 否/未知 | <code>body</code> | <code>'base'</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>KG-TID</code> | <code>unknown</code> | 否/未知 | <code>header</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>x-router</code> | <code>unknown</code> | 否/未知 | <code>header</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>krmAudio</code> |
| Request DTO | <code>ApiKrmAudioRequest</code> |
| Response DTO | <code>NetworkApiKrmAudioResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiKrmAudioResponse |

<a id="api-song-007"></a>
## API-SONG-007 · 获取音乐详情

| 属性 | 值 |
|---|---|
| 模块 | <code>privilege_lite.js</code> |
| Node 包装路由 | <code>/privilege/lite</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/privilege_lite.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/v2/get_res_privilege/lite</code> | <code>POST</code> | <code>media.store.kugou.com</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>album_id</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>appid</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>area_code</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>behavior</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>clientver</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>Content-Type</code> | <code>unknown</code> | 否/未知 | <code>header</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>hash</code> | <code>string</code> | 是 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>need_hash_offset</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>qualities</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>relate</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>resource</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>support_verify</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>x-router</code> | <code>unknown</code> | 否/未知 | <code>header</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>privilegeLite</code> |
| Request DTO | <code>ApiPrivilegeLiteRequest</code> |
| Response DTO | <code>NetworkApiPrivilegeLiteResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiPrivilegeLiteResponse |

<a id="api-song-008"></a>
## API-SONG-008 · 获取歌曲高潮部分时间

| 属性 | 值 |
|---|---|
| 模块 | <code>song_climax.js</code> |
| Node 包装路由 | <code>/song/climax</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/song_climax.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://expendablekmrcdn.kugou.com</code> | <code>/v1/audio_climax/audio</code> | <code>GET</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>data</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>hash</code> | <code>string</code> | 是 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |

### 返回值证据

| Body 路径 | 证据 |
|---|---|
| <code>status</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>data</code> | <code>CONSUMER_CONFIRMED</code> |

这里只列出源码或固定 PC 消费端能够证明的字段；未列出的字段不代表不存在。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>songClimax</code> |
| Request DTO | <code>ApiSongClimaxRequest</code> |
| Response DTO | <code>NetworkApiSongClimaxResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiSongClimaxResponse |

<a id="api-song-009"></a>
## API-SONG-009 · 获取歌曲成绩单信息

| 属性 | 值 |
|---|---|
| 模块 | <code>song_ranking.js</code> |
| Node 包装路由 | <code>/song/ranking</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/song_ranking.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/grow/v1/song_ranking/play_page/ranking_info</code> | <code>GET</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>album_audio_id</code> | <code>string</code> | 是 | <code>query</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>songRanking</code> |
| Request DTO | <code>ApiSongRankingRequest</code> |
| Response DTO | <code>NetworkApiSongRankingResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiSongRankingResponse |

<a id="api-song-010"></a>
## API-SONG-010 · 获取更详细的歌曲成绩单信息（需登录）

| 属性 | 值 |
|---|---|
| 模块 | <code>song_ranking_filter.js</code> |
| Node 包装路由 | <code>/song/ranking/filter</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/song_ranking_filter.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/grow/v1/song_ranking/unlock/v2/ranking_filter</code> | <code>GET</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>album_audio_id</code> | <code>string</code> | 是 | <code>query</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>page</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>1</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>pagesize</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>30</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>songRankingFilter</code> |
| Request DTO | <code>ApiSongRankingFilterRequest</code> |
| Response DTO | <code>NetworkApiSongRankingFilterResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiSongRankingFilterResponse |

<a id="api-song-011"></a>
## API-SONG-011 · 获取音乐 URL（需先调用 /register/dev 获取 dfid）

| 属性 | 值 |
|---|---|
| 模块 | <code>song_url.js</code> |
| Node 包装路由 | <code>/song/url</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/song_url.js</code> |

### Android 首页迁移证据

- <code>MoeKoeMusic-Mobile/api@283f1e97:module/song_url.js</code>
- <code>MoeKoeMusic-Mobile@ab71195d4cf3297332490fd37704d1ae8973d4c5:src/features/player/song-url.ts</code>

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/v5/url</code> | <code>GET</code> | <code>trackercdn.kugou.com</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>album_audio_id</code> | <code>string</code> | 否/未知 | <code>query</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>album_id</code> | <code>string</code> | 否/未知 | <code>query</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>area_code</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>behavior</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>cdnBackup</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>clientver</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>cmd</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>free_part</code> | <code>boolean &#124; string</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>hash</code> | <code>string</code> | 是 | <code>query</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>module</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>page_id</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>pid</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>pidversion</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>ppage_id</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>quality</code> | <code>SongQuality</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>ssa_flag</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>version</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>x-router</code> | <code>unknown</code> | 否/未知 | <code>header</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

| Body 路径 | 证据 |
|---|---|
| <code>data.error</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>data.status</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>status</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>url</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>timeLength</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>volume</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>volume_gain</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>volume_peak</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>extName</code> | <code>CONSUMER_CONFIRMED</code> |

这里只列出源码或固定 PC 消费端能够证明的字段；未列出的字段不代表不存在。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>songUrl</code> |
| Request DTO | <code>ApiSongUrlRequest</code> |
| Response DTO | <code>NetworkApiSongUrlResponse</code>；名称为静态候选，现行实现由 internal <code>@Serializable</code> 类型化 wire DTO 直接承接 Retrofit 响应 |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code>, <code>ApiDeviceIdentity</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiSongUrlResponse |

<a id="api-song-012"></a>
## API-SONG-012 · 获取音乐 URL（新版，一次性返回所有音质，但存在音频加密）

| 属性 | 值 |
|---|---|
| 模块 | <code>song_url_new.js</code> |
| Node 包装路由 | <code>/song/url/new</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | <code>SOURCE_CONFIRMED</code> |
| Cookie 回写 | 未发现 |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/song_url_new.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>http://tracker.kugou.com</code> | <code>/v6/priv_url</code> | <code>POST</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>album_audio_id</code> | <code>string</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>area_code</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>behavior</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>dfid</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>free_part</code> | <code>boolean &#124; string</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>hash</code> | <code>string</code> | 是 | <code>module</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>qualities</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>quality</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>token</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>userid</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>&lt;redacted&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>vip</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>vip_token</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>vipType</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>0</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>songUrlNew</code> |
| Request DTO | <code>ApiSongUrlNewRequest</code> |
| Response DTO | <code>NetworkApiSongUrlNewResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code>, <code>ApiDeviceIdentity</code>, <code>ApiResponseDecoder</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiSongUrlNewResponse |
