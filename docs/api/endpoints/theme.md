# 主题内容 API

本页记录 4 个 Lite 包装模块。所有结论均来自固定静态源码；“上游请求”才是 Android 直连需要实现的协议。

[返回 API 文档首页](../README.md)

<a id="api-theme-001"></a>
## API-THEME-001 · 获取主题音乐

| 属性 | 值 |
|---|---|
| 模块 | <code>theme_music.js</code> |
| Node 包装路由 | <code>/theme/music</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>common-ssa</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/theme_music.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/everydayrec.service/v1/mul_theme_category_recommend</code> | <code>POST</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>clienttime</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>ids</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>module_id</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>platform</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>userid</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>themeMusic</code> |
| Request DTO | <code>ApiThemeMusicRequest</code> |
| Response DTO | <code>NetworkApiThemeMusicResponse</code>；含 UNKNOWN 时先使用宽容中间结构 |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiThemeMusicResponse |

<a id="api-theme-002"></a>
## API-THEME-002 · 获取主题音乐详情

| 属性 | 值 |
|---|---|
| 模块 | <code>theme_music_detail.js</code> |
| Node 包装路由 | <code>/theme/music/detail</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>common-ssa</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/theme_music_detail.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/everydayrec.service/v1/theme_category_recommend</code> | <code>POST</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>clienttime</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>id</code> | <code>string</code> | 是 | <code>body</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>module_id</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>platform</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>show_theme_category_id</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>userid</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>themeMusicDetail</code> |
| Request DTO | <code>ApiThemeMusicDetailRequest</code> |
| Response DTO | <code>NetworkApiThemeMusicDetailResponse</code>；含 UNKNOWN 时先使用宽容中间结构 |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiThemeMusicDetailResponse |

<a id="api-theme-003"></a>
## API-THEME-003 · 获取主题歌单

| 属性 | 值 |
|---|---|
| 模块 | <code>theme_playlist.js</code> |
| Node 包装路由 | <code>/theme/playlist</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>common-ssa</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/theme_playlist.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/v2/getthemelist</code> | <code>POST</code> | <code>everydayrec.service.kugou.com</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>area_code</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>clienttime</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>clientver</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>module_id</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>platform</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>userid</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>x-router</code> | <code>unknown</code> | 否/未知 | <code>header</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>themePlaylist</code> |
| Request DTO | <code>ApiThemePlaylistRequest</code> |
| Response DTO | <code>NetworkApiThemePlaylistResponse</code>；含 UNKNOWN 时先使用宽容中间结构 |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiThemePlaylistResponse |

<a id="api-theme-004"></a>
## API-THEME-004 · 获取主题歌单所有歌曲

| 属性 | 值 |
|---|---|
| 模块 | <code>theme_playlist_track.js</code> |
| Node 包装路由 | <code>/theme/playlist/track</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>common-ssa</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/theme_playlist_track.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/v2/gettheme_songidlist</code> | <code>POST</code> | <code>everydayrec.service.kugou.com</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>area_code</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>clienttime</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>clientver</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>module_id</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>platform</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>theme_id</code> | <code>string</code> | 是 | <code>body</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>userid</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>x-router</code> | <code>unknown</code> | 否/未知 | <code>header</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>themePlaylistTrack</code> |
| Request DTO | <code>ApiThemePlaylistTrackRequest</code> |
| Response DTO | <code>NetworkApiThemePlaylistTrackResponse</code>；含 UNKNOWN 时先使用宽容中间结构 |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiThemePlaylistTrackResponse |
