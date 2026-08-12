# 图片 API

本页记录 2 个 Lite 包装模块。所有结论均来自固定静态源码；“上游请求”才是 Android 直连需要实现的协议。

[返回 API 文档首页](../README.md)

<a id="api-images-001"></a>
## API-IMAGES-001 · 获取歌手和专辑图片

| 属性 | 值 |
|---|---|
| 模块 | <code>images.js</code> |
| Node 包装路由 | <code>/images</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/images.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://expendablekmr.kugou.com</code> | <code>dynamic: `/container/v2/image?${query.join('&amp;')}`</code> | <code>GET</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>album_audio_id</code> | <code>string</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>album_id</code> | <code>string</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>count</code> | <code>number</code> | 否/未知 | <code>module</code> | <code>5</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>hash</code> | <code>string</code> | 是 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>signature</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>images</code> |
| Request DTO | <code>ApiImagesRequest</code> |
| Response DTO | <code>NetworkApiImagesResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>OkHttp Call.Factory</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiImagesResponse |

<a id="api-images-002"></a>
## API-IMAGES-002 · 获取歌手图片

| 属性 | 值 |
|---|---|
| 模块 | <code>images_audio.js</code> |
| Node 包装路由 | <code>/images/audio</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/images_audio.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://expendablekmr.kugou.com</code> | <code>dynamic: `/v2/author_image/audio?${query.join('&amp;')}`</code> | <code>GET</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>album_audio_id</code> | <code>string</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>audio_id</code> | <code>string</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>count</code> | <code>number</code> | 否/未知 | <code>module</code> | <code>5</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>filename</code> | <code>string</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>hash</code> | <code>string</code> | 是 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>signature</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>imagesAudio</code> |
| Request DTO | <code>ApiImagesAudioRequest</code> |
| Response DTO | <code>NetworkApiImagesAudioResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>OkHttp Call.Factory</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiImagesAudioResponse |
