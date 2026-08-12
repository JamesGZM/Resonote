# 设备与验证 API

本页记录 1 个 Lite 包装模块。所有结论均来自固定静态源码；“上游请求”才是 Android 直连需要实现的协议。

[返回 API 文档首页](../README.md)

<a id="api-device-001"></a>
## API-DEVICE-001 · 获取 dfid（设备标识），获取音乐 URL 前需先调用此接口

| 属性 | 值 |
|---|---|
| 模块 | <code>register_dev.js</code> |
| Node 包装路由 | <code>/register/dev</code> |
| 认证 | <code>optional</code>（INFERRED，除非响应/源码另有证据） |
| 操作属性 | <code>read</code> |
| 产品范围 | <code>candidate</code> |
| 验证 | <code>static-only</code> |
| 响应转换 | <code>SOURCE_CONFIRMED</code> |
| Cookie 回写 | <code>SOURCE_CONFIRMED</code> |
| 风控 | <code>surface-challenge</code> |
| 来源 | <code>MoeKoeMusic/api@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb:module/register_dev.js</code> |

### 上游请求

| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |
|---:|---|---|---|---|---|---|---|
| 1 | <code>useAxios</code> | <code>https://userservice.kugou.com</code> | <code>/risk/v2/r_register_dev</code> | <code>POST</code> | <code>-</code> | <code>android</code> | <code>arraybuffer</code> |

### 请求字段

| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |
|---|---|---:|---|---|---|
| <code>accelerometer</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>false</code> | <code>SOURCE_CONFIRMED</code> |
| <code>accelerometerValue</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>''</code> | <code>SOURCE_CONFIRMED</code> |
| <code>availableRamSize</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>4983533568</code> | <code>SOURCE_CONFIRMED</code> |
| <code>availableRomSize</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>48114719</code> | <code>SOURCE_CONFIRMED</code> |
| <code>availableSDSize</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>48114717</code> | <code>SOURCE_CONFIRMED</code> |
| <code>basebandVer</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>''</code> | <code>SOURCE_CONFIRMED</code> |
| <code>batteryLevel</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>100</code> | <code>SOURCE_CONFIRMED</code> |
| <code>batteryStatus</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>3</code> | <code>SOURCE_CONFIRMED</code> |
| <code>brand</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>'Redmi'</code> | <code>SOURCE_CONFIRMED</code> |
| <code>buildSerial</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>'unknown'</code> | <code>SOURCE_CONFIRMED</code> |
| <code>device</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>'marble'</code> | <code>SOURCE_CONFIRMED</code> |
| <code>gravity</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>false</code> | <code>SOURCE_CONFIRMED</code> |
| <code>gravityValue</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>''</code> | <code>SOURCE_CONFIRMED</code> |
| <code>gyroscope</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>false</code> | <code>SOURCE_CONFIRMED</code> |
| <code>gyroscopeValue</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>''</code> | <code>SOURCE_CONFIRMED</code> |
| <code>imei</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>imsi</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>''</code> | <code>SOURCE_CONFIRMED</code> |
| <code>light</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>false</code> | <code>SOURCE_CONFIRMED</code> |
| <code>lightValue</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>''</code> | <code>SOURCE_CONFIRMED</code> |
| <code>magnetic</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>false</code> | <code>SOURCE_CONFIRMED</code> |
| <code>magneticValue</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>''</code> | <code>SOURCE_CONFIRMED</code> |
| <code>manufacturer</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>'Xiaomi'</code> | <code>SOURCE_CONFIRMED</code> |
| <code>noCookie</code> | <code>boolean</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>orientation</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>false</code> | <code>SOURCE_CONFIRMED</code> |
| <code>orientationValue</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>''</code> | <code>SOURCE_CONFIRMED</code> |
| <code>p</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>part</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>platid</code> | <code>unknown</code> | 否/未知 | <code>query</code> | <code>-</code> | <code>SOURCE_CONFIRMED</code> |
| <code>pressure</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>false</code> | <code>SOURCE_CONFIRMED</code> |
| <code>pressureValue</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>''</code> | <code>SOURCE_CONFIRMED</code> |
| <code>step_counter</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>false</code> | <code>SOURCE_CONFIRMED</code> |
| <code>step_counterValue</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>''</code> | <code>SOURCE_CONFIRMED</code> |
| <code>temperature</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>false</code> | <code>SOURCE_CONFIRMED</code> |
| <code>temperatureValue</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>''</code> | <code>SOURCE_CONFIRMED</code> |
| <code>timestamp</code> | <code>number &#124; string</code> | 否/未知 | <code>module</code> | <code>-</code> | <code>DECLARED</code> |
| <code>token</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>userid</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |
| <code>uuid</code> | <code>unknown</code> | 否/未知 | <code>module</code> | <code>&lt;source-expression&gt;</code> | <code>SOURCE_CONFIRMED</code> |

### 返回值证据

| Body 路径 | 证据 |
|---|---|
| <code>data.dfid</code> | <code>SOURCE_CONFIRMED</code> |
| <code>status</code> | <code>SOURCE_CONFIRMED</code> |
| <code>data.data</code> | <code>CONSUMER_CONFIRMED</code> |

这里只列出源码或固定 PC 消费端能够证明的字段；未列出的字段不代表不存在。

### Android 映射

| 项目 | 建议 |
|---|---|
| DataSource 操作 | <code>registerDev</code> |
| Request DTO | <code>ApiRegisterDevRequest</code> |
| Response DTO | <code>NetworkApiRegisterDevResponse</code>；含 UNKNOWN 时不得据此生成严格 DTO |
| 传输实现 | <code>OkHttp Call.Factory</code> |
| 协议组件 | <code>ApiRequestSigner</code>, <code>ApiSession</code>, <code>ApiDeviceIdentity</code>, <code>ApiResponseDecoder</code> |
| 领域映射 | 在 <code>core:data</code> 映射；不得向 UI 暴露 NetworkApiRegisterDevResponse |
