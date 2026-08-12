# Lite 公共协议

## 请求管线

标准 HTTP/JSON 业务端点由私有 Retrofit Service 直接返回类型化 `ApiResponse<T>`；方法级策略由 OkHttp application interceptor 读取，依次注入公共参数/Session、对最终 Query 与 Body 字节签名，并将已知 `ssa-code` Header 归一化到 JSON 信封。设备注册在 Retrofit 调用前以 suspend single-flight 完成，不允许 Interceptor 发起嵌套请求。

Android 直连必须复现固定 API 基线的请求上下文：Lite `appid=3116`、`clientver=11440`、秒级 `clienttime`、持久化设备身份，以及按端点选择的签名、请求体和响应解码。默认网关为 `https://gateway.kugou.com`；带 `x-router` 的请求仍以该网关为传输入口。

## 公共参数与请求头

| 名称 | 位置 | 来源 | 说明 |
|---|---|---|---|
| `dfid` | Query/Header | DeviceSession | 设备注册结果；未注册时源码可使用占位值 |
| `mid` | Query/Header | DeviceIdentity | 由持久化 GUID 按固定算法派生 |
| `uuid` | Query | Provider | 固定基线默认 `-` |
| `appid` | Query | Lite Config | 3116 |
| `clientver` | Query | Lite Config | 11440，个别端点会覆盖 |
| `clienttime` | Query/Header | Clock | 秒级时间戳，必须由可注入时钟提供 |
| `token` / `userid` | Query/Body | Session | 登录后按端点注入 |
| `x-router` | Header | Endpoint | 选择网关后端，不能误当作 Retrofit Base URL |

固定源码还注入 User-Agent 与若干 KG 路由头。具体字面量和签名材料不在文档重复，后续实现须从固定 MIT 源码逐项迁移并用 golden test 锁定；凭证型配置不得写入文档或 Fixture。

## 签名模式

- `android`：对规范化 Query 和序列化 Body 生成 Android signature。
- `web`：用于二维码等 Web 登录协议。
- `register`：用于设备注册协议。
- `none`：源码显式跳过 signature，可能仍有端点自定义 key。
- `unknown`：无法仅由模块静态确定，实施前必须补证据。

签名器必须依赖可注入 `Clock`，并保持参数排序、字符串化、Body 字节和 URL 编码与 Node 基线一致。

## 会话和设备身份

会话至少包含 `token`、`userid`、`vip_token`、`vip_type`；设备上下文至少包含 `dfid`、GUID、MID、DEV 和平台标识。设备注册通过可注入 Provider 按 Mobile 合同读取当前 Android 设备的总内存、品牌、Build ID、型号和厂商，缺失时使用固定 fallback，存储字段继续采用 Mobile 的固定兼容值；它优先读取解密后的 `data.dfid`，并与 Mobile 通用 Cookie 合并链一致地接受响应 `Set-Cookie` 中的 `dfid`，两处都缺失时必须报告协议错误，不能带占位值继续业务请求。PC 的 Authorization 拼接只是包装层传输格式，Android 直连不得把它原样发送给上游，而应按端点写入 Query、Body、Header 或 Cookie。敏感值必须持久化加密，日志和 Fixture 一律脱敏。

## 登录 Origin 与 Lite 条件

- 发送手机验证码：`http://login.user.kugou.com/v7/send_mobile_code`，唯一允许的明文 Origin，只携带 MID 身份。
- 手机验证码登录：`https://loginserviceretry.kugou.com/v7/login_by_verifycode`。
- 密码登录：`https://gateway.kugou.com/v9/login_by_pwd`，并设置 `x-router: login.user.kugou.com`。
- 风控提交：`https://verifyservice.kugou.com/v4/verify_user_info`；扫码端点使用各自独立 HTTPS Origin。

Lite 验证码登录固定发送 `t1/t2/dfid/dev/gitversion`，不得发送 Standard 分支的 `t3`。登录成功必须解密 `secu_params`、校验 token/userid、合并响应 Cookie，再由数据层原子提交加密 Session。

## 加密与二进制

固定基线出现 AES、RSA 公钥加密、歌单/云盘 AES 封装、KRC 解码、ArrayBuffer 和 PCM/文件二进制。标准 HTTP/JSON 端点使用 Retrofit（动态 URL 可用 `@Url` 表达）；二进制、加密或多阶段特殊协议由内部 `ProtocolTransport` 使用共享 OkHttp `Call.Factory`，不把特殊编排塞入普通 Retrofit 接口或同步 Interceptor。

## 重试边界

签名 API Client 禁用 OkHttp 连接失败自动重放；HTTP 5xx、业务错误和协议错误均不在 Interceptor 中重试。风控验证成功后只能由原发起流程显式创建一次新请求，使时间戳与签名重新生成；写操作没有幂等保证时不得自动重试。取消必须原样传播。

## 风控 SID/EDT

固定 PC 包装层在仅收到 `ssa-code` Header 时不会等待上游返回 `sid/edt`，而是使用当前 MID、userid、dfid、进程级 WebGL 指纹和行为事件生成 EDT，并以 RSA-OAEP(SHA-256/MGF1-SHA-256) 封装临时 AES 密钥得到 SID。Android 协议层在 Challenge 已携带完整 `sid/edt` 时保留原值，仅对缺失上下文生成一次，并且只在验证提交请求的协程内存中使用。

## 错误模型

必须分别保留 HTTP 失败、Provider 业务失败、签名/设备失败、登录过期、风控验证、解密失败、结构不兼容和网络失败。上游常同时使用 HTTP 状态与 Body 内 `status`/`error_code`；静态文档没有证明二者存在统一关系。`error_code` 缺失或数值零表示无该错误，任何非空且不等价于数值零的值（包括非数字字符串）都按业务拒绝处理。携带 `ssa-code` 的响应只能有界读取，超限和畸形 Body 均关闭后报告协议错误。

## 响应兼容策略

`UNKNOWN` 或仅 `CONSUMER_CONFIRMED` 的响应不得直接转成全字段非空 DTO。初次实现应忽略未知键、对漂移字段使用受控宽容序列化，并在 Network DTO 到领域模型边界完成校验。
