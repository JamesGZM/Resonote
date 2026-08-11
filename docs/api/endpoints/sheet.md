# 曲谱 API

本页记录 6 个 Lite 包装模块。所有结论均来自固定静态源码；“上游请求”才是 Android 直连需要实现的协议。

[返回 API 文档首页](../README.md)

<a id="api-sheet-001"></a>
## API-SHEET-001 · 获取曲谱合集 / 曲谱合集详情

| 属性 | 值 |
|---|---|
| 模块 | <code>sheet_collection.js</code> |
| Node 包装路由 | <code>/sheet/collection</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>handle-and-replay-once</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/sheet_collection.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/miniyueku/v1/opern_square/get_home_module_config</code> | <code>GET</code> | <code>-</code> | <code>web</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>collection_id</code> | <code>string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>page</code> | <code>number</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>pagesize</code> | <code>number</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>position</code> | <code>SheetCollectionPosition</code> | 否/未知 | <code>query</code> | <code>2</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>srcappid</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>sheetCollection</code> |
| Request DTO | <code>ApiSheetCollectionRequest</code> |
| Response DTO | <code>NetworkApiSheetCollectionResponse</code>；含 UNKNOWN 时先使用宽容中间结构 |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiSheetCollectionResponse |

<a id="api-sheet-002"></a>
## API-SHEET-002 · 获取曲谱详情（AI 曲谱为 XML 文件需自行解析）

| 属性 | 值 |
|---|---|
| 模块 | <code>sheet_detail.js</code> |
| Node 包装路由 | <code>/sheet/detail</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>handle-and-replay-once</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/sheet_detail.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/opern/v1/detail/info</code> | <code>GET</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>id</code> | <code>string</code> | 是 | <code>query</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>source</code> | <code>string</code> | 是 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>sheetDetail</code> |
| Request DTO | <code>ApiSheetDetailRequest</code> |
| Response DTO | <code>NetworkApiSheetDetailResponse</code>；含 UNKNOWN 时先使用宽容中间结构 |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiSheetDetailResponse |

<a id="api-sheet-003"></a>
## API-SHEET-003 · 获取曲谱推荐

| 属性 | 值 |
|---|---|
| 模块 | <code>sheet_explore.js</code> |
| Node 包装路由 | <code>/sheet/explore</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>handle-and-replay-once</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/sheet_explore.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/opern/v1/home/get_rec_opern</code> | <code>POST</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>exposure_mixids</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>instruments</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>1</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>level</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>0</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>page</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>1</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>pagesize</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>30</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>tagid</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>0</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>sheetExplore</code> |
| Request DTO | <code>ApiSheetExploreRequest</code> |
| Response DTO | <code>NetworkApiSheetExploreResponse</code>；含 UNKNOWN 时先使用宽容中间结构 |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiSheetExploreResponse |

<a id="api-sheet-004"></a>
## API-SHEET-004 · 获取曲谱排行榜

| 属性 | 值 |
|---|---|
| 模块 | <code>sheet_rank.js</code> |
| Node 包装路由 | <code>/sheet/rank</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>handle-and-replay-once</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/sheet_rank.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/opern/v1/home/get_rank_opern</code> | <code>POST</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>instruments</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>1</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>level</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>0</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>page</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>1</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>pagesize</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>30</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>tagid</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>0</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>sheetRank</code> |
| Request DTO | <code>ApiSheetRankRequest</code> |
| Response DTO | <code>NetworkApiSheetRankResponse</code>；含 UNKNOWN 时先使用宽容中间结构 |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiSheetRankResponse |

<a id="api-sheet-005"></a>
## API-SHEET-005 · 获取曲谱歌曲详情

| 属性 | 值 |
|---|---|
| 模块 | <code>sheet_song.js</code> |
| Node 包装路由 | <code>/sheet/song</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>handle-and-replay-once</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/sheet_song.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/opern/v1/detail/song_info</code> | <code>GET</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>album_audio_id</code> | <code>string</code> | 是 | <code>query</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>instruments</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>1</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>level</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>0</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>sheetSong</code> |
| Request DTO | <code>ApiSheetSongRequest</code> |
| Response DTO | <code>NetworkApiSheetSongResponse</code>；含 UNKNOWN 时先使用宽容中间结构 |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiSheetSongResponse |

<a id="api-sheet-006"></a>
## API-SHEET-006 · 获取曲谱标签

| 属性 | 值 |
|---|---|
| 模块 | <code>sheet_tags.js</code> |
| Node 包装路由 | <code>/sheet/tags</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>handle-and-replay-once</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/sheet_tags.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/opern/v1/home/get_tags</code> | <code>GET</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>sheetTags</code> |
| Request DTO | <code>ApiSheetTagsRequest</code> |
| Response DTO | <code>NetworkApiSheetTagsResponse</code>；含 UNKNOWN 时先使用宽容中间结构 |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiSheetTagsResponse |
