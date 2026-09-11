# 图片解析 Markdown 设计

## 目标

在 MD 优化台增加“解析图片”能力：用户上传一张图片后，调用已配置的多模态模型，生成可复制、可下载的完整 Markdown 文档；现有 Markdown 优化与 ZIP 流程保持不变。

## 方案

- 新增 `POST /api/md-optimizer/parse-image` multipart 接口，字段为 `image`、`provider`、`model`、`maxTokens`。
- 后端校验图片非空、扩展名/MIME 类型和 10 MB 大小限制，将图片编码为 data URL，通过 ai4j 的 `ChatMessage.withUser(text, image)` 发送。
- 使用独立的 `ImageMarkdownParserService` 组织提示词和结果清理，只返回 Markdown 正文；模型包裹的代码围栏或说明文字会被去除。
- 返回复用 `MarkdownOptimizeResponse` 的结果字段，`originalContent` 记录图片文件名提示，`optimizedContent` 为生成的 Markdown，保留模型与 token 信息。
- 前端在输入方式区域增加“解析图片”页签和图片上传区，上传后显示文件名/预览，点击解析调用接口；结果继续使用预览、源码、复制、下载能力。

## 边界与错误

- 支持 PNG、JPEG、WebP、GIF；不接受 SVG、PDF、ZIP 或未知类型。
- 仅允许单张图片，超过 10 MB 或无法识别时返回明确的参数错误。
- 不记录图片内容、base64、提示词或密钥；图片只在请求生命周期内存在。
- 未配置视觉模型或模型拒绝图片时返回现有统一错误格式，前端保留已选文件并允许重试。

## 验证

- 服务单元测试覆盖图片校验、data URL 构造、提示词约束和结果清理。
- Controller 测试覆盖 multipart 成功与非法文件。
- Web UI 资源测试断言新入口、接口地址和结果操作存在。
