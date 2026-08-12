# 评论 API

本页记录 7 个 Lite 包装模块。所有结论均来自固定静态源码；“上游请求”才是 Android 直连需要实现的协议。

[返回 API 文档首页](../README.md)

<a id="api-comment-001"></a>
## API-COMMENT-001 · 获取专辑评论（无需登录）

| 属性 | 值 |
|---|---|
| 模块 | <code>comment_album.js</code> |
| Node 包装路由 | <code>/comment/album</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>excluded</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/comment_album.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/m.comment.service/v1/cmtlist</code> | <code>POST</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>code</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>id</code> | <code>string</code> | 是 | <code>query</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>need_show_image</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>page</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>1</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>pagesize</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>30</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>show_classify</code> | <code>0 &#124; 1</code> | 否/未知 | <code>query</code> | <code>1</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>show_hotword_list</code> | <code>0 &#124; 1</code> | 否/未知 | <code>query</code> | <code>1</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>commentAlbum</code> |
| Request DTO | <code>ApiCommentAlbumRequest</code> |
| Response DTO | <code>NetworkApiCommentAlbumResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiCommentAlbumResponse |

<a id="api-comment-002"></a>
## API-COMMENT-002 · 获取歌曲评论数（无需登录）

| 属性 | 值 |
|---|---|
| 模块 | <code>comment_count.js</code> |
| Node 包装路由 | <code>/comment/count</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>excluded</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/comment_count.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/index.php</code> | <code>GET</code> | <code>sum.comment.service.kugou.com</code> | <code>web</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>code</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>hash</code> | <code>string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>r</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>special_id</code> | <code>string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>x-router</code> | <code>unknown</code> | 否/未知 | <code>header</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>commentCount</code> |
| Request DTO | <code>ApiCommentCountRequest</code> |
| Response DTO | <code>NetworkApiCommentCountResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiCommentCountResponse |

<a id="api-comment-003"></a>
## API-COMMENT-003 · 获取楼层评论

| 属性 | 值 |
|---|---|
| 模块 | <code>comment_floor.js</code> |
| Node 包装路由 | <code>/comment/floor</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>excluded</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>none-or-dynamic</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/comment_floor.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>dynamic</code> | <code>dynamic</code> | <code>dynamic</code> | <code>DYNAMIC</code> | <code>-</code> | <code>unknown</code> | <code>unknown</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>code</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>mixsongid</code> | <code>string</code> | 是 | <code>module</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>page</code> | <code>number</code> | 否/未知 | <code>module</code> | <code>1</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>pagesize</code> | <code>number</code> | 否/未知 | <code>module</code> | <code>30</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>resource_type</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>resourceType</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>''</code> | <code>SOURCE_CONFIRMED</code> |
| <code>show_classify</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>1</code> | <code>SOURCE_CONFIRMED</code> |
| <code>show_hotword_list</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>1</code> | <code>SOURCE_CONFIRMED</code> |
| <code>special_id</code> | <code>string</code> | 是 | <code>module</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>tid</code> | <code>string</code> | 是 | <code>module</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>commentFloor</code> |
| Request DTO | <code>ApiCommentFloorRequest</code> |
| Response DTO | <code>NetworkApiCommentFloorResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>OkHttp Call.Factory</code> |
| 协议组件 | <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiCommentFloorResponse |

<a id="api-comment-004"></a>
## API-COMMENT-004 · 获取歌曲所有评论（无需登录）

| 属性 | 值 |
|---|---|
| 模块 | <code>comment_music.js</code> |
| Node 包装路由 | <code>/comment/music</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>excluded</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/comment_music.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/mcomment/v1/cmtlist</code> | <code>POST</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>code</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>extdata</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>mixsongid</code> | <code>string</code> | 是 | <code>query</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>need_show_image</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>page</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>1</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>pagesize</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>30</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>show_classify</code> | <code>0 &#124; 1</code> | 否/未知 | <code>query</code> | <code>1</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>show_hotword_list</code> | <code>0 &#124; 1</code> | 否/未知 | <code>query</code> | <code>1</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>commentMusic</code> |
| Request DTO | <code>ApiCommentMusicRequest</code> |
| Response DTO | <code>NetworkApiCommentMusicResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiCommentMusicResponse |

<a id="api-comment-005"></a>
## API-COMMENT-005 · 获取歌曲分类评论（无需登录）

| 属性 | 值 |
|---|---|
| 模块 | <code>comment_music_classify.js</code> |
| Node 包装路由 | <code>/comment/music/classify</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>excluded</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/comment_music_classify.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/mcomment/v1/cmt_classify_list</code> | <code>POST</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>code</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>extdata</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>mixsongid</code> | <code>string</code> | 是 | <code>query</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>need_show_image</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>page</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>1</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>pagesize</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>30</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>sort</code> | <code>CommentSort</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>type_id</code> | <code>string</code> | 是 | <code>query</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>commentMusicClassify</code> |
| Request DTO | <code>ApiCommentMusicClassifyRequest</code> |
| Response DTO | <code>NetworkApiCommentMusicClassifyResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiCommentMusicClassifyResponse |

<a id="api-comment-006"></a>
## API-COMMENT-006 · 获取歌曲热词评论（无需登录）

| 属性 | 值 |
|---|---|
| 模块 | <code>comment_music_hotword.js</code> |
| Node 包装路由 | <code>/comment/music/hotword</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>excluded</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/comment_music_hotword.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/mcomment/v1/get_hot_word</code> | <code>POST</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>code</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>extdata</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>hot_word</code> | <code>string</code> | 是 | <code>query</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>mixsongid</code> | <code>string</code> | 是 | <code>query</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>need_show_image</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>page</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>1</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>pagesize</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>30</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>commentMusicHotword</code> |
| Request DTO | <code>ApiCommentMusicHotwordRequest</code> |
| Response DTO | <code>NetworkApiCommentMusicHotwordResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiCommentMusicHotwordResponse |

<a id="api-comment-007"></a>
## API-COMMENT-007 · 获取歌单评论（无需登录）

| 属性 | 值 |
|---|---|
| 模块 | <code>comment_playlist.js</code> |
| Node 包装路由 | <code>/comment/playlist</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>excluded</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/comment_playlist.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/m.comment.service/v1/cmtlist</code> | <code>POST</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>code</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>content_type</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>id</code> | <code>string</code> | 是 | <code>query</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>need_show_image</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>page</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>1</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>pagesize</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>30</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>show_classify</code> | <code>0 &#124; 1</code> | 否/未知 | <code>query</code> | <code>1</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>show_hotword_list</code> | <code>0 &#124; 1</code> | 否/未知 | <code>query</code> | <code>1</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>tag</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>commentPlaylist</code> |
| Request DTO | <code>ApiCommentPlaylistRequest</code> |
| Response DTO | <code>NetworkApiCommentPlaylistResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiCommentPlaylistResponse |
