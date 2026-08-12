# 登录 API

本页记录 15 个 Lite 包装模块。所有结论均来自固定静态源码；“上游请求”才是 Android 直连需要实现的协议。

[返回 API 文档首页](../README.md)

<a id="api-login-001"></a>
## API-LOGIN-001 · 发送手机验证码

| 属性 | 值 |
|---|---|
| 模块 | <code>captcha_sent.js</code> |
| Node 包装路由 | <code>/captcha/sent</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>write-or-sensitive</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/captcha_sent.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>http://login.user.kugou.com</code> | <code>/v7/send_mobile_code</code> | <code>POST</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>businessid</code> | <code>number</code> | 否/未知 | <code>body</code> | <code>5</code> | <code>SOURCE_CONFIRMED</code> |
| <code>cookie</code> | <code>unknown</code> | 否/未知 | <code>cookie</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>mobile</code> | <code>string</code> | 是 | <code>body</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>plat</code> | <code>number</code> | 否/未知 | <code>body</code> | <code>3</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |

### 返回值证据

| Body 路径 | 证据 |
|---|---|
| <code>status</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>data</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>data.data</code> | <code>CONSUMER_CONFIRMED</code> |

这里只列出源码或固定 PC 消费端能够证明的字段；未列出的字段不代表不存在。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>captchaSent</code> |
| Request DTO | <code>ApiCaptchaSentRequest</code> |
| Response DTO | <code>NetworkApiCaptchaSentResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>OkHttp Call.Factory</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code>, <code>ApiDeviceIdentity</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiCaptchaSentResponse |

<a id="api-login-002"></a>
## API-LOGIN-002 · 获取验证码信息（登录触发二次验证时调用）

| 属性 | 值 |
|---|---|
| 模块 | <code>get_verify_info.js</code> |
| Node 包装路由 | <code>/get/verify/info</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>write-or-sensitive</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>bypass</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/get_verify_info.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/verifyservice/v3/get_verify_info</code> | <code>POST</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>edt</code> | <code>string</code> | 否/未知 | <code>body</code> | <code>''</code> | <code>SOURCE_CONFIRMED</code> |
| <code>eventid</code> | <code>string</code> | 是 | <code>body</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>i</code> | <code>string</code> | 否/未知 | <code>body</code> | <code>''</code> | <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>platid</code> | <code>number</code> | 否/未知 | <code>body</code> | <code>2</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>rtype</code> | <code>number</code> | 否/未知 | <code>body</code> | <code>1</code> | <code>SOURCE_CONFIRMED</code> |
| <code>sid</code> | <code>string</code> | 否/未知 | <code>body</code> | <code>''</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>userid</code> | <code>string</code> | 否/未知 | <code>body</code> | <code>&lt;redacted&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>wasm</code> | <code>number</code> | 否/未知 | <code>body</code> | <code>1</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

| Body 路径 | 证据 |
|---|---|
| <code>data.v_type</code> | <code>REFERENCE_CONFIRMED</code> |
| <code>data.txappid</code> | <code>REFERENCE_CONFIRMED</code> |

这里只列出源码或固定 PC 消费端能够证明的字段；未列出的字段不代表不存在。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>getVerifyInfo</code> |
| Request DTO | <code>ApiGetVerifyInfoRequest</code> |
| Response DTO | <code>NetworkApiGetVerifyInfoResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>OkHttp Call.Factory</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiGetVerifyInfoResponse |

<a id="api-login-003"></a>
## API-LOGIN-003 · 用户名密码登录（可能需要验证，不推荐使用）

| 属性 | 值 |
|---|---|
| 模块 | <code>login.js</code> |
| Node 包装路由 | <code>/login</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>write-or-sensitive</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | <code>SOURCE_CONFIRMED</code> |
| Cookie 回写 | <code>SOURCE_CONFIRMED</code> |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/login.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/v9/login_by_pwd</code> | <code>POST</code> | <code>login.user.kugou.com</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>clienttime_ms</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>code</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>''</code> | <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>params</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>password</code> | <code>string</code> | 是 | <code>module</code> | <code>''</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>pk</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>plat</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>support_multi</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>t1</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>t2</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>username</code> | <code>string</code> | 是 | <code>body</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>x-router</code> | <code>unknown</code> | 否/未知 | <code>header</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

| Body 路径 | 证据 |
|---|---|
| <code>data.token</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data.userid</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data.vip_type</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data.vip_token</code> | <code>SOURCE_CONFIRMED</code> |
| <code>status</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data.secu_params</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>data.data</code> | <code>CONSUMER_CONFIRMED</code> |

这里只列出源码或固定 PC 消费端能够证明的字段；未列出的字段不代表不存在。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>login</code> |
| Request DTO | <code>ApiLoginRequest</code> |
| Response DTO | <code>NetworkApiLoginResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>OkHttp Call.Factory</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code>, <code>ApiResponseDecoder</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiLoginResponse |

<a id="api-login-004"></a>
## API-LOGIN-004 · 手机号验证码登录

| 属性 | 值 |
|---|---|
| 模块 | <code>login_cellphone.js</code> |
| Node 包装路由 | <code>/login/cellphone</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>write-or-sensitive</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | <code>SOURCE_CONFIRMED</code> |
| Cookie 回写 | <code>SOURCE_CONFIRMED</code> |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/login_cellphone.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://loginserviceretry.kugou.com</code> | <code>/v7/login_by_verifycode</code> | <code>POST</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>clienttime_ms</code> | <code>number</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>code</code> | <code>string</code> | 是 | <code>module</code> | <code>''</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>dev</code> | <code>string</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>dfid</code> | <code>string</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>gitversion</code> | <code>string</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>key</code> | <code>string</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>mobile</code> | <code>string</code> | 是 | <code>module</code> | <code>''</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>params</code> | <code>string</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>pk</code> | <code>string</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>plat</code> | <code>number</code> | 否/未知 | <code>body</code> | <code>1</code> | <code>SOURCE_CONFIRMED</code> |
| <code>support_multi</code> | <code>number</code> | 否/未知 | <code>body</code> | <code>1</code> | <code>SOURCE_CONFIRMED</code> |
| <code>support-calm</code> | <code>unknown</code> | 否/未知 | <code>header</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>t1</code> | <code>string</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>t2</code> | <code>string</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>User-Agent</code> | <code>unknown</code> | 否/未知 | <code>header</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>userid</code> | <code>string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |

### 返回值证据

| Body 路径 | 证据 |
|---|---|
| <code>data.token</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data.t1</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data.userid</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data.vip_type</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data.vip_token</code> | <code>SOURCE_CONFIRMED</code> |
| <code>status</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data.secu_params</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data.info_list</code> | <code>REFERENCE_CONFIRMED</code> |
| <code>data.info_list.userid</code> | <code>REFERENCE_CONFIRMED</code> |
| <code>data.info_list.nickname</code> | <code>REFERENCE_CONFIRMED</code> |
| <code>data.info_list.pic</code> | <code>REFERENCE_CONFIRMED</code> |
| <code>data.info_list.p_grade</code> | <code>REFERENCE_CONFIRMED</code> |

这里只列出源码或固定 PC 消费端能够证明的字段；未列出的字段不代表不存在。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>loginCellphone</code> |
| Request DTO | <code>ApiLoginCellphoneRequest</code> |
| Response DTO | <code>NetworkApiLoginCellphoneResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>OkHttp Call.Factory</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code>, <code>ApiDeviceIdentity</code>, <code>ApiResponseDecoder</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiLoginCellphoneResponse |

<a id="api-login-005"></a>
## API-LOGIN-005 · 获取用户设备列表（需登录）

| 属性 | 值 |
|---|---|
| 模块 | <code>login_device.js</code> |
| Node 包装路由 | <code>/login/device</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>write-or-sensitive</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | <code>SOURCE_CONFIRMED</code> |
| Cookie 回写 | 未发现 |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/login_device.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://userinfoservice.kugou.com</code> | <code>/v2/get_dev</code> | <code>POST</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>clienttime_ms</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>params</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>pk</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>plat</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>token</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>userid</code> | <code>string</code> | 否/未知 | <code>body</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>loginDevice</code> |
| Request DTO | <code>ApiLoginDeviceRequest</code> |
| Response DTO | <code>NetworkApiLoginDeviceResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code>, <code>ApiResponseDecoder</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiLoginDeviceResponse |

<a id="api-login-006"></a>
## API-LOGIN-006 · 登出指定设备（需登录）

