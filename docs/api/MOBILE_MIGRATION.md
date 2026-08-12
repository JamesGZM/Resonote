# Mobile 实际 API 迁移总账

本页只统计 `MoeKoeMusic-Mobile/src` 非生成业务源码中的 `mobileApi.*` 调用。固定基线共 39 个模块；Resonote 当前实现 39 个，剩余 0 个。测试状态中的 `fixture` 表示使用脱敏响应、MockWebServer 或 Fake DataSource，不代表真实账号联调通过。

| Mobile 模块 | 文档 ID | Mobile 消费位置 | 认证 | 状态 | 后续批次 |
|---|---|---|---|---|---|
| `register_dev` | API-DEVICE-001 | `src/lib/kugou-api/index.ts` | 匿名 | 已实现/fixture | 基础设施 |
| `captcha_sent` | API-LOGIN-001 | `src/app/login.tsx` | 可选 | 已实现/fixture | 认证首期 |
| `login_cellphone` | API-LOGIN-004 | `src/app/login.tsx` | 可选 | 已实现/fixture | 认证首期 |
| `login` | API-LOGIN-003 | `src/app/login.tsx` | 可选 | 已实现/fixture | 认证首期 |
| `get_verify_info` | API-LOGIN-002 | `src/app/login.tsx` | 可选 | 已实现/fixture | 认证首期 |
| `verify_user_info` | API-LOGIN-015 | `src/app/login.tsx` | 可选 | 已实现/fixture | 认证首期 |
| `login_qr_key` | API-LOGIN-010 | `src/app/login.tsx` | 可选 | 已实现/fixture | 二维码登录 |
| `login_qr_check` | API-LOGIN-008 | `src/app/login.tsx` | 可选 | 已实现/fixture | 二维码登录 |
| `everyday_recommend` | API-DISCOVER-003 | `src/features/home/load-home-data.ts` | 可选 | 已实现/fixture | 首页 |
| `top_playlist` | API-DISCOVER-012 | 首页、发现 | 可选 | 已实现/fixture | 首页 |
| `rank_list` | API-RANKING-003 | 首页、发现 | 可选 | 已实现/fixture | 首页 |
| `rank_audio` | API-RANKING-001 | 首页、发现 | 可选 | 已实现/fixture | 首页 |
| `top_song` | API-DISCOVER-013 | 首页、发现 | 可选 | 已实现/fixture | 首页 |
| `playlist_track_all` | API-PLAYLIST-007 | 歌单、媒体库 | 可选 | 已实现/fixture | 首页 |
| `song_url` | API-SONG-011 | `src/features/player/song-url.ts` | 可选 | 已实现/fixture | 播放 |
| `search` | API-SEARCH-001 | `src/features/search/search-api.ts` | 可选 | 已实现/fixture | 搜索 |
| `yueku_banner` | API-DISCOVER-016 | `src/features/home/load-home-data.ts` | 可选 | 已实现/fixture | 公开内容 |
| `playlist_tags` | API-PLAYLIST-006 | `src/features/discover/discover-api.ts` | 可选 | 已实现/fixture | 公开内容 |
| `top_album` | API-DISCOVER-008 | `src/features/discover/discover-api.ts` | 可选 | 已实现/fixture | 公开内容 |
| `album_songs` | API-ALBUM-004 | `src/features/discover/discover-api.ts` | 可选 | 已实现/fixture | 公开内容 |
| `artist_detail` | API-ARTIST-003 | `src/features/artist/artist-api.ts` | 可选 | 已实现/fixture | 公开内容 |
| `artist_audios` | API-ARTIST-002 | `src/features/artist/artist-api.ts` | 可选 | 已实现/fixture | 公开内容 |
| `search_complex` | API-SEARCH-002 | `src/features/search/search-api.ts` | 可选 | 已实现/fixture | 公开内容 |
| `search_hot` | API-SEARCH-004 | `src/features/search/search-api.ts` | 可选 | 已实现/fixture | 公开内容 |
| `search_suggest` | API-SEARCH-007 | `src/features/search/search-api.ts` | 可选 | 已实现/fixture | 公开内容 |
| `search_lyric` | API-SEARCH-005 | 播放器歌词 | 可选 | 已实现/fixture | 公开内容 |
| `lyric` | API-LYRICS-001 | 播放器歌词 | 可选 | 已实现/fixture | 公开内容 |
| `video_url` | API-VIDEO-003 | `src/features/mv/mv-api.ts` | 匿名 | 已实现/fixture | 公开内容 |
| `audio_match` | API-RECOGNITION-001 | `src/features/recognize/recognize-api.ts` | 可选 | 已实现/fixture | 公开内容 |
| `user_detail` | API-USER-003 | `src/features/account/user-api.ts` | 必需 | 已实现/仅 Fake | 用户态 |
| `user_vip_detail` | API-USER-013 | `src/features/account/user-api.ts` | 必需 | 已实现/仅 Fake | 用户态 |
| `user_playlist` | API-USER-008 | `src/features/library/library-api.ts` | 必需 | 已实现/仅 Fake | 用户态 |
| `playlist_add` | API-PLAYLIST-001 | `src/features/library/library-api.ts` | 必需 | 已实现/仅 Fake | 用户态 |
| `playlist_tracks_add` | API-PLAYLIST-009 | `src/features/library/library-api.ts` | 必需 | 已实现/仅 Fake | 用户态 |
| `playlist_tracks_del` | API-PLAYLIST-010 | `src/features/library/library-api.ts` | 必需 | 已实现/仅 Fake | 用户态 |
| `user_cloud` | API-CLOUD-001 | `src/features/cloud/cloud-api.ts` | 必需 | 已实现/仅 Fake | 用户态 |
| `user_cloud_url` | API-CLOUD-003 | 播放器、云盘 | 必需 | 已实现/仅 Fake | 用户态 |
| `youth_day_vip` | API-YOUTH-008 | `src/features/account/vip-api.ts` | 必需 | 已实现/仅 Fake | 用户态 |
| `youth_day_vip_upgrade` | API-YOUTH-009 | `src/features/account/vip-api.ts` | 必需 | 已实现/仅 Fake | 用户态 |

## 测试与联调规则

- 请求构造以 Mobile 实际模块源码为第一证据，消费字段以对应 `src/features/**` 调用点为第一证据，并与本目录 API 文档交叉验证。
- 匿名读取接口可保留显式开关的 live canary；默认测试不得访问外网。
- 登录后接口使用已认证 Fake Session、Fake DataSource、MockWebServer 和脱敏 synthetic fixture，覆盖成功、服务拒绝、登录过期、风控、协议畸形与取消传播。
- 未经真实账号人工联调的接口必须保持“仅 Fake”标记，不得将 fixture 通过写成真实服务已验证。
- 文档校验脚本会从固定 `MoeKoeMusic-Mobile` 提交的 `src` 业务源码提取 `mobileApi.*` 调用，与本页 39 个模块逐项比较，并确认每个文档 ID 在 API catalog、Android 网络实现和测试追踪中都有对应证据；完整 catalog 的 164 个模块数不代表本期迁移数量。
- PC 与 Mobile 共用的请求层统一识别 `status=0`/非零 `error_code` 的失败外形，但业务含义按端点保留：识曲无匹配、播放版权、VIP 已领取和认证失效不得混为一类。首期认证注册表仍只包含已实证的 `API-SEARCH-001 + 152`。
- 云盘解密后的业务失败当前保持普通服务错误；PC 与 Mobile 均未提供云盘登录失效业务码证据，不推测、不登记。
- `includeDefaultParams=false` 与 Session 传播相互独立：歌词搜索不带默认 Query，但认证缓存存在时仍发送 Full Cookie。Mobile `search` 的所有类型变体共享 `API-SEARCH-001 + 152` 认证分类，并使用请求前 Session revision。

## 39 项协议测试追踪

下表中的测试均为离线测试；请求协议使用 MockWebServer，用户态业务使用 Fake Session/Fake DataSource 和 synthetic fixture。一个测试覆盖多个接口时，测试会分别断言各请求的路径、关键参数或消费字段。

