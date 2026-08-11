# 歌单 API

本页记录 10 个 Lite 包装模块。所有结论均来自固定静态源码；“上游请求”才是 Android 直连需要实现的协议。

[返回 API 文档首页](../README.md)

<a id="api-playlist-001"></a>
## API-PLAYLIST-001 · 收藏歌单 / 新建歌单（需登录）

| 属性 | 值 |
|---|---|
| 模块 | <code>playlist_add.js</code> |
| Node 包装路由 | <code>/playlist/add</code> |
| 认证 | <code>required</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>write-or-sensitive</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>common-ssa</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/playlist_add.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/cloudlist.service/v5/add_list</code> | <code>POST</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>from_shupinmv</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>is_pri</code> | <code>0 &#124; 1</code> | 否/未知 | <code>body</code> | <code>0</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>list_create_gid</code> | <code>string</code> | 否/未知 | <code>body</code> | <code>''</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>list_create_listid</code> | <code>string</code> | 是 | <code>body</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>list_create_userid</code> | <code>string</code> | 是 | <code>body</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>name</code> | <code>string</code> | 是 | <code>body</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>source</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>1</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>token</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>total_ver</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>type</code> | <code>PlaylistAddType</code> | 否/未知 | <code>body</code> | <code>0</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>userid</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

| Body 路径 | 证据 |
|---|---|
| <code>status</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>data.info.listid</code> | <code>CONSUMER_CONFIRMED</code> |

这里只列出源码或固定 PC 消费端能够证明的字段；未列出的字段不代表不存在。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>playlistAdd</code> |
| Request DTO | <code>ApiPlaylistAddRequest</code> |
| Response DTO | <code>NetworkApiPlaylistAddResponse</code>；含 UNKNOWN 时先使用宽容中间结构 |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiPlaylistAddResponse |

<a id="api-playlist-002"></a>
## API-PLAYLIST-002 · 取消收藏歌单 / 删除歌单（需登录）

| 属性 | 值 |
|---|---|
| 模块 | <code>playlist_del.js</code> |
| Node 包装路由 | <code>/playlist/del</code> |
| 认证 | <code>required</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>write-or-sensitive</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | <code>SOURCE_CONFIRMED</code> |
| Cookie 回写 | 未发现 |
| 风控 | <code>common-ssa</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/playlist_del.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/v2/delete_list</code> | <code>POST</code> | <code>cloudlist.service.kugou.com</code> | <code>android</code> | <code>arraybuffer</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>appid</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>clienttime</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>clientver</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>key</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>last_area</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>last_time</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>listid</code> | <code>string</code> | 是 | <code>module</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>p</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>token</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>userid</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>x-router</code> | <code>unknown</code> | 否/未知 | <code>header</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>playlistDel</code> |
| Request DTO | <code>ApiPlaylistDelRequest</code> |
| Response DTO | <code>NetworkApiPlaylistDelResponse</code>；含 UNKNOWN 时先使用宽容中间结构 |
| 传输实现 | <code>OkHttp Call.Factory</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code>, <code>ApiResponseDecoder</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiPlaylistDelResponse |

<a id="api-playlist-003"></a>
## API-PLAYLIST-003 · 获取歌单详情

| 属性 | 值 |
|---|---|
| 模块 | <code>playlist_detail.js</code> |
| Node 包装路由 | <code>/playlist/detail</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>common-ssa</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/playlist_detail.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/v3/get_list_info</code> | <code>POST</code> | <code>pubsongs.kugou.com</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>data</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>ids</code> | <code>string</code> | 是 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>token</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>userid</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>x-router</code> | <code>unknown</code> | 否/未知 | <code>header</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>playlistDetail</code> |
| Request DTO | <code>ApiPlaylistDetailRequest</code> |
| Response DTO | <code>NetworkApiPlaylistDetailResponse</code>；含 UNKNOWN 时先使用宽容中间结构 |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiPlaylistDetailResponse |

<a id="api-playlist-004"></a>
## API-PLAYLIST-004 · 获取音效歌单

| 属性 | 值 |
|---|---|
| 模块 | <code>playlist_effect.js</code> |
| Node 包装路由 | <code>/playlist/effect</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>common-ssa</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/playlist_effect.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/pubsongs/v1/get_sound_effect_list</code> | <code>POST</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>page</code> | <code>number</code> | 否/未知 | <code>body</code> | <code>1</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>pagesize</code> | <code>number</code> | 否/未知 | <code>body</code> | <code>30</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>playlistEffect</code> |
| Request DTO | <code>ApiPlaylistEffectRequest</code> |
| Response DTO | <code>NetworkApiPlaylistEffectResponse</code>；含 UNKNOWN 时先使用宽容中间结构 |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiPlaylistEffectResponse |

<a id="api-playlist-005"></a>
## API-PLAYLIST-005 · 获取相似歌单

| 属性 | 值 |
|---|---|
| 模块 | <code>playlist_similar.js</code> |
| Node 包装路由 | <code>/playlist/similar</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>common-ssa</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/playlist_similar.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/pubsongs/v1/kmr_get_similar_lists</code> | <code>POST</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>appid</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>clienttime</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>clientver</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>ids</code> | <code>string</code> | 是 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>key</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>need_songs</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>show_list</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>ugc</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>userid</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>playlistSimilar</code> |
| Request DTO | <code>ApiPlaylistSimilarRequest</code> |
| Response DTO | <code>NetworkApiPlaylistSimilarResponse</code>；含 UNKNOWN 时先使用宽容中间结构 |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiPlaylistSimilarResponse |

