# strong-ai
## 项目简介
### 功能简介


## 技术栈
- spring boot2.4.2, spring cloud2020,
- mysql, mybatis, mybatis-plus, hikaricp,
- xxl-job
- redis-cluster
- rabbitmq
- nacos
- dubbo


## git仓库地址


## 接口地址


## 备注


## 使用说明
- 服务器端口号：11028

## 开发环境及依赖


## 快速启动

## swagger地址

http://localhost:11028/doc.html

## 使用示例

### 网页端

应用启动后访问 [http://localhost:11028/](http://localhost:11028/) 即可打开聊天页面。页面支持 OpenAI、DeepSeek 模型切换、SSE 流式回答、停止生成、重新生成、会话搜索和深浅主题。会话记录保存在当前浏览器的 `localStorage` 中，不会同步到其他浏览器或设备。

访问 [http://localhost:11028/md-optimizer.html](http://localhost:11028/md-optimizer.html) 可打开 Markdown 优化台。支持直接输入、上传 `.md/.markdown` 文件或上传仅包含一个 Markdown 文件的 ZIP 文档包；ZIP 中的图片可在预览中展示，其他附件会保留并支持下载，优化结果可重新导出为 ZIP。新增“解析图片”入口，可上传 PNG/JPEG/WebP/GIF 图片并由视觉模型生成 Markdown。页面提供格式整理、内容润色和结构重组三种模式，并支持 Markdown 预览、Diff、复制和下载。

### AI 聊天

AI 调用由 `ai4j-spring-boot-starter` 自动装配。`bootstrap.yml` 的 `ai.platforms` 以 `id` 作为接口的 `provider`，以 `platform` 指定模型厂商实现。密钥必须通过环境变量或 Nacos 密文配置注入。OpenAI 使用 `OPENAI_API_KEY`，可通过 `OPENAI_API_HOST`、`OPENAI_CHAT_COMPLETION_URL` 和 `OPENAI_RESPONSES_URL` 覆盖默认端点。

GPT-5.6：

```bash
curl -X POST http://localhost:11028/api/ai/chat \
  -H 'Content-Type: application/json' \
  -d '{"provider":"openai","model":"gpt-5.6-sol","messages":[{"role":"user","content":"你好"}]}'
```

DeepSeek：

```bash
curl -X POST http://localhost:11028/api/ai/chat \
  -H 'Content-Type: application/json' \
  -d '{"provider":"deepseek","model":"deepseek-chat","messages":[{"role":"user","content":"你好"}]}'
```

### Markdown 优化

```bash
curl -X POST http://localhost:11028/api/md-optimizer/optimize \
  -H 'Content-Type: application/json' \
  -d '{"content":"# 标题\n\n正文","mode":"format","documentType":"README","provider":"openai","model":"gpt-5.6-sol"}'
```

## 注意

- swagger 文档

1. 只允许在**测试环境**开启, 其它环境**不允许**开启
2. 默认关闭, 如需启用, 请在**配置中心**(如 nacos `config.properties`)中增加配置 `strong.swagger.enable = true`