| 文档 ID | 协议/业务测试方法 |
|---|---|
| API-DEVICE-001 | `missingDfidRegistersAnonymousDeviceBeforeSignedSearch`、`deviceRegistrationAcceptsDfidFromResponseCookie` |
| API-LOGIN-001 | `sendMobileCodeUsesIsolatedMidIdentityAndExactBody` |
| API-LOGIN-004 | `mobileLoginBuildsLiteBodyMergesCookiesAndAcceptsObjectSecret` |
| API-LOGIN-003 | `passwordLoginMatchesMobileContractAndCommitsDecodedCredentials` |
| API-LOGIN-002 | `methodForUsesGatewayContractAndDecodesSmsVerification` |
| API-LOGIN-015 | `submitUsesIsolatedRiskOriginAndBypassesRecursiveVerification` |
| API-LOGIN-010、API-LOGIN-008 | `qrLoginUsesWebSignatureAndBuildsAuthenticatedSession` |
| API-DISCOVER-003 | `dailyRecommendationsUsesMobileContractAndDecodesRequiredFields` |
| API-DISCOVER-012 | `recommendedPlaylistsUsesNestedMobileBody`、`categoryPlaylistsPassesMobileCategoryAndPaging` |
| API-RANKING-003 | `rankingsUseFixedQueryAndDecodeRequiredFields` |
| API-RANKING-001 | `rankingSongsUseFixedBodyHeaderAndDecodePage` |
| API-DISCOVER-013 | `newSongsUsesFixedUnclassifiedRankAndDecodesDeprecatedFallback` |
| API-PLAYLIST-007 | `playlistSongsUseOffsetAndDecodeInfoTracksAndFileId` |
| API-SONG-011 | `songSourceUsesRegisteredIdentitySongKeyAndHttpsBackup` |
| API-SEARCH-001 | `searchSongsDecodeIntoSharedNetworkSong`、`typedSearchUsesMobilePathsPagingAndConsumerFields`、`typedSearchAuthenticationCodeUsesTheSharedSearchGate` |
| API-DISCOVER-016、API-PLAYLIST-006 | `bannersAndPlaylistTagsMatchMobileContracts` |
| API-DISCOVER-008、API-ALBUM-004 | `albumsAndNestedAlbumSongsMapOnlyMobileConsumerFields` |
| API-ARTIST-003、API-ARTIST-002 | `artistDetailAndAudiosUseMobileHeadersSortAndDedicatedOrigin` |
| API-SEARCH-002、API-SEARCH-004、API-SEARCH-007 | `complexHotAndSuggestSearchMatchMobileConsumerShapes` |
| API-SEARCH-005、API-LYRICS-001 | `lyricSearchMatchesRuntimeLiteSignatureAndDownloadDecodesBase64Lrc`（断言无默认 Query 且保留认证 Cookie） |
| API-VIDEO-003 | `videoUrlUsesSongKeyAndRejectsCleartext` |
| API-RECOGNITION-001 | `recognitionPostsRawPcmAndSortsMatchesByConfidence`（断言源码实际使用的 `useid` Query 拼写） |
| API-USER-003 | `userDetailUsesAuthenticatedMobileContractAndMapsConsumerFields` |
| API-USER-013 | `userVipUsesDedicatedOriginAndMapsActiveSvip` |
| API-USER-008 | `userPlaylistsMatchesMobilePagingAndFiltersAlbumEntries` |
| API-PLAYLIST-001 | `createPlaylistMatchesMobileBodyAndReturnsListId` |
| API-PLAYLIST-009 | `addPlaylistTracksBuildsResourcesAndSanitizesSeparators` |
| API-PLAYLIST-010 | `deletePlaylistTracksUsesFileIdsAndCloudListRouter` |
| API-CLOUD-001 | `cloudTracksUsesEncryptedMobileContractAndMapsCloudPage` |
| API-CLOUD-003 | `cloudSongUrlUsesMobileSignatureAndRequiresHttps` |
| API-YOUTH-008、API-YOUTH-009 | `dailyVipEndpointsRequireAuthAndPreserveAlreadyDoneSemantics` |

## 实际业务调用变体

模块数量只用于核对依赖范围，不能代替业务能力覆盖。当前额外按 Mobile 调用参数核对以下复用模块：

- `search`：单曲使用 `/v3/search/song`；歌单、专辑、歌手和 MV 分别使用 `/v1/search/special|album|author|mv`，均保留关键词、页码、页大小和消费字段映射。
- `top_playlist`：首页推荐使用 `category_id=0`；发现页分类加载透传实际 `category_id`、页码和页大小。
- `playlist_track_all`：歌单详情与媒体库均使用全局歌单 ID 和偏移分页。
- `verify_user_info`：短信与腾讯验证共享端点，但分别构造 `code` 与 `verifycode` 证明字段。

## Android 分层

- `core:network` 对外提供按业务能力拆分的窄接口：Auth、Home、Catalog、Ranking、Playlist、Playback、UserProfile、Library、Cloud、Search、Lyrics、Video、Recognition、VIP；每个公开端口独立成文件，不保留 catch-all `NetworkDataSources.kt`。
- Home、Catalog、Ranking、Playlist、Search、Lyrics、Video、Recognition、UserProfile 与 Library 均使用独立生产实现；共享 wire-to-network 歌曲解码器单独复用，特殊协议原始响应模型位于 `protocol` 层。
- Retrofit 端点按 Content、Playback、Search、Lyrics、Video、Recognition、Account 协议面分文件声明，`MusicApi` 仅作聚合创建入口，继续共享单例 Retrofit/OkHttp。
- 设备注册、手机/密码登录、风控验证和云盘列表属于特殊加密协议，使用共享 `ProtocolTransport`/`Call.Factory`；其余本期接口使用 Retrofit。生成文档会按这份实现注册表校验传输类型，避免由上游模块外形误判。
- `core:data` Repository 按消费者拆为 Search、Lyrics、Video、Recognition、UserProfile、Library 等窄接口，只依赖对应 Network port，负责输入校验、Network → Domain 映射和可展示失败类型。
- App 入口、根认证导航、登录占位页面与 Tabs Shell 分文件组合；认证 Required/Expired 导航到持续占位页面，不使用 Snackbar 代替登录流程。
- Session 写入、失效清理、Store 外部变更和门禁确认使用同一 Mutex/revision 规则；公开认证 Flow 只读取事务完成后的 Store 最终值，未订阅 Flow 时旧请求同样不能清除新 Session。`Full` 原样传播持久化 Cookie；登录响应合并与失效清理由 Session 存储边界负责。登录占位文案使用 Android string resource。
- 设备注册、Session、签名、风控、加密和特殊 Origin 继续集中在 `protocol`/`risk`/`session`，不进入 Feature 或 Repository。
- 登录后能力的自动测试仍只使用 Fake Session、Fake DataSource、MockWebServer 和 synthetic fixture；真实账号联调状态不变。