| 属性 | 值 |
|---|---|
| 模块 | <code>login_device_kick.js</code> |
| Node 包装路由 | <code>/login/device/kick</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>write-or-sensitive</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | <code>SOURCE_CONFIRMED</code> |
| Cookie 回写 | 未发现 |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/login_device_kick.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/loginservice/v1/dev_logout</code> | <code>GET</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>appid</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>clienttime</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>clientver</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>dfid</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>Host</code> | <code>unknown</code> | 否/未知 | <code>header</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>mid</code> | <code>string</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>plat</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>signature</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>srcappid</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>t</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>t_appid</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>t_clientver</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>t_mid</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>token</code> | <code>string</code> | 否/未知 | <code>body</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>userid</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>uuid</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>loginDeviceKick</code> |
| Request DTO | <code>ApiLoginDeviceKickRequest</code> |
| Response DTO | <code>NetworkApiLoginDeviceKickResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code>, <code>ApiDeviceIdentity</code>, <code>ApiResponseDecoder</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiLoginDeviceKickResponse |

<a id="api-login-007"></a>
## API-LOGIN-007 · 开放接口登录（目前仅支持微信）

| 属性 | 值 |
|---|---|
| 模块 | <code>login_openplat.js</code> |
| Node 包装路由 | <code>/login/openplat</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>write-or-sensitive</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | <code>SOURCE_CONFIRMED</code> |
| Cookie 回写 | <code>SOURCE_CONFIRMED</code> |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/login_openplat.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://gateway.kugou.com</code> | <code>/v6/login_by_openplat</code> | <code>POST</code> | <code>login.user.kugou.com</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>clienttime_ms</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>code</code> | <code>string</code> | 是 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>cookie</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>force_login</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>openid</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>params</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>partnerid</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>pk</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>t1</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>t2</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>t3</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>x-router</code> | <code>unknown</code> | 否/未知 | <code>header</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

| Body 路径 | 证据 |
|---|---|
| <code>status</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data.secu_params</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data.token</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data.t1</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data.userid</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data.vip_type</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data.vip_token</code> | <code>SOURCE_CONFIRMED</code> |

这里只列出源码或固定 PC 消费端能够证明的字段；未列出的字段不代表不存在。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>loginOpenplat</code> |
| Request DTO | <code>ApiLoginOpenplatRequest</code> |
| Response DTO | <code>NetworkApiLoginOpenplatResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code>, <code>ApiResponseDecoder</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiLoginOpenplatResponse |

<a id="api-login-008"></a>
## API-LOGIN-008 · 二维码登录 - 检测扫码状态

| 属性 | 值 |
|---|---|
| 模块 | <code>login_qr_check.js</code> |
| Node 包装路由 | <code>/login/qr/check</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>write-or-sensitive</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | <code>SOURCE_CONFIRMED</code> |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/login_qr_check.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://login-user.kugou.com</code> | <code>/v2/get_userinfo_qrcode</code> | <code>GET</code> | <code>-</code> | <code>web</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>appid</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>key</code> | <code>string</code> | 是 | <code>query</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>plat</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>srcappid</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |

### 返回值证据

| Body 路径 | 证据 |
|---|---|
| <code>data.status</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data.token</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data.userid</code> | <code>SOURCE_CONFIRMED</code> |
| <code>status</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>data.nickname</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>data</code> | <code>CONSUMER_CONFIRMED</code> |

这里只列出源码或固定 PC 消费端能够证明的字段；未列出的字段不代表不存在。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>loginQrCheck</code> |
| Request DTO | <code>ApiLoginQrCheckRequest</code> |
| Response DTO | <code>NetworkApiLoginQrCheckResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiLoginQrCheckResponse |

<a id="api-login-009"></a>
## API-LOGIN-009 · 二维码登录 - 生成二维码

| 属性 | 值 |
|---|---|
| 模块 | <code>login_qr_create.js</code> |
| Node 包装路由 | <code>/login/qr/create</code> |
| 认证 | <code>anonymous</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>write-or-sensitive</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>none-or-dynamic</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/login_qr_create.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>dynamic</code> | <code>dynamic</code> | <code>dynamic</code> | <code>DYNAMIC</code> | <code>-</code> | <code>unknown</code> | <code>unknown</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>key</code> | <code>string</code> | 是 | <code>module</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>qrimg</code> | <code>string &#124; boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |

