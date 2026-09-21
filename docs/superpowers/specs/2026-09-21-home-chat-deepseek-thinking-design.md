# 首页会话 DeepSeek 深度思考设计

## 目标

- 新建首页会话默认使用 `deepseek:deepseek-v4.1-flash`。
- DeepSeek V4.1 Flash 会话在输入框显示“深度思考”按钮，默认关闭，允许用户切换。
- 保留 GPT-5.6 现有深度思考能力。

## 交互

- 按钮沿用输入框现有样式、图标和开启态，不新增视觉组件。
- DeepSeek V4.1 Flash 和 OpenAI GPT-5.6 显示按钮。
- DeepSeek Reasoner 是固定推理模型，不显示可切换按钮。
- 发送期间按钮禁用，切换结果仍作为当前会话配置持久化。
- 已有会话不迁移模型；只有新建会话使用新的默认模型。

## 数据流

1. 前端新建会话时发送 `provider: "deepseek"`、`model: "deepseek-v4.1-flash"`。
2. 用户切换按钮时，前端继续通过会话更新接口保存 `deepThinking`。
3. 服务端从会话构造聊天请求，并将 `deepThinking` 传入模型请求。
4. DeepSeek V4.1 Flash 使用顶层扩展参数 `thinking.type`：开启为 `enabled`，关闭为 `disabled`。
5. GPT-5.6 继续使用 `reasoning_effort`，不改变既有行为。

## 测试

- Web UI 资源测试验证新建会话的默认 provider/model。
- Web UI 资源测试验证 DeepSeek V4.1 Flash 支持显示深度思考按钮。
- 服务测试验证 DeepSeek 开启和关闭时生成正确的 `thinking.type` 扩展参数。
- 运行相关 Web UI 与 AI 聊天服务测试，确认 GPT-5.6 行为没有回归。

## 非目标

- 不修改已有会话数据。
- 不改变 DeepSeek Reasoner 的固定推理行为。
- 不调整 Markdown 优化台或文档治理台的默认模型。
