# 第三周技术设计：模板、术语库、确定性质量规则与报告

> 关联：[技术文档治理台产品设计](./2026-09-18-technical-document-governance-design.md) 第 5 节交付排期第 3 周。
> 验收目标：可检查章节缺失、标题层级、链接、代码块和术语违规。
> 前提约束（产品设计第 2、4 节）：结构与术语等质量结论必须来自可解释的确定性规则；AI 风险提示不阻塞保存；规则集与阈值以 MySQL 为准，不引入 Redis。

## 1. 现状盘点

第一周落库时已把第三周的多数骨架建好，以下为已具备项（位置以 `strong-ai-webapp` 源码为准）：

| 能力 | 现状 | 位置 |
| --- | --- | --- |
| 确定性规则引擎 | `check()` 覆盖空文档、标题跳级、代码围栏未闭合、危险链接、禁用术语、必填章节 6 类规则，逐行输出带行号、严重级别、修复建议 | `service/MarkdownQualityChecker.java` |
| 代码块内豁免 | ` ``` ` 围栏内的标题与术语不参与检查 | `MarkdownQualityChecker` `fenced` 开关 |
| 报告落库 | `checkAndSave()` 生成报告、逐条问题入库、扣分算分、记指标与操作日志，全程带 `teamId` | `service/QualityReportService.java` |
| 模板服务 | `requiredHeadings()` 聚合团队+文档类型的必填章节（带 5 分钟缓存）；`upsert()` 管理员维护 | `service/TemplateService.java` |
| 术语服务 | `list()`（带缓存）与 `upsert()`（管理员鉴权、大小写按原文） | `service/TermService.java` |
| 术语接口 | `GET /terms`、`PUT /terms` | `controller/DocumentController.java` |
| 质量检查接口 | `POST /documents/{id}/quality-check` | `controller/DocumentGovernanceController.java` |
| 数据表 | `dg_template`、`dg_term`、`dg_rule_set`、`dg_quality_report`、`dg_quality_issue` 均已在 `V1` 建好 | `db/migration/V1__document_governance.sql` |
| 团队隔离 | 模板、术语、报告全链路查询携带 `team_id` | 全链路 |

## 2. 缺口分析

对照验收标准与产品设计第 3 节实体表（模板 / 术语库 / 规则集 / 质量报告），仍有 6 处缺口：

1. **规则集完全未接入**（硬缺口）。`dg_rule_set` 表已建，但无 Entity / Mapper / Service，规则的启用开关与阈值全部硬编码在 `MarkdownQualityChecker` 与 `QualityReportService`（扣分 30/15/5 写死）。产品设计明确「规则集：结构规则、模板必填项、术语规则与阈值，团队共享，管理员配置」，当前不可配。
2. **模板无对外接口**。`TemplateService` 只有 Service，没有任何 Controller 入口，管理员无法通过 API 创建/查询/维护模板；验收「章节缺失检查」依赖模板数据，却无从录入。
3. **质量报告不可回看**。`checkAndSave` 只能新建报告，没有「查询某文档最近报告 / 历史报告 / 报告详情」的接口。产品设计要求「质量报告随版本生成，可重新检查」，前端无法展示上次结果。
4. **术语匹配过糙**。`line.contains(term.getTerm())` 是大小写敏感的裸子串匹配：`OldSDK` 会命中 `SDK`（误报），`oldsdk` 不会命中 `OldSDK`（漏报）；且只处理 `forbidden=true` 的术语，`preferredTerm` 存在但 `forbidden=false` 的「建议替换」规则未实现。
5. **必填章节匹配只认精确文本**。`headings.contains(required)` 要求标题文字完全相等，忽略前后空白/层级差异之外无归一化；模板配置「安装」而文档写「安装说明」不会命中，规则实际可用性弱。
6. **规则严重级别与阈值不可调**。评分扣分权重、各规则的开/关、必填章节属于哪个模板，均无配置面；验收指标「高严重度问题修复率」依赖严重级别定义稳定，需要可解释、可配置的来源。

## 3. 设计方案

设计取舍：本周核心是把「可解释、可配置的确定性规则」补完整，而非扩展新规则类型。优先补 **规则集接入（缺口 1/6）** 和 **模板/报告接口（缺口 2/3）**，术语与章节匹配（缺口 4/5）做低风险修正，不做分词或语义匹配。

### 3.1 规则集接入（缺口 1/6，核心）

- 新增 `RuleSetEntity`（映射既有 `dg_rule_set`）、`RuleSetMapper`、`RuleSetService`。
- `config_json` 存一份确定性配置，结构如下（缺省即回退到当前硬编码值，保证向后兼容）：

```json
{
  "rules": {
    "HEADING_ORDER":    { "enabled": true, "severity": "LOW" },
    "UNSAFE_LINK":      { "enabled": true, "severity": "HIGH" },
    "UNCLOSED_FENCE":   { "enabled": true, "severity": "HIGH" },
    "FORBIDDEN_TERM":   { "enabled": true, "severity": "MEDIUM" },
    "PREFERRED_TERM":   { "enabled": true, "severity": "LOW" },
    "REQUIRED_HEADING": { "enabled": true, "severity": "MEDIUM" },
    "EMPTY_DOCUMENT":   { "enabled": true, "severity": "HIGH" }
  },
  "scoreWeights": { "HIGH": 30, "MEDIUM": 15, "LOW": 5 }
}
```

- `RuleSetService.effectiveConfig(teamId)`：取团队 `enabled=1` 的规则集，反序列化为 `RuleConfig` 值对象；无配置时返回内置默认，5 分钟缓存（与模板/术语一致）。
- `MarkdownQualityChecker.check(...)` 增加 `RuleConfig` 参数：按 `enabled` 决定是否产出某规则问题，按 `severity` 覆盖默认级别。评分权重从 `RuleConfig.scoreWeights` 取。
- 管理员接口：`GET /rule-sets`、`PUT /rule-sets`（body 校验 JSON 结构合法，非法抛 `ParamValiateException`），复用 `requireAdmin()` 模式。
- 配置解析必须容错：字段缺失走默认、非法枚举忽略该条，绝不因规则集损坏导致质量检查整体失败。

### 3.2 模板管理接口（缺口 2）

- 新增 `TemplateController`（或并入 `DocumentController` 的 `/templates` 段），提供：
  - `GET /templates`：列出当前团队模板（成员可读）。
  - `GET /templates?documentType=xxx`：按文档类型过滤。
  - `PUT /templates`：管理员 upsert（复用 `TemplateService.upsert`）。
- `TemplateEntity` 直接作为请求/响应体（与术语 `PUT /terms` 现状保持一致，本周不额外引入 DTO，降低改动面）。
- `TemplateMapper` 补 `selectByTeamId(teamId)` 用于列表。

### 3.3 质量报告查询（缺口 3）

- `QualityReportMapper` 补：
  - `selectLatestByDocument(teamId, documentId)`：取最近一份报告。
  - `selectByDocument(teamId, documentId)`：报告列表（不含 issues，轻量）。
- `QualityIssueMapper` 补 `selectByReportId(teamId, reportId)`。
- `QualityReportService` 增查询方法，组装 `QualityCheckResponse`（含 issues）。
- 接口：
  - `GET /documents/{id}/quality-report/latest`：最近一次报告（前端进入文档默认展示）。
  - `GET /documents/{id}/quality-reports`：历史报告摘要。
- 全部经 `documentService.requireReadable(documentId)` 做团队+可读校验。

### 3.4 术语匹配修正（缺口 4）

- 大小写不敏感匹配：预编译 `Pattern.compile(Pattern.quote(term), CASE_INSENSITIVE)`，避免裸 `contains`。
- 词边界防误命中：对纯 ASCII 术语加 `\b` 词边界（`\bSDK\b` 不再命中 `OldSDK`）；含 CJK 的术语无可靠词边界，维持子串匹配并在文档中注明该限制。
- 新增 `PREFERRED_TERM` 规则：`forbidden=false` 且 `preferredTerm` 非空且与 `term` 不同时，命中 `term` 给出 LOW 级「建议替换为 preferredTerm」提示（非阻断）。
- 术语数量大时逐行逐词 O(lines×terms) 需注意：首期技术文档与团队术语规模可控，暂不做 Aho-Corasick，仅在文档中记风险。

### 3.5 必填章节匹配放宽（缺口 5）

- 匹配改为「归一化后包含」：去空白 + 大小写不敏感，且允许文档标题以模板章节名开头（模板「安装」命中文档「安装说明」）。
- 归一化规则集中到一个私有方法，模板侧与文档标题侧共用，保证对称。
- 保守策略：只放宽「前缀/大小写/空白」，不做同义词，避免把不相关章节误判为命中。

### 3.6 检查主链路串联

`QualityReportService.checkAndSave` 调整为：

```text
requireReadable
  -> ruleSetService.effectiveConfig(teamId)
  -> templateService.requiredHeadings(teamId, documentType)
  -> termService.list(teamId)
  -> checker.check(content, requiredHeadings, terms, ruleConfig)
  -> 按 ruleConfig.scoreWeights 评分
  -> 落报告 + 问题 + 指标 + 操作日志
```

## 4. 改动清单

| 类型 | 文件 | 动作 |
| --- | --- | --- |
| Entity | `RuleSetEntity`（新） | 映射 `dg_rule_set` |
| Mapper | `RuleSetMapper`（新） | 团队规则集查询 |
| Mapper | `TemplateMapper` | 补 `selectByTeamId` |
| Mapper | `QualityReportMapper`、`QualityIssueMapper` | 补报告/问题查询方法 |
| Model | `RuleConfig`（新，值对象）；`RuleSetRequest`（新） | 规则配置结构与请求体 |
| Service | `RuleSetService`（新） | 规则集读写、缓存、默认回退、JSON 容错解析 |
| Service | `MarkdownQualityChecker` | 增 `RuleConfig` 参数；术语大小写/词边界；`PREFERRED_TERM`；章节归一化匹配 |
| Service | `QualityReportService` | 串接 `RuleConfig`；报告查询方法；权重取配置 |
| Service | `TemplateService` | 暴露 `list(teamId)` |
| Controller | `DocumentGovernanceController` | 新增报告查询接口 |
| Controller | `TemplateController`（新）/ `DocumentController` | 模板 CRUD、规则集读写接口 |
| 测试 | `MarkdownQualityCheckerTest` 增补 | 规则开关、严重级别覆盖、术语大小写/词边界、PREFERRED_TERM、章节前缀匹配、代码块豁免 |
| 测试 | `RuleSetServiceTest`（新） | 默认回退、非法 JSON 容错、管理员鉴权 |
| 测试 | `QualityReportServiceTest`（新/增补） | 权重生效、报告查询、团队隔离 |

无新增数据库迁移：`dg_rule_set` 已在 `V1` 建好，本周只接代码。

## 5. 验收对照

| 验收项 | 落点 | 状态 |
| --- | --- | --- |
| 章节缺失 | 已有 `REQUIRED_HEADING` + 3.2 模板接口 + 3.5 匹配放宽 | 补齐 |
| 标题层级 | 已有 `HEADING_ORDER` | 保持 |
| 链接 | 已有 `UNSAFE_LINK` | 保持 |
| 代码块 | 已有 `UNCLOSED_FENCE` + 围栏豁免 | 保持 |
| 术语违规 | 已有 `FORBIDDEN_TERM` + 3.4 匹配修正 + `PREFERRED_TERM` | 补齐 |
| 规则可解释可配置 | 3.1 规则集接入 | 新增 |
| 报告可回看 | 3.3 报告查询接口 | 新增 |

## 6. 前端配套（static/document-governance.*）

- **质量检查面板**：进入文档调 `GET /quality-report/latest` 展示上次结果；「重新检查」调 `POST /quality-check`。
- **问题列表**：按严重级别（HIGH/MEDIUM/LOW）配色分组，点击定位到 `lineNumber`；展示 `suggestion`。
- **模板管理**（管理员）：文档类型 → 必填章节编辑；成员只读。
- **术语库**：已有查询/维护，补 `preferredTerm` 与 `forbidden` 的区分展示。
- **规则集**（管理员）：规则开关与严重级别、评分权重的表单；非管理员隐藏入口。

## 7. 风险与注意

- **规则集 JSON 容错**：`config_json` 由管理员编辑，损坏或字段缺失时必须回退默认，绝不阻断检查；解析异常记日志但不外抛。
- **术语匹配的语言边界**：`\b` 词边界对 CJK 无效，中文禁用词仍走子串匹配，可能有误命中；本周不引入分词，作为已知限制记录。
- **向后兼容**：无规则集的存量团队走内置默认权重与全规则启用，行为与当前一致，不产生回归。
- **性能**：逐行 × 术语数的匹配对超大文档或超多术语为 O(n×m)，首期规模可控；若后续术语库膨胀，再评估 Aho-Corasick 或缓存编译后的 Pattern 列表。
- **鉴权一致性**：模板、规则集的写操作统一走 `requireAdmin()`；报告、模板、术语的读操作统一走团队校验，避免跨团队泄漏。
