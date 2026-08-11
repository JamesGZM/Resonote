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

## 首条纵切片

按搜索 → 播放地址 → 歌词 → Media3 播放实施。开始 Kotlin 代码前，先为相应端点补齐签名 golden fixture、脱敏响应 fixture 或明确的宽容 DTO 决策。

## 禁止依赖

- UI/Feature 不直接依赖 Retrofit、OkHttp 或 API DTO。
- Provider 语义只存在于 `core:network` 的内部协议 package，不向 Feature 或领域模型暴露。
- 领域模型不保留上游字段命名和传输层可空性。
- 不把 PC → Node 的 Authorization 桥接协议误作上游协议。