### 返回值证据

| Body 路径 | 证据 |
|---|---|
| <code>code</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>data.base64</code> | <code>CONSUMER_CONFIRMED</code> |

这里只列出源码或固定 PC 消费端能够证明的字段；未列出的字段不代表不存在。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>loginQrCreate</code> |
| Request DTO | <code>ApiLoginQrCreateRequest</code> |
| Response DTO | <code>NetworkApiLoginQrCreateResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>OkHttp Call.Factory</code> |
| 协议组件 | <code>none</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiLoginQrCreateResponse |

<a id="api-login-010"></a>
## API-LOGIN-010 · 二维码登录 - 生成 key

| 属性 | 值 |
|---|---|
| 模块 | <code>login_qr_key.js</code> |
| Node 包装路由 | <code>/login/qr/key</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>write-or-sensitive</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/login_qr_key.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://login-user.kugou.com</code> | <code>/v2/qrcode</code> | <code>GET</code> | <code>-</code> | <code>web</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>plat</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>qrcode_txt</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>srcappid</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>type</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

| Body 路径 | 证据 |
|---|---|
| <code>status</code> | <code>CONSUMER_CONFIRMED</code> |
| <code>data.qrcode</code> | <code>CONSUMER_CONFIRMED</code> |

这里只列出源码或固定 PC 消费端能够证明的字段；未列出的字段不代表不存在。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>loginQrKey</code> |
| Request DTO | <code>ApiLoginQrKeyRequest</code> |
| Response DTO | <code>NetworkApiLoginQrKeyResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiLoginQrKeyResponse |

<a id="api-login-011"></a>
## API-LOGIN-011 · 刷新登录状态，延长 token 过期时间

| 属性 | 值 |
|---|---|
| 模块 | <code>login_token.js</code> |
| Node 包装路由 | <code>/login/token</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>write-or-sensitive</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | <code>SOURCE_CONFIRMED</code> |
| Cookie 回写 | <code>SOURCE_CONFIRMED</code> |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/login_token.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>http://login.user.kugou.com</code> | <code>/v5/login_by_token</code> | <code>POST</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>clienttime_ms</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>cookie</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>p3</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>params</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>pk</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>plat</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>t1</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>t2</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>t3</code> | <code>unknown</code> | 否/未知 | <code>body</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>token</code> | <code>string</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>userid</code> | <code>string</code> | 否/未知 | <code>body</code> | <code>&lt;redacted&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |

### 返回值证据

| Body 路径 | 证据 |
|---|---|
| <code>data.token</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data.t1</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data.userid</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data.vip_type</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data.vip_token</code> | <code>SOURCE_CONFIRMED</code> |
| <code>status</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data.secu_params</code> | <code>SOURCE_CONFIRMED</code> |

这里只列出源码或固定 PC 消费端能够证明的字段；未列出的字段不代表不存在。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>loginToken</code> |
| Request DTO | <code>ApiLoginTokenRequest</code> |
| Response DTO | <code>NetworkApiLoginTokenResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>Retrofit</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code>, <code>ApiDeviceIdentity</code>, <code>ApiResponseDecoder</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiLoginTokenResponse |

<a id="api-login-012"></a>
## API-LOGIN-012 · 微信登录 - 检测扫码状态

| 属性 | 值 |
|---|---|
| 模块 | <code>login_wx_check.js</code> |
| Node 包装路由 | <code>/login/wx/check</code> |
| 认证 | <code>anonymous</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>write-or-sensitive</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | <code>SOURCE_CONFIRMED</code> |
| Cookie 回写 | 未发现 |
| 风控 | <code>none-or-dynamic</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/login_wx_check.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>dynamic</code> | <code>dynamic</code> | <code>dynamic</code> | <code>DYNAMIC</code> | <code>-</code> | <code>unknown</code> | <code>unknown</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>uuid</code> | <code>string</code> | 是 | <code>module</code> | <code>''</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>loginWxCheck</code> |
| Request DTO | <code>ApiLoginWxCheckRequest</code> |
| Response DTO | <code>NetworkApiLoginWxCheckResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>OkHttp Call.Factory</code> |
| 协议组件 | <code>ApiResponseDecoder</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiLoginWxCheckResponse |

<a id="api-login-013"></a>
## API-LOGIN-013 · 微信登录 - 生成二维码

| 属性 | 值 |
|---|---|
| 模块 | <code>login_wx_create.js</code> |
| Node 包装路由 | <code>/login/wx/create</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>write-or-sensitive</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | <code>SOURCE_CONFIRMED</code> |
| Cookie 回写 | 未发现 |
| 风控 | <code>none-or-dynamic</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/login_wx_create.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>dynamic</code> | <code>dynamic</code> | <code>dynamic</code> | <code>DYNAMIC</code> | <code>-</code> | <code>unknown</code> | <code>unknown</code> |

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
| DataSource 操作 | <code>loginWxCreate</code> |
| Request DTO | <code>ApiLoginWxCreateRequest</code> |
| Response DTO | <code>NetworkApiLoginWxCreateResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>OkHttp Call.Factory</code> |
| 协议组件 | <code>ApiSession</code>, <code>ApiResponseDecoder</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiLoginWxCreateResponse |

<a id="api-login-014"></a>
## API-LOGIN-014 · 生成 sid/edt 并提交验证（内部调用 generateSimulate + verify_user_info）

| 属性 | 值 |
|---|---|
| 模块 | <code>sidedt.js</code> |
| Node 包装路由 | <code>/sidedt</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | 未发现模块级转换 |
| Cookie 回写 | 未发现 |
| 风控 | <code>bypass</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/sidedt.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>dynamic</code> | <code>dynamic</code> | <code>dynamic</code> | <code>DYNAMIC</code> | <code>-</code> | <code>unknown</code> | <code>unknown</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>dfid</code> | <code>string</code> | 否/未知 | <code>module</code> | <code>&lt;redacted&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>mid</code> | <code>string</code> | 否/未知 | <code>module</code> | <code>&lt;redacted&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>userid</code> | <code>string</code> | 否/未知 | <code>module</code> | <code>&lt;redacted&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>sidedt</code> |
| Request DTO | <code>ApiSidedtRequest</code> |
| Response DTO | <code>NetworkApiSidedtResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>OkHttp Call.Factory</code> |
| 协议组件 | <code>ApiSession</code>, <code>ApiDeviceIdentity</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiSidedtResponse |

<a id="api-login-015"></a>
## API-LOGIN-015 · 提交验证码验证（腾讯验证码/手机验证码）

| 属性 | 值 |
|---|---|
| 模块 | <code>verify_user_info.js</code> |
| Node 包装路由 | <code>/verify/user/info</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>write-or-sensitive</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | <code>SOURCE_CONFIRMED</code> |
| Cookie 回写 | 未发现 |
| 风控 | <code>bypass</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/verify_user_info.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://verifyservice.kugou.com</code> | <code>/v4/verify_user_info</code> | <code>POST</code> | <code>-</code> | <code>android</code> | <code>json</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>clientver</code> | <code>number</code> | 否/未知 | <code>query</code> | <code>11510</code> | <code>SOURCE_CONFIRMED</code> |
| <code>edt</code> | <code>string</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>eventid</code> | <code>string</code> | 是 | <code>module</code> | <code>-</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>i</code> | <code>string</code> | 否/未知 | <code>body</code> | <code>''</code> | <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>platid</code> | <code>number</code> | 否/未知 | <code>module</code> | <code>2</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>sid</code> | <code>string</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>userid</code> | <code>string</code> | 否/未知 | <code>module</code> | <code>&lt;redacted&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>v_type</code> | <code>number</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>verifycode</code> | <code>string</code> | 是 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>DECLARED</code>, <code>SOURCE_CONFIRMED</code> |
| <code>wasm</code> | <code>number</code> | 否/未知 | <code>body</code> | <code>1</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

上游响应由包装层透传，静态证据未确认字段级结构：`UNKNOWN`。不得据此生成严格 Kotlin DTO。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>verifyUserInfo</code> |
| Request DTO | <code>ApiVerifyUserInfoRequest</code> |
| Response DTO | <code>NetworkApiVerifyUserInfoResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>OkHttp Call.Factory</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code>, <code>ApiResponseDecoder</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiVerifyUserInfoResponse |
