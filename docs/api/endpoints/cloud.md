# 云盘 API

本页记录 3 个 Lite 包装模块。所有结论均来自固定静态源码；“上游请求”才是 Android 直连需要实现的协议。

[返回 API 文档首页](../README.md)

<a id="api-cloud-001"></a>
## API-CLOUD-001 · 获取用户云盘音乐（需登录）

| 属性 | 值 |
|---|---|
| 模块 | <code>user_cloud.js</code> |
| Node 包装路由 | <code>/user/cloud</code> |
| 认证 | <code>required</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>deferred</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | <code>SOURCE_CONFIRMED</code> |
| Cookie 回写 | 未发现 |
| 风控 | <code>common-ssa</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/user_cloud.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://mcloudservice.kugou.com</code> | <code>/v1/get_list</code> | <code>POST</code> | <code>-</code> | <code>none</code> | <code>arraybuffer</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>appid</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>clienttime</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>clientver</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>key</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>mid</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>p</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>page</code> | <code>number</code> | 否/未知 | <code>module</code> | <code>1</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>pagesize</code> | <code>number</code> | 否/未知 | <code>module</code> | <code>30</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>token</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>userid</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

| Body 路径 | 证据 |
|---|---|
| <code>status</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>data.type_size</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>data</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>data.list</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>data.info</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>data.list_count</code> | <code>CONSUMER_CONFIRMED</code> |

这里只列出源码或固定 PC 消费端能够证明的字段；未列出的字段不代表不存在。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>userCloud</code> |
| Request DTO | <code>ApiUserCloudRequest</code> |
| Response DTO | <code>NetworkApiUserCloudResponse</code>；含 UNKNOWN 时先使用宽容中间结构 |
| 传输实现 | <code>OkHttp Call.Factory</code> |
| 协议组件 | <code>ApiSession</code>, <code>ApiDeviceIdentity</code>, <code>ApiResponseDecoder</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiUserCloudResponse |

<a id="api-cloud-002"></a>
## API-CLOUD-002 · 上传音乐文件到用户云盘

| 属性 | 值 |
|---|---|
| 模块 | <code>user_cloud_upload.js</code> |
| Node 包装路由 | <code>/user/cloud/upload</code> |
| 认证 | <code>required</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>write-or-sensitive</code> |
| 产品范围 | <code>deferred</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | <code>SOURCE_CONFIRMED</code> |
| Cookie 回写 | 未发现 |
| 风控 | <code>partial-common-ssa</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/user_cloud_upload.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>native</code> | <code>absolute-url</code> | <code>http://bssulbig.kugou.com/v2/authorization</code> | <code>GET</code> | <code>-</code> | <code>none</code> | <code>json</code> |
| 2 | <code>native</code> | <code>absolute-url</code> | <code>http://bssulbig.kugou.com/multipart/initiate/music</code> | <code>POST</code> | <code>-</code> | <code>none</code> | <code>json</code> |
| 3 | <code>native</code> | <code>dynamic</code> | <code>dynamic: `http://${external_host}/multipart/upload`</code> | <code>POST</code> | <code>-</code> | <code>none</code> | <code>json</code> |
| 4 | <code>native</code> | <code>dynamic</code> | <code>dynamic: `http://${external_host}/multipart/complete`</code> | <code>POST</code> | <code>-</code> | <code>none</code> | <code>json</code> |
| 5 | <code>useAxios</code> | <code>https://mcloudservice.kugou.com</code> | <code>/v1/add_files</code> | <code>POST</code> | <code>-</code> | <code>none</code> | <code>arraybuffer</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>album_audio_id</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>appid</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>audio_id</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>author_name</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>''</code> | <code>SOURCE_CONFIRMED</code> |
| <code>bitrate</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>clienttime</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>clientver</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>extendname</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>filename</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>key</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>list_ver</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>mid</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>name</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>p</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timelen</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>token</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>userid</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

| Body 路径 | 证据 |
|---|---|
| <code>uploadInfo</code> | <code>SOURCE_CONFIRMED</code> |
| <code>msg</code> | <code>SOURCE_CONFIRMED</code> |
| <code>msg.response</code> | <code>SOURCE_CONFIRMED</code> |

这里只列出源码或固定 PC 消费端能够证明的字段；未列出的字段不代表不存在。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>userCloudUpload</code> |
| Request DTO | <code>ApiUserCloudUploadRequest</code> |
| Response DTO | <code>NetworkApiUserCloudUploadResponse</code>；含 UNKNOWN 时先使用宽容中间结构 |
| 传输实现 | <code>OkHttp Call.Factory</code> |
| 协议组件 | <code>ApiSession</code>, <code>ApiDeviceIdentity</code>, <code>ApiResponseDecoder</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiUserCloudUploadResponse |

<a id="api-cloud-003"></a>
## API-CLOUD-003 · 获取用户云盘音乐 URL（需登录，目前文件大小约 10M）

| 属性 | 值 |
|---|---|
| 模块 | <code>user_cloud_url.js</code> |
| Node 包装路由 | <code>/user/cloud/url</code> |
| 认证 | <code>required</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>deferred</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>common-ssa</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/user_cloud_url.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/bsstrackercdngz/v2/query_musicclound_url</code> | <code>GET</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>album_audio_id</code> | <code>string</code> | 否/未知 | <code>query</code> | <code>0</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>album_id</code> | <code>string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>audio_id</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>0</code> | <code>SOURCE_CONFIRMED</code> |
| <code>bucket</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>hash</code> | <code>string</code> | 是 | <code>query</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>key</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>kv_id</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>name</code> | <code>string</code> | 否/未知 | <code>query</code> | <code>''</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>pid</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>ssa_flag</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>ssl</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>version</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>with_res_tag</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

| Body 路径 | 证据 |
|---|---|
| <code>status</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>data</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>data.url</code> | <code>CONSUMER_CONFIRMED</code> |

这里只列出源码或固定 PC 消费端能够证明的字段；未列出的字段不代表不存在。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>userCloudUrl</code> |
| Request DTO | <code>ApiUserCloudUrlRequest</code> |
| Response DTO | <code>NetworkApiUserCloudUrlResponse</code>；含 UNKNOWN 时先使用宽容中间结构 |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiUserCloudUrlResponse |
