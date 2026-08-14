# Network 文档规则

本目录遵循根 [工程规则](../../AGENTS.md)，并只描述 Resonote 当前 App 使用的网络能力。

- 以 `core/network` 的公开 DataSource、`core/data` 的 Repository 消费和测试为事实源。
- 每项能力记录语义化操作、实现方式、Method/Host/Path、认证/Session、实际响应模型与验证状态；不维护人工数字 ID、Endpoint 注册表或上游全集。
- 请求路径、参数、签名、Session、认证分类和映射由 Gradle/JUnit 测试保证。文档校验只检查入口和链接，不扫描源码方法名。
- `../MoeKoeMusic` 与 `../MoeKoeMusic-Mobile` 只提供固定源码证据；未被 Resonote 使用的上游接口不进入主文档。
- 禁止记录 Token、Cookie、手机号、设备标识、签名密钥、完整真实响应或其他敏感信息。Fixture 必须最小化并脱敏。
- Live Canary 必须显式启用；文档应区分 Fixture、Canary 与真实账号验证，不把静态阅读写成运行通过。