<a id="api-playlist-006"></a>
## API-PLAYLIST-006 · 获取歌单分类（含 category 信息）

| 属性 | 值 |
|---|---|
| 模块 | <code>playlist_tags.js</code> |
| Node 包装路由 | <code>/playlist/tags</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>common-ssa</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/playlist_tags.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/pubsongs/v1/get_tags_by_type</code> | <code>POST</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>source</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>tag_id</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>tag_type</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
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
| DataSource 操作 | <code>playlistTags</code> |
| Request DTO | <code>ApiPlaylistTagsRequest</code> |
| Response DTO | <code>NetworkApiPlaylistTagsResponse</code>；含 UNKNOWN 时先使用宽容中间结构 |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiPlaylistTagsResponse |

<a id="api-playlist-007"></a>
## API-PLAYLIST-007 · 获取歌单所有歌曲

| 属性 | 值 |
|---|---|
| 模块 | <code>playlist_track_all.js</code> |
| Node 包装路由 | <code>/playlist/track/all</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>common-ssa</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/playlist_track_all.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/pubsongs/v2/get_other_list_file_nofilt</code> | <code>GET</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>area_code</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>extend_fields</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>id</code> | <code>string</code> | 是 | <code>query</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>page</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>1</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>pagesize</code> | <code>number</code> | 否/未知 | <code>module</code> | <code>30</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>personal_switch</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>plat</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>type</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

| Body 路径 | 证据 |
|---|---|
| <code>status</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>data.songs</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>data.list_info.count</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>data.list_info</code> | <code>CONSUMER_CONFIRMED</code> |

这里只列出源码或固定 PC 消费端能够证明的字段；未列出的字段不代表不存在。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>playlistTrackAll</code> |
| Request DTO | <code>ApiPlaylistTrackAllRequest</code> |
| Response DTO | <code>NetworkApiPlaylistTrackAllResponse</code>；含 UNKNOWN 时先使用宽容中间结构 |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiPlaylistTrackAllResponse |

<a id="api-playlist-008"></a>
## API-PLAYLIST-008 · 获取歌单所有歌曲（新版，仅支持用户创建及收藏的歌单）

| 属性 | 值 |
|---|---|
| 模块 | <code>playlist_track_all_new.js</code> |
| Node 包装路由 | <code>/playlist/track/all/new</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>common-ssa</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/playlist_track_all_new.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/v4/get_list_all_file</code> | <code>POST</code> | <code>cloudlist.service.kugou.com</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>allplatform</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>area_code</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>listid</code> | <code>string</code> | 是 | <code>body</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>page</code> | <code>number</code> | 否/未知 | <code>body</code> | <code>1</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>pagesize</code> | <code>number</code> | 否/未知 | <code>body</code> | <code>30</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>show_cover</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>show_relate_goods</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>token</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>&lt;redacted&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>type</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>userid</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>&lt;redacted&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>x-router</code> | <code>unknown</code> | 否/未知 | <code>header</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>playlistTrackAllNew</code> |
| Request DTO | <code>ApiPlaylistTrackAllNewRequest</code> |
| Response DTO | <code>NetworkApiPlaylistTrackAllNewResponse</code>；含 UNKNOWN 时先使用宽容中间结构 |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiPlaylistTrackAllNewResponse |

<a id="api-playlist-009"></a>
## API-PLAYLIST-009 · 对歌单添加歌曲（需登录）

| 属性 | 值 |
|---|---|
| 模块 | <code>playlist_tracks_add.js</code> |
| Node 包装路由 | <code>/playlist/tracks/add</code> |
| 认证 | <code>required</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>write-or-sensitive</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>common-ssa</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/playlist_tracks_add.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/cloudlist.service/v6/add_song</code> | <code>POST</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>data</code> | <code>string</code> | 是 | <code>body</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>last_area</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>last_time</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>list_ver</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>listid</code> | <code>string</code> | 是 | <code>body</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>scene</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>slow_upload</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>token</code> | <code>unknown</code> | 否/未知 | <code>query</code>, <code>body</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>type</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>userid</code> | <code>unknown</code> | 否/未知 | <code>query</code>, <code>body</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>playlistTracksAdd</code> |
| Request DTO | <code>ApiPlaylistTracksAddRequest</code> |
| Response DTO | <code>NetworkApiPlaylistTracksAddResponse</code>；含 UNKNOWN 时先使用宽容中间结构 |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiPlaylistTracksAddResponse |

<a id="api-playlist-010"></a>
## API-PLAYLIST-010 · 对歌单删除歌曲（需登录）

| 属性 | 值 |
|---|---|
| 模块 | <code>playlist_tracks_del.js</code> |
| Node 包装路由 | <code>/playlist/tracks/del</code> |
| 认证 | <code>required</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>write-or-sensitive</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>common-ssa</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/playlist_tracks_del.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/v4/delete_songs</code> | <code>POST</code> | <code>cloudlist.service.kugou.com</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>data</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>fileids</code> | <code>string</code> | 是 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>list_ver</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>listid</code> | <code>string</code> | 是 | <code>body</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>token</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>type</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>userid</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>x-router</code> | <code>unknown</code> | 否/未知 | <code>header</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>playlistTracksDel</code> |
| Request DTO | <code>ApiPlaylistTracksDelRequest</code> |
| Response DTO | <code>NetworkApiPlaylistTracksDelResponse</code>；含 UNKNOWN 时先使用宽容中间结构 |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiPlaylistTracksDelResponse |
