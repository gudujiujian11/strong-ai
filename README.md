# Strong AI

Strong AI 是一个基于 Spring Boot 的多模型 AI 应用，提供网页聊天、服务端会话管理、Markdown 优化、图片转 Markdown 和技术文档治理能力。

## 功能

- 多模型 AI 聊天：统一接入 OpenAI、DeepSeek、豆包和通义千问等模型，支持同步与 SSE 流式调用。
- 会话管理：服务端保存会话、历史消息和模型设置，支持新建、切换模型、清空和重新生成回复。
- Markdown 优化：支持内容校验、格式整理、内容润色和结构重组；可处理 Markdown 文件、单 Markdown 文件的 ZIP 包，以及 PNG/JPEG/WebP/GIF 图片。
- 技术文档治理：支持团队内文档草稿、质量检查、AI 优化任务、版本发布与回滚、术语库和 30 天草稿清理。

## 模块

| 模块 | 职责 |
| --- | --- |
| `strong-ai-webapp` | Spring Boot Web 应用、REST API、静态页面与业务实现 |
| `strong-ai-model` | API 请求、响应和领域模型 |
| `strong-ai-mq-model` | 消息队列模型 |
| `strong-ai-remote-api` | OpenFeign 远程调用契约 |

## 技术栈

- Java 8、Spring Boot 2.4.2、Spring Cloud 2020
- MySQL、MyBatis-Plus、HikariCP、Flyway
- Nacos 配置中心与注册中心、OpenFeign
- `ai4j-spring-boot-starter`、SSE、Log4j2
- 原生 HTML、CSS、JavaScript 静态工作台

## 前置条件

- JDK 8
- Maven 3.6.3：`/Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn`
- MySQL 数据库
- Nacos。默认会加载 `config.properties`、`cache.properties`、`mq.properties`、`db.properties` 和 `log4j2-spring.xml`。
- 至少配置一个 AI 平台的密钥。密钥仅允许通过环境变量或 Nacos 密文配置提供，禁止提交到仓库。

应用端口默认为 `11028`。数据库迁移由 Flyway 在启动时执行。

## 启动

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am clean package
java -jar strong-ai-webapp/target/strong-ai.jar
```

启动后可访问：

| 地址 | 用途 |
| --- | --- |
| `http://localhost:11028/` | AI 聊天与会话管理 |
| `http://localhost:11028/md-optimizer.html` | Markdown 优化台 |
| `http://localhost:11028/document-governance.html` | 技术文档治理台 |
| `http://localhost:11028/doc.html` | Swagger 文档，仅限测试环境开启 |

Swagger 默认关闭。仅在测试环境通过配置中心设置 `strong.swagger.enable=true` 后启用。

## AI 平台配置

`bootstrap.yml` 中的 `ai.platforms` 以 `id` 作为请求的 `provider`，以 `platform` 指定 SDK 实现。常用环境变量如下：

| 平台 | 密钥变量 | 可选地址变量 |
| --- | --- | --- |
| OpenAI | `OPENAI_API_KEY` | `OPENAI_API_HOST`、`OPENAI_CHAT_COMPLETION_URL`、`OPENAI_RESPONSES_URL` |
| DeepSeek | `DEEPSEEK_API_KEY` | `DEEPSEEK_API_HOST`、`DEEPSEEK_CHAT_COMPLETION_URL` |
| 豆包 | `DOUBAO_API_KEY` | `DOUBAO_API_HOST`、`DOUBAO_CHAT_COMPLETION_URL` |
| 通义千问 | `QWEN_API_KEY` | `QWEN_API_HOST`、`QWEN_RESPONSES_URL` |

可用模型名称由对应平台决定。示例中使用的 `gpt-5.6-sol` 仅在已配置该模型的平台可用。

## 核心接口

| 分组 | 路径 | 说明 |
| --- | --- | --- |
| AI 聊天 | `POST /api/ai/chat` | 同步调用模型 |
| AI 聊天 | `POST /api/ai/chat/stream` | SSE 流式调用模型 |
| 会话 | `/api/conversations` | 会话及历史消息的增删改查 |
| 会话 | `POST /api/conversations/{id}/messages/stream` | 基于服务端会话历史流式生成回复 |
| Markdown | `POST /api/md-optimizer/validate` | 校验 Markdown 内容与文件名 |
| Markdown | `POST /api/md-optimizer/optimize` | 同步优化 Markdown |
| Markdown | `POST /api/md-optimizer/parse-image` | 上传图片并生成 Markdown |
| 文档治理 | `/api/document-governance/documents` | 文档草稿、发布、回滚、版本和质量检查 |
| 文档治理 | `/api/document-governance/ai-tasks/{taskId}` | 查询 AI 优化任务及 SSE 状态流 |
| 文档治理 | `/api/document-governance/terms` | 查询或维护当前团队术语 |
| Codex 网关 | `POST /api/codex/v1/responses` | 固定使用 `deepseek-v4.1-flash` 的 Responses 兼容接口 |
| Codex 网关 | `GET /api/codex/v1/models` | 返回固定模型列表 |

### Codex 网关配置

网关只接受 `CODEX_GATEWAY_API_KEY`，上游 DeepSeek 使用 `DEEPSEEK_API_KEY`，地址默认
为 `https://api.deepseek.com`。两个密钥均通过环境变量或 Nacos 密文配置注入：

```bash
export CODEX_GATEWAY_API_KEY='供 Codex 使用的内部密钥'
export DEEPSEEK_API_KEY='DeepSeek API 密钥'
```

Codex 用户配置（`~/.codex/config.toml`）示例：

```toml
model = "deepseek-v4.1-flash"
model_provider = "strong_ai"

[model_providers.strong_ai]
name = "Strong AI DeepSeek V4.1 Flash"
base_url = "https://your-domain.example.com/api/codex/v1"
env_key = "CODEX_GATEWAY_API_KEY"
wire_api = "responses"
```

### AI 聊天示例

```bash
curl -X POST http://localhost:11028/api/ai/chat \
  -H 'Content-Type: application/json' \
  -d '{"provider":"openai","model":"gpt-5.6-sol","messages":[{"role":"user","content":"你好"}]}'
```

### Markdown 优化示例

```bash
curl -X POST http://localhost:11028/api/md-optimizer/optimize \
  -H 'Content-Type: application/json' \
  -d '{"content":"# 标题\n\n正文","mode":"format","documentType":"README","provider":"openai","model":"gpt-5.6-sol"}'
```

## 技术文档治理身份

文档治理 API 依赖可信网关注入以下请求头：

```text
X-Strong-User-Id
X-Strong-Team-Id
X-Strong-Role: member | admin
```

网关必须先移除客户端伪造的同名请求头，再注入身份信息。生产环境不允许浏览器直接伪造这些头。

未接入网关的本地或测试环境，可显式设置以下配置以显示页面顶部的模拟身份入口：

```properties
strong.document-governance.test-login.enabled=true
```

模拟身份仅保存在浏览器 `localStorage`，用于自动附加上述身份头；生产环境必须保持默认值 `false`。

## 测试

运行文档治理身份相关测试时，需要连同 Maven 模块依赖一起构建：

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am -DforkCount=0 -DfailIfNoTests=false -Dtest=DocumentGovernanceTestLoginControllerTest,DocumentIdentityInterceptorTest test
```

## 安全与数据留存

- AI 密钥、文档正文和完整提示词不得写入仓库、应用日志或指标系统。
- 文档按团队隔离；成员和管理员权限由网关角色决定。
- 未发布且连续 30 天未更新的草稿会定时清理；已发布版本长期保留。
