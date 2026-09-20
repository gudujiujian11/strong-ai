# 技术文档治理台操作手册

本文用于测试和验收 `http://localhost:11028/document-governance.html`。示例围绕一份“订单服务 API 文档”展开，覆盖模板、术语库、规则集、文档检查、版本和质量报告。

## 1. 测试准备

### 1.1 启动应用

先按项目 README 配置 MySQL、Nacos 和至少一个 AI 平台，再启动应用：

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am clean package
java -jar strong-ai-webapp/target/strong-ai.jar
```

打开 `http://localhost:11028/document-governance.html`。

### 1.2 配置本地模拟身份

本地测试没有网关注入身份时，在测试配置中打开：

```properties
strong.document-governance.test-login.enabled=true
```

重启后，页面右上角会出现“测试身份”。先用管理员身份配置规范：

| 字段 | 示例值 |
| --- | --- |
| 用户 ID | `qa-admin` |
| 团队 ID | `team-order` |
| 角色 | `管理员` |

模板、术语库、规则集写入后，再切换为成员身份验证普通编辑流程：

| 字段 | 示例值 |
| --- | --- |
| 用户 ID | `qa-member` |
| 团队 ID | `team-order` |
| 角色 | `成员` |

模拟身份只保存在浏览器 `localStorage`，不要在生产环境开启。真实环境应由网关注入以下请求头，浏览器不能自行伪造：

```text
X-Strong-User-Id: qa-member
X-Strong-Team-Id: team-order
X-Strong-Role: member
```

## 2. 模板怎么用

模板定义某种文档类型必须包含的章节。检查时，系统读取当前团队中匹配 `documentType` 的模板；每行一个必填章节。标题允许忽略大小写、空白，并允许以模板章节名开头，例如模板要求“安装”，文档标题“安装说明”也算满足。

### 2.1 页面操作案例：创建 README 模板

1. 使用管理员身份打开右侧“术语库”页签。
2. 点击底部“模板管理”。
3. 填写并保存：

| 字段 | 示例值 |
| --- | --- |
| 模板名称 | `订单服务 README` |
| 适用文档类型 | `README` |
| 必填章节 | 见下方，每行一项 |

```text
概述
安装
配置
使用说明
接口说明
故障排查
```

保存后，模板列表应出现“订单服务 README / README”。成员可以读取模板，但不能保存模板。

### 2.2 API 等价操作

```bash
curl -X PUT 'http://localhost:11028/api/document-governance/templates' \
  -H 'Content-Type: application/json' \
  -H 'X-Strong-User-Id: qa-admin' \
  -H 'X-Strong-Team-Id: team-order' \
  -H 'X-Strong-Role: admin' \
  -d '{
    "name": "订单服务 README",
    "documentType": "README",
    "requiredHeadings": "概述\n安装\n配置\n使用说明\n接口说明\n故障排查"
  }'
```

查询当前团队模板：

```bash
curl 'http://localhost:11028/api/document-governance/templates' \
  -H 'X-Strong-User-Id: qa-member' \
  -H 'X-Strong-Team-Id: team-order' \
  -H 'X-Strong-Role: member'
```

### 2.3 验证结果

新建类型为 `README` 的文档，只写入 `# 概述` 和 `# 安装说明`，运行质量检查，应看到 `REQUIRED_HEADING` 问题：

```text
缺少必填章节：配置
缺少必填章节：使用说明
缺少必填章节：接口说明
缺少必填章节：故障排查
```

补齐章节后重新检查，这些问题应消失。模板本身不直接生成正文，只参与必填章节检查。

## 3. 术语库怎么用

术语库用于团队统一写法，有两种规则：

- 勾选“禁用术语”：命中后生成 `FORBIDDEN_TERM`，属于需要修复的问题；可填写推荐写法。
- 不勾选“禁用术语”，只填写“推荐写法”：命中后生成 `PREFERRED_TERM`，属于低严重度建议。

纯 ASCII 术语按单词边界匹配，例如 `SDK` 不会误命中 `OldSDK`；中文术语按子串匹配。代码围栏内的标题和术语不检查。

### 3.1 页面操作案例：统一订单服务术语

