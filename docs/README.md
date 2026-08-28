# Resonote 文档导航

这里收录需要随产品和源码长期维护的文档。临时实现计划、单次 QA 过程、机器生成的对比报告和本机路径记录不属于长期文档，不应提交到本目录。

## 产品与设计

- [产品需求](../design/PRODUCT_REQUIREMENTS.md)：现行产品定位、范围、信息架构和行为合同。
- [设计基础](../design/FOUNDATION.md)：品牌、色彩、排版、图标和自适应基础。
- [组件系统](../design/COMPONENT_SYSTEM.md)：通用组件、导航和音乐组件合同。

## 工程

- [架构](ARCHITECTURE.md)：模块职责、依赖方向、数据流和导航拓扑。
- [开发指南](DEVELOPMENT.md)：代码放置、测试策略、格式检查和发布门禁。
- [网络能力](api/README.md)：当前应用使用的网络能力入口。
- [网络协议](api/PROTOCOL.md)：签名、Session、默认参数和错误处理规则。
- [网络验证](api/VERIFICATION.md)：自动化测试与协议运行证据。

## 架构决策

`adr/` 保存会长期影响实现的架构决策及其取舍。日常开发优先遵循现行架构和开发指南；只有在追溯原因或评估边界变化时才需要查阅 ADR。

## 维护原则

- 用户已验收的行为、当前源码与测试优先于旧截图和历史讨论。
- 现行规则写入产品、设计、架构或协议文档；仍影响实现的重大技术取舍写入 ADR。
- 临时任务清单、单次验收日志和构建产物留在 Issue、Pull Request 或 CI Artifact 中。
- 已失效的讨论和过程记录由 Git、Issue 与 Pull Request 保留，不在仓库内重复维护历史流水账。
- 修改文档后运行 `./gradlew checkDocumentation`，确保入口和相对链接有效。
