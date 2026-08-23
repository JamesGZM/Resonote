# Resonote 当前网络能力

本文只描述当前 App 通过 `core:network` DataSource 暴露、并由 `core:data` Repository 消费的能力。它不是上游 API 全集，也不为操作分配人工编号。协议公共行为见 [PROTOCOL](PROTOCOL.md)，证据与运行状态见 [VERIFICATION](VERIFICATION.md)。

## 能力清单

默认 Origin 为 `https://gateway.kugou.com`。表中的响应是 Resonote 实际消费的 Network 模型；内部 DTO 和未知字段不构成公共合同。

### Home 与内容浏览

| DataSource / Repository 操作 | 实现与 Method、Host/Path | Session | 实际消费模型 |
|---|---|---|---|
| `dailyRecommendations` | Retrofit `POST /everyday_song_recommend` | 可选 | `List<NetworkSong>` |
| `newSongs` | Retrofit `POST /musicadservice/container/v1/newsong_publish` | 可选 | `List<NetworkSong>` |
| `radioRecommendations` | Retrofit `POST /singlecardrec.service/v1/single_card_recommend` | 可选 | `List<NetworkSong>` |
| `recommendedPlaylists`, `categoryPlaylists` | Retrofit `POST /v2/special_recommend` | 可选 | `List<NetworkPlaylistSummary>` |
| `banners`, `playlistCategories` | Retrofit `POST /ads.gateway/v3/listen_banner`, `/pubsongs/v1/get_tags_by_type` | 可选 | `NetworkBanner`, `NetworkPlaylistCategory` 列表 |
| `newAlbums`, `albumSongs` | Retrofit `POST /musicadservice/v1/mobile_newalbum_sp`, `/v1/album_audio/lite` | 可选 | `NetworkAlbum`, `NetworkAlbumSongPage` |
| `artistDetail`, `artistSongs` | Retrofit `POST /kmr/v3/author` 与动态作者歌曲 URL | 可选 | `NetworkArtistInfo`, `NetworkSongPage` |
| `rankings`, `rankingSongs` | Retrofit `GET /ocean/v6/rank/list`, `POST /openapi/kmr/v2/rank/audio` | 可选 | `NetworkRanking`, `NetworkSongPage` |
| `playlistSongs` | Retrofit `GET /pubsongs/v2/get_other_list_file_nofilt` | 可选 | `NetworkPlaylistPage` |

### Search

| DataSource / Repository 操作 | 实现与 Method、Host/Path | 认证策略 | 实际消费模型 |
|---|---|---|---|
| `searchSongs` | Retrofit `GET /v3/search/song` | 请求显式允许将业务码 `152` 识别为认证失败 | `NetworkSearchPage` |
| `searchPlaylists`, `searchAlbums`, `searchArtists`, `searchMvs` | Retrofit 动态 Search URL | 同上 | `NetworkSearchResultPage<T>` |
| `searchComplex` | Retrofit `GET https://complexsearch.kugou.com/...` | 普通可选 Session | `NetworkComplexSearch` |
| `hotSearchKeywords` | Retrofit `GET /api/v3/search/hot_tab` | 普通可选 Session | `List<NetworkSearchKeyword>` |
| `searchSuggestions` | Retrofit `GET /v2/getSearchTip` | 普通可选 Session | `List<String>` |

只有声明 Search 认证策略的请求会把 `152` 分类为登录需要或 Session 过期；其他请求收到相同业务码时仍按普通业务失败处理。

### Playback、歌词、视频与识曲

| DataSource / Repository 操作 | 实现与 Method、Host/Path | Session | 实际消费模型 |
|---|---|---|---|
| `resolveSongSource` | Retrofit `POST /v2/get_res_privilege/lite` 后 `GET /v5/url` | 可选；权限结果参与候选选择 | `NetworkSongSource` |
| `resolveCloudSongSource` | Retrofit `GET https://gateway.kugou.com/bsstrackercdngz/v2/query_musicclound_url` | 必需 | `NetworkSongSource` |
| `searchLyric`, `downloadLyric` | Retrofit 动态 `lyrics.kugou.com` URL；KRC 解码在 Network 内部 | 不传播 Session | `NetworkLyricCandidate`, 解码文本 |
| `resolveVideoUrl` | Retrofit `GET /v2/interface/index` | 可选 | URL 字符串 |
| `recognizeAudio` | Retrofit 二进制 `POST /fingerprint.service/v1/music_trackid_mulit` | 可选 | `List<NetworkRecognitionMatch>` |

### Account、Library、Cloud 与 VIP

| DataSource / Repository 操作 | 实现与 Method、Host/Path | Session | 实际消费模型 |
|---|---|---|---|
| `sendMobileCode`, `loginWithMobileCode`, `loginWithPassword` | 特殊协议 `POST` 到 mobile-code、mobile-login 与 gateway 登录 Origin | 登录前设备身份；成功后原子提交 Session | 登录结果模型 |
| `createQrLoginKey`, `checkQrLogin` | Retrofit 动态 QR Login HTTPS URL，Web 签名 | 登录前/轮询后提交 Session | `NetworkQrLoginStatus` |
| `userDetail`, `userVip` | Retrofit `POST /v3/get_my_info` 与 VIP 动态 URL | 必需 | `NetworkUserDetail`, `NetworkUserVip` |
| `userPlaylists`, `createPlaylist`, `favoritePlaylist`, `deletePlaylist`, `addPlaylistTracks`, `deletePlaylistTracks` | Retrofit `/v7/get_all_list`, `/cloudlist.service/v5/add_list`, `/cloudlist.service/v6/add_song`, `/v4/delete_songs`；Protocol `/v2/delete_list` | 必需 | `NetworkUserPlaylist` 或写入结果 |
| `cloudTracks` | 特殊协议 `POST https://mcloudservice.kugou.com/v1/get_list` | 必需 | `NetworkCloudPage` |
| `accountHistory`, `uploadAccountPlayback` | 特殊协议 `POST https://gateway.kugou.com/playhistory/v1/get_songs`, `POST https://gateway.kugou.com/playhistory/v1/upload_songs` | 必需 | 游标分页的最近播放 `NetworkListeningHistoryPage`；在线播放达记录门槛后上报 `album_audio_id/MixSongID` |
| `claimDailyVip`, `upgradeDailyVip` | Retrofit youth VIP 路径 | 必需 | `NetworkVipRewardResult` |

设备注册与风控验证是共享协议能力，不作为页面 Endpoint：设备注册使用 `POST https://userservice.kugou.com/risk/v2/r_register_dev`；风控上下文与提交分别使用 gateway verify-info 和 `https://verifyservice.kugou.com/v4/verify_user_info`。

## 维护规则

- 新能力只有在 DataSource 与 Repository 已存在真实消费者后才加入本文。
- Method、Path、Policy、DTO 映射以源码与测试为事实源；文档不生成生产注册表。
- 验证状态必须区分 Fixture、匿名 Canary、真实账号验证，不能用静态源码阅读替代运行证据。
- 固定参考为 `../MoeKoeMusic@a86cfefb3093` 与 `../MoeKoeMusic-Mobile@ab71195d4cf3`；未使用能力只在调研中记录。
