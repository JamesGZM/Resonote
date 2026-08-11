# Fixture 准入规则

当前静态基线没有可安全认定为完整上游响应的 JSON Fixture，因此本目录暂不包含伪造样例。后续 Fixture 必须：

- 来自固定源码已提交样例或经批准的只读采样。
- 旁置来源、提交或采样条件。
- 删除 token、userid、Cookie、dfid、MID、GUID、设备信息和账号内容。
- 不以人工拼装 JSON 冒充真实响应。
- 在响应 Schema 中把相应字段标为 `FIXTURE_CONFIRMED`。