使用管理员身份，在“术语库”页签点击加号，依次新增：

| 术语 | 推荐写法 | 禁用 | 说明 |
| --- | --- | --- | --- |
| `接口` | `API` | 是 | 对外技术文档统一使用 API |
| `订单中心` | `Order Center` | 否 | 产品英文名 |
| `SDK` | `SDK` | 否 | 检查单词边界 |

保存后，右侧列表应显示禁用标记或推荐写法。成员可以查看术语，不能新增或修改。

### 3.2 API 等价操作

```bash
curl -X PUT 'http://localhost:11028/api/document-governance/terms' \
  -H 'Content-Type: application/json' \
  -H 'X-Strong-User-Id: qa-admin' \
  -H 'X-Strong-Team-Id: team-order' \
  -H 'X-Strong-Role: admin' \
  -d '{
    "term": "接口",
    "preferredTerm": "API",
    "forbidden": true,
    "description": "对外技术文档统一使用 API"
  }'

curl 'http://localhost:11028/api/document-governance/terms' \
  -H 'X-Strong-User-Id: qa-member' \
  -H 'X-Strong-Team-Id: team-order' \
  -H 'X-Strong-Role: member'
```

### 3.3 验证结果

文档中写入：

````markdown
# 接口说明

订单服务接口由订单中心提供。

```java
// 接口 仅在代码示例中，不参与术语检查
```
````

检查结果应至少包含一条：

```text
规则：FORBIDDEN_TERM
严重级别：MEDIUM
行号：3
问题：检测到禁用术语：接口
建议：建议使用：API
```

“订单中心”会生成 `PREFERRED_TERM / LOW` 建议；代码块中的“接口”不会生成问题。将 `接口` 改为 `API` 后重新检查，相关问题消失。

## 4. 规则集怎么用

规则集控制检查项是否启用、严重级别和评分扣分权重。没有规则集时使用默认配置：`HIGH=30`、`MEDIUM=15`、`LOW=5`，所有规则默认启用。规则集 JSON 配置错误时系统回退默认配置，不会阻断检查。

### 4.1 页面操作案例：订单服务发布规则

使用管理员身份，在“术语库”页签点击“规则管理”，填写：

| 字段 | 示例值 |
| --- | --- |
| 规则集名称 | `订单服务发布规则` |
| 启用 | 勾选 |
| 规则配置 JSON | 见下方 |

```json
{
  "rules": {
    "HEADING_ORDER": { "enabled": true, "severity": "LOW" },
    "UNSAFE_LINK": { "enabled": true, "severity": "HIGH" },
    "UNCLOSED_FENCE": { "enabled": true, "severity": "HIGH" },
    "FORBIDDEN_TERM": { "enabled": true, "severity": "HIGH" },
    "PREFERRED_TERM": { "enabled": true, "severity": "LOW" },
    "REQUIRED_HEADING": { "enabled": true, "severity": "MEDIUM" },
    "EMPTY_DOCUMENT": { "enabled": true, "severity": "HIGH" }
  },
  "scoreWeights": { "HIGH": 40, "MEDIUM": 20, "LOW": 5 }
}
```

这个案例把禁用术语从默认的 `MEDIUM / 15 分` 调整为 `HIGH / 40 分`。保存后重新检查包含“接口”的文档，报告中的严重级别应变为 `HIGH`，分数下降幅度也应按 40 分计算。

### 4.2 API 等价操作

```bash
curl -X PUT 'http://localhost:11028/api/document-governance/rule-sets' \
  -H 'Content-Type: application/json' \
  -H 'X-Strong-User-Id: qa-admin' \
  -H 'X-Strong-Team-Id: team-order' \
  -H 'X-Strong-Role: admin' \
  -d '{
    "name": "订单服务发布规则",
    "enabled": true,
    "configJson": "{\"rules\":{\"FORBIDDEN_TERM\":{\"enabled\":true,\"severity\":\"HIGH\"}},\"scoreWeights\":{\"HIGH\":40,\"MEDIUM\":20,\"LOW\":5}}"
  }'
```

查询规则集：

```bash
curl 'http://localhost:11028/api/document-governance/rule-sets' \
  -H 'X-Strong-User-Id: qa-admin' \
  -H 'X-Strong-Team-Id: team-order' \
  -H 'X-Strong-Role: admin'
```

### 4.3 规则关闭案例

将 `UNSAFE_LINK` 改为 `"enabled": false`，保存后检查包含 `[登录](javascript:alert(1))` 的文档。该链接不再产生 `UNSAFE_LINK`，但其他启用的规则仍会执行。建议测试结束后恢复为启用，避免影响后续案例。

## 5. 完整文档测试案例

### 5.1 新建并保存草稿

切换成员身份，点击左侧“新建文档”，填写：

| 字段 | 示例值 |
| --- | --- |
| 标题 | `订单服务 API 文档` |
| 文档类型 | `README` |
| 标签 | `订单,API,发布验收` |

将下面内容粘贴到编辑器：

```markdown
# 订单服务 API 文档

## 概述
订单服务负责创建和查询订单。

## 安装
使用 Java 8 启动服务。

## 接口说明
订单中心提供订单接口。

## 使用说明
调用接口前需要完成鉴权。

## 配置
服务端口默认为 8080。

## 故障排查
查看应用日志和监控指标。
```

点击保存草稿。成员只能保存自己创建的草稿；管理员可以保存团队内文档。已发布版本不可直接修改，修改后会显示“有未发布改动”。

### 5.2 AI 优化（可选）

选择可用模型，点击“AI 优化”。该操作会创建异步任务，页面会轮询任务状态；成功后结果写入草稿，失败时原内容保留，可重试。AI 只提供优化建议，不能替代规则检查，也不会自动发布。

### 5.3 运行质量检查

点击“质量检查”。系统按当前团队的模板、术语库和规则集同步检查，并生成一份质量报告。报告中的每条问题都有规则编码、严重级别、行号、问题描述和建议。修复内容后再次点击“质量检查”，系统会生成新的报告，不覆盖历史报告。

预期通过条件：

- 必填章节全部存在；
- 没有 `HIGH` 或 `MEDIUM` 问题；
- 术语已按团队推荐写法统一；
- 分数达到团队约定阈值（示例可用 `100`）。

### 5.4 发布、比较和回滚

1. 检查通过后点击“发布”，可在 API 中通过 `releaseNote` 记录发布说明。
2. 在“版本历史”中查看版本摘要；已发布版本不可修改。
3. 选中两个版本比较，页面调用动态 diff；也可以选中一个版本后点击“对比草稿”。
4. 选中历史版本点击“回滚到此版本”。回滚不会修改历史版本，而是复制内容创建一个新版本。
5. 不再使用的文档点击“归档”；归档后不能编辑或发布，恢复后才能继续操作。

API 示例：

```bash
# 发布当前草稿
curl -X POST 'http://localhost:11028/api/document-governance/documents/1/publish?releaseNote=订单服务%20v1.0' \
  -H 'X-Strong-User-Id: qa-member' -H 'X-Strong-Team-Id: team-order' -H 'X-Strong-Role: member'

# 比较版本 1 与版本 2
curl 'http://localhost:11028/api/document-governance/documents/1/diff?from=1&to=2' \
  -H 'X-Strong-User-Id: qa-member' -H 'X-Strong-Team-Id: team-order' -H 'X-Strong-Role: member'

# 将版本 1 回滚为新版本
curl -X POST 'http://localhost:11028/api/document-governance/documents/1/rollback/1?releaseNote=恢复稳定版本' \
  -H 'X-Strong-User-Id: qa-member' -H 'X-Strong-Team-Id: team-order' -H 'X-Strong-Role: member'
```

## 6. 质量报告如何输出

### 6.1 页面输出

质量检查完成后，右侧“质量检查”页签直接展示：

- 总分；
- 问题总数；
- 每条问题的严重级别、规则编码、行号、描述和修复建议。

“版本历史”页签展示版本和 diff；质量报告不是发布版本正文，修复后应重新检查并保留历史报告记录。

### 6.2 接口 JSON 输出

查询最近一次报告（含问题明细）：

```bash
curl 'http://localhost:11028/api/document-governance/documents/1/quality-report/latest' \
  -H 'X-Strong-User-Id: qa-member' \
  -H 'X-Strong-Team-Id: team-order' \
  -H 'X-Strong-Role: member' > quality-report-latest.json
```

