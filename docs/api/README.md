# Lite 静态 API 契约

> 状态：静态证据基线，不代表上游接口当前可用或获得服务授权。

## 基线

- PC 消费端：`MoeKoeMusic@52c9833afe2e7fedcba8d5b23ff8d1f9731af73a`
- API 协议源：`MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb`
- 平台：概念版 `lite`（`appid=3116`、`clientver=11440`）
- 模块：164
- 验证：仅静态分析，无外部网络请求

## 阅读顺序

1. [公共协议](PROTOCOL.md)
2. [机器可读目录](catalog.yaml)
3. [Android/NIA 映射](ANDROID_MAPPING.md)
4. [验证与缺口](VERIFICATION.md)
5. [接口领域索引](#接口领域)

Node 包装路由只描述 PC 调用的本地 Express 接口；每个接口章节中的“上游请求”才是 Android 直连契约。字段证据等级为 `SOURCE_CONFIRMED`、`CONSUMER_CONFIRMED`、`DECLARED`、`FIXTURE_CONFIRMED`、`INFERRED`、`UNKNOWN`。

## 接口领域

- [AI 推荐](endpoints/ai.md)：1
- [专辑](endpoints/album.md)：4
- [歌手](endpoints/artist.md)：9
- [刷刷](endpoints/brush.md)：1
- [云盘](endpoints/cloud.md)：3
- [评论](endpoints/comment.md)：7
- [设备与验证](endpoints/device.md)：1
- [发现与推荐](endpoints/discover.md)：17
- [收藏统计](endpoints/favorite.md)：1
- [电台](endpoints/fm.md)：4
- [图片](endpoints/images.md)：2
- [IP 内容](endpoints/ip.md)：5
- [登录](endpoints/login.md)：15
- [长音频](endpoints/longaudio.md)：6
- [歌词](endpoints/lyrics.md)：1
- [歌单](endpoints/playlist.md)：10
- [排行](endpoints/ranking.md)：5
- [听歌识曲](endpoints/recognition.md)：1
- [场景音乐](endpoints/scene.md)：8
- [搜索](endpoints/search.md)：7
- [曲谱](endpoints/sheet.md)：6
- [歌曲](endpoints/song.md)：12
- [主题内容](endpoints/theme.md)：4
- [用户](endpoints/user.md)：13
- [视频](endpoints/video.md)：3
- [概念版专区](endpoints/youth.md)：16
- [其他](endpoints/misc.md)：2

## 完整性摘要

- 全量模块：164/164
- 固定 PC 消费端直接使用：46
- 无字段级响应证据：118
- 未映射的固定 PC 请求路由：0

完整统计和限制见 [VERIFICATION](VERIFICATION.md)。

## 重新生成与校验

在 Resonote 根目录执行：

```shell
node docs/api/tools/generate-docs.mjs
node docs/api/tools/validate-docs.mjs
```

工具只读取固定 Git 对象；如 MoeKoeMusic 不在默认相邻目录，可通过 `MOEKOE_ROOT` 指向仓库。生成器会替换本目录中的领域文档、Schema 和 Fixture 索引。
