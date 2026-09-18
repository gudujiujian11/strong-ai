# 技术文档治理台 AI 优化模型选择设计

## 目标

在技术文档治理台编辑区为“AI 优化”增加模型下拉选择。当前只展示并支持 `OpenAI / GPT-5.6 Luna`，用户点击“AI 优化”时将选择结果提交给现有 AI 优化任务接口。

## 方案

- 在 `document-governance.html` 的编辑操作区增加原生 `select` 控件，放在“AI 优化”按钮左侧。
- 选项固定为 provider `openai`、model `gpt-5.6-luna`，初始化默认选中。
- 在 `document-governance.js` 中缓存控件引用；执行优化时从下拉框读取 provider/model，并沿用现有任务创建、重试和 SSE 状态流逻辑。
- 在 `document-governance.css` 中复用现有表单变量与响应式布局，保证桌面端和窄屏端不改变既有编辑操作可用性。
- 不修改后端接口、数据库或模型校验逻辑。

## 验证

- 静态资源测试确认模型选择控件和 `gpt-5.6-luna` 存在。
- JavaScript 源码检查确认任务请求使用下拉框当前值，而非硬编码旧模型。
- 运行 `strong-ai-webapp` 相关 Maven 测试。