查询历史报告摘要：

```bash
curl 'http://localhost:11028/api/document-governance/documents/1/quality-reports' \
  -H 'X-Strong-User-Id: qa-member' \
  -H 'X-Strong-Team-Id: team-order' \
  -H 'X-Strong-Role: member' > quality-reports.json
```

最近报告的 `data` 结构示例：

```json
{
  "reportId": 18,
  "documentId": 1,
  "documentVersionId": null,
  "score": 75,
  "createdAt": 1780000000000,
  "issues": [
    {
      "ruleCode": "FORBIDDEN_TERM",
      "severity": "HIGH",
      "lineNumber": 9,
      "message": "检测到禁用术语：接口",
      "suggestion": "建议使用：API"
    }
  ]
}
```

说明：草稿检查时 `documentVersionId` 可能为空；发布版本检查会关联版本 ID。接口返回外层沿用项目 `Result`，实际业务内容在 `data` 字段中。

### 6.3 输出为测试摘要 Markdown

项目当前没有单独的“导出报告”按钮，可以用 `jq` 把 JSON 摘要成测试附件：

```bash
jq -r '
  .data as $r |
  "# 质量检查报告\n\n" +
  "- 文档 ID：" + ($r.documentId|tostring) + "\n" +
  "- 报告 ID：" + ($r.reportId|tostring) + "\n" +
  "- 得分：" + ($r.score|tostring) + "\n" +
  "- 问题数：" + (($r.issues|length)|tostring) + "\n\n" +
  "## 问题明细\n" +
  (if ($r.issues|length) == 0 then "\n无问题。\n" else
    ($r.issues[] | "\n- [" + .severity + "] " + .ruleCode + "，第" + (.lineNumber|tostring) + "行：" + .message + "；建议：" + .suggestion)
  end)
' quality-report-latest.json > quality-report-latest.md
```

验收时建议同时提交 `quality-report-latest.json` 和 `quality-report-latest.md`：JSON 用于机器核验，Markdown 用于测试报告或缺陷单阅读。

## 7. 常见测试点

| 测试点 | 预期 |
| --- | --- |
| 成员保存其他成员草稿 | 失败，原文不丢失 |
| 成员维护模板、术语、规则集 | 失败，仅管理员允许 |
| 不同 `teamId` 查询文档或规范 | 查不到其他团队数据 |
| 代码块中出现禁用术语 | 不产生术语问题 |
| 缺少闭合代码围栏 | 产生 `UNCLOSED_FENCE / HIGH` |
| 标题从 H1 直接跳到 H3 | 产生 `HEADING_ORDER` |
| 使用 `javascript:` 或 `data:` 链接 | 产生 `UNSAFE_LINK / HIGH` |
| AI 任务失败 | 草稿保留，可重试，不创建半成品版本 |
| 回滚历史版本 | 生成新版本，历史版本内容不变 |
| 连续 30 天未更新的草稿 | 按清理任务删除；已发布版本长期保留 |

## 8. API 速查

| 能力 | 方法 | 路径 |
| --- | --- | --- |
| 当前身份 | GET | `/api/document-governance/identity` |
| 文档列表/详情 | GET | `/api/document-governance/documents`、`/documents/{id}` |
| 新建文档 | POST | `/api/document-governance/documents` |
| 保存草稿 | PUT | `/api/document-governance/documents/{id}/draft` |
| 质量检查 | POST | `/api/document-governance/documents/{id}/quality-check` |
| 最近报告 | GET | `/api/document-governance/documents/{id}/quality-report/latest` |
| 历史报告 | GET | `/api/document-governance/documents/{id}/quality-reports` |
| 模板 | GET/PUT | `/api/document-governance/templates` |
| 术语库 | GET/PUT | `/api/document-governance/terms` |
| 规则集 | GET/PUT | `/api/document-governance/rule-sets` |
| 发布/回滚 | POST | `/documents/{id}/publish`、`/documents/{id}/rollback/{versionNo}` |
| 任意版本 diff | GET | `/documents/{id}/diff?from={versionNo}&to={versionNo\|draft}` |
