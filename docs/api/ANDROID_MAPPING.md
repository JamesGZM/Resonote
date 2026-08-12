# Android / NIA 映射

## 模块边界

`core:network` 按 NIA 方式统一拥有共享 OkHttp、Retrofit、序列化、Lite 签名、设备、会话、Cookie、加密、Network DTO、解码与风控协议；`core:data` 拥有 Repository、缓存与领域映射；Feature/ViewModel 只依赖 Repository。

## NIA 对应方式

- 以 `ApiNetworkDataSource` 暴露远端能力，具体 Retrofit/OkHttp 类保持 internal。
- 每个接口章节给出稳定操作名与静态 DTO 命名候选；候选名称不表示对应类已经实现，已迁移状态以本页纵切片记录和代码为准。
- 固定 API 包的 `ApiResponse<T = any>` 明确定义了泛型响应模式；默认 `any` 只是 TypeScript 对尚未声明端点 Body 类型的退路。Android 的 Retrofit Service 直接返回 internal `ApiResponse<具体 Data DTO>`，对应实测服务端 JSON 的 `status/error_code/data` 信封；没有该信封的播放地址使用独立 DTO。HTTP 状态由 Retrofit 异常映射，`ssa-code` Header 由受限响应拦截器归一化，Cookie 由 Session/特殊协议在内部处理，不向 Service 返回类型或 DataSource 暴露 `retrofit2.Response`。各端点 Data DTO 仍需结合 Mobile 消费模型与字段读取、PC 实际字段访问和脱敏实测来收敛。
- Retrofit converter 直接把标准 HTTP/JSON 响应反序列化为 internal `@Serializable` wire DTO；`ignoreUnknownKeys` 只用于兼容服务端新增字段，已知的字符串/数字变体由字段 serializer 显式处理。DataSource 校验必要字段后映射 Network model，wire DTO 与 Network model 均不得进入 Compose 或公共领域模型。`JsonObject` 只保留在加密、二进制或确有多形结构的特殊协议边界。
- Repository 使用 fake DataSource 测试，不以脆弱的调用顺序 mock 为主。
- 标准 HTTP/JSON 业务接口由内部 `MusicApi` 以 Retrofit 声明；方法级 `@ApiRequestPolicy` 声明静态策略；`ApiDefaultsInterceptor` 读取已初始化的 Session 内存快照并注入公共参数/Header/Cookie，`ApiSigningInterceptor` 通过 Retrofit `Invocation` Tag 对最终 Query 与序列化 Body 字节签名。
- `Call.Factory` 只作为最底层传输抽象，并由 `ProtocolTransport` 用于设备注册、加密登录和风控验证等二进制、加密或多阶段特殊协议；普通业务接口不得用它重新实现一套 Retrofit。Retrofit 按固定 NIA 基线通过 `dagger.Lazy<Call.Factory>` 延迟取得共享 Client。
- 设备注册通过可注入 Provider 按 Mobile 合同读取总内存、品牌、Build ID、型号和厂商，缺失时使用 fallback，存储字段保留 Mobile 固定兼容值；携带 `ssa-code` 的响应按 2 MiB 上限有界读取并在拒绝路径关闭 Body。

## 通用风控

`core:network` 将 `ssa-code` Header 归一化进类型化 `ApiResponse<T>`，仅在 `error_code=20028` 时上抛内部 Challenge。`core:data` 将其登记为不透明 `RiskChallengeHandle`，并通过 `RiskVerificationRepository` 暴露验证方式查询和证明提交；Feature/ViewModel 不依赖 Network 类型。验证成功后的单次显式重试仍由原发起流程持有，Interceptor、Authenticator、Network DataSource 和特殊协议传输均不得自动重放。验证接口必须旁路 Challenge 检测以避免递归。

## 首页首批纵切片

- `ApiNetworkDataSource` 暴露每日推荐、`top_card`、推荐歌单、新歌速递和歌曲 URL 五个窄操作；共同复用设备注册、Session、签名、类型化风控检测和 Retrofit 调用链。
- `HomeRepository` 并发刷新三个首页区块，每日推荐在每次成功请求后重抽 6 首；单区失败保留旧快照，旧代际结果不得覆盖新请求。
- `loadRadio(mode)` 按需加载 `card_id=1/2/3/4/6`，默认私人好歌为 1。
- `SongPlaybackRepository` 只返回服务原生 HTTPS 主/备用地址、时长和扩展名；仅返回 HTTP 时报告 `InsecureMediaUrl` 协议错误，其他非空畸形地址报告 `MalformedResponse`，不通过改写 scheme 伪造安全地址。匿名 VIP 候选实测返回的 `error_code=35104` 与无 URL 的 VIP 响应统一映射为 `PlaybackUnavailableReason.Vip`；其他未知业务码仍保持服务拒绝，从而类型化区分版权、VIP、网络、协议与风控失败。
- 本批只落到 `core:network`、`core:data`、`core:model`，不包含 Compose、导航、Media3、Queue 或 Mini Player。

这里的“首批”只覆盖首页首屏内容请求，不等于所有首页入口的目标页面已经可用。Feature 模块的 `api` 是跨功能导航/调用合同，不承载网络接口；首页作为 Tabs Shell 根页面当前使用单一 `:feature:home`，不建立空的 `:feature:home:api`。

## 首页入口可达闭环

- 排行榜快捷入口本身不请求网络，进入发现的榜单子页面后由发现领域加载 `API-RANKING-003`（榜单列表），进入具体榜单后使用 `API-RANKING-001`（榜单歌曲）。两者不得加入首页下拉刷新的并发组。
- 精选歌单快捷入口进入发现的推荐歌单分类，复用已经实现的 `API-DISCOVER-012`，不复制 PC 固定个人歌单 ID，也不增加一个首页专属接口。
- 首页 6 个推荐歌单和发现歌单共用详情目的地；点击后使用 `API-PLAYLIST-007` 分页读取歌单信息和歌曲。
- `API-RANKING-003`、`API-RANKING-001`、`API-PLAYLIST-007` 已由共享 Network DataSource、`RankingRepository` 与 `PlaylistRepository` 实现，并保持类型化错误、取消传播和 Mobile 分页语义：榜单按可消费歌曲是否填满当前页判断，歌单将非正总数视为未知且以原始页大小兜底；它们不扩充 `HomeRepository.refresh()` 的职责。
- 推荐、榜单和歌单歌曲的音质同时读取显式 HQ/SQ Hash 与 `relate_goods` 可用档位；320K 映射为 `HighQuality`，不冒充 `HighResolution`。缺失歌手在领域层保留为 `null`，由 UI 本地化兜底；仅歌单协议需要的 `fileid` 留在 Network DTO，不进入 `OnlineSong`。

## 禁止依赖

- UI/Feature 不直接依赖 Retrofit、OkHttp 或 API DTO。
- Provider 语义只存在于 `core:network` 的内部协议 package，不向 Feature 或领域模型暴露。
- 领域模型不保留上游字段命名和传输层可空性。
- 不把 PC → Node 的 Authorization 桥接协议误作上游协议。
