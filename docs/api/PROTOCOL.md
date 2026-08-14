# Network Protocol

## 边界

`core:network` 对外暴露业务语义化 DataSource。普通 HTTP/JSON 请求由私有 Retrofit Service 和 DTO 实现；加密、二进制或多阶段流程由内部 `ProtocolTransport` 使用共享 OkHttp `Call.Factory` 实现。Feature 不接触这些传输细节。

## 请求 Policy

`ApiRequestPolicy` 只表达会改变真实请求或响应分类的行为：

- `signatureMode`：Android、Web 或不签名。
- `sessionPropagation`：是否传播当前 Session。
- `includeDefaultParams`：是否注入 Lite 公共参数。
- `serviceAuthentication`：该调用允许识别的认证业务码策略。

特殊协议使用 `ApiEndpointSpec` 直接声明 Method、Origin、Path、签名、Session、默认参数以及可选认证业务码集合。两者都不包含人工文档 ID。诊断信息使用 HTTP Method、脱敏 Host/Path 和调用类型。

## 公共上下文

默认网关为 `https://gateway.kugou.com`，Lite 配置包含 `appid=3116`、`clientver=11440`、可注入时钟生成的秒级 `clienttime`，以及持久化设备身份。`x-router` 用于选择网关后端，不等价于 Retrofit Base URL。

Session 至少管理 token、user id 与 VIP 信息；设备上下文管理 dfid、GUID/MID 和平台信息。敏感值不得进入日志、文档或 Fixture。设备注册以 suspend single-flight 在业务请求前完成，不允许 Interceptor 发起嵌套请求。

## 签名与重试

签名针对最终 Query 与序列化 Body 字节生成，因此公共参数和 Session 必须先注入。签名器依赖可注入 Clock，测试固定参数排序、编码和 Body 字节。

签名 Client 禁止 OkHttp 自动重放。HTTP 5xx、业务失败和协议失败不在 Interceptor 中重试；风控成功后只能由原始业务流程显式构造一次新请求。取消原样传播，非幂等写操作不得自动重试。

## 认证分类

HTTP 401/403 与明确的服务认证业务码进入统一分类器：匿名上下文得到 `LoginRequired`，已登录上下文会清理失效 Session 并得到 `SessionExpired`。服务业务码必须由具体请求 Policy 或特殊协议 Spec 显式声明；相同数字出现在其他调用中不能被全局误判。

当前 Search 请求显式识别业务码 `152`。这条规则属于真实协议行为，不是 Endpoint 查表关系。

## 响应与错误

网络层区分 HTTP、业务拒绝、认证、签名/设备、风控、解密、结构不兼容和传输失败。序列化允许已知的上游字段漂移，但 DTO 到领域模型边界必须验证必要字段；不得用空集合掩盖协议错误。

响应读取有界，Body 和 Header 中的服务状态在统一 verifier 中处理。未知字段可忽略，字段兼容证据优先参考当前测试和固定 Mobile 消费行为。
