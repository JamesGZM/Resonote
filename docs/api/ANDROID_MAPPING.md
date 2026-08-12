# Android / NIA 映射

## 模块边界

`core:network` 按 NIA 方式统一拥有共享 OkHttp、Retrofit、序列化、Lite 签名、设备、会话、Cookie、加密、Network DTO、解码与通用风控协调；`core:data` 拥有 Repository、缓存与领域映射；Feature/ViewModel 只依赖 Repository。

## NIA 对应方式

- 以 `ApiNetworkDataSource` 暴露远端能力，具体 Retrofit/OkHttp 类保持 internal。
- 每个接口章节给出稳定操作名和 DTO 根类型建议。
- Network DTO 使用 kotlinx.serialization，默认忽略未知键；不得进入 Compose 或公共领域模型。
- Repository 使用 fake DataSource 测试，不以脆弱的调用顺序 mock 为主。
- Retrofit 只用于单阶段、稳定 Host、JSON 请求；动态路径、二进制、加密或多阶段流程使用共享 `Call.Factory`。

## 通用风控

`core:network` 从 Body 与 Header 统一识别 `20028`/`ssaCode` Challenge，通过不依赖 UI 的 `ApiRiskVerifier` 串行完成验证。普通请求验证成功后重新生成时间戳和签名并最多重试一次；验证接口必须旁路协调器，超时或断网不得触发重试。

## 首页首批纵切片

- `ApiNetworkDataSource` 暴露每日推荐、`top_card`、推荐歌单、新歌速递和歌曲 URL 五个窄操作；共同复用设备注册、Session、签名、风控和请求执行器。
- `HomeRepository` 并发刷新三个首页区块，每日推荐在每次成功请求后重抽 6 首；单区失败保留旧快照，旧代际结果不得覆盖新请求。
- `loadRadio(mode)` 按需加载 `card_id=1/2/3/4/6`，默认私人好歌为 1。
- `SongPlaybackRepository` 只返回首个 HTTPS 主/备用地址、时长和扩展名，并类型化区分版权、VIP、网络、协议与风控失败。
- 本批只落到 `core:network`、`core:data`、`core:model`，不包含 Compose、导航、Media3、Queue 或 Mini Player。

## 禁止依赖

- UI/Feature 不直接依赖 Retrofit、OkHttp 或 API DTO。
- Provider 语义只存在于 `core:network` 的内部协议 package，不向 Feature 或领域模型暴露。
- 领域模型不保留上游字段命名和传输层可空性。
- 不把 PC → Node 的 Authorization 桥接协议误作上游协议。
