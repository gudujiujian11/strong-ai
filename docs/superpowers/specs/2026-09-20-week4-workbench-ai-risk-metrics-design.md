# 第四周技术设计：工作台整合、AI 风险提示、操作与用量指标

> 关联：[技术文档治理台产品设计](./2026-09-18-technical-document-governance-design.md) 第 5 节交付排期第 4 周。
> 验收目标：优化、检查、保存和发布形成单一流程。
> 前提约束（产品设计第 3、4 节）：AI 只提供优化建议和风险提示，不产出质量结论；AI 风险检查不阻塞保存；模型密钥、完整提示词及正文不得写入应用日志或指标系统；当前阶段不启用 Redis，任务状态与幂等键以 MySQL 为准。

## 1. 现状盘点

前三周（尤其第一周落库）已把第四周的多数骨架提前建好，以下为已具备项（位置以 `strong-ai-webapp` 源码为准）：

| 能力 | 现状 | 位置 |
| --- | --- | --- |
| 异步 AI 优化任务 | `submit()` 幂等提交、`process()` 后台执行、失败可重试、24h TTL 清理 | `service/DocumentAiTaskService.java` |
| 任务状态查询 / SSE | `GET /ai-tasks/{id}`、`GET /ai-tasks/{id}/events`（SSE 推送状态） | `controller/DocumentController.java` |
| AI 结果写回草稿 | 成功后 `applyAiDraft()` 写入 `draftContent`，不创建半成品版本 | `DocumentAiTaskService.process` → `DocumentService.applyAiDraft` |
| 用量指标采集 | `recordAiTask()` 记 Token、时延、结果计数；`recordQualityCheck()` 记规则命中，落 Micrometer | `service/DocumentMetricsService.java` |
| 操作日志写入 | 保存 / 发布 / 回滚 / 归档 / AI 提交 / AI 重试 / AI 失败 / 质量检查全部落 `dg_operation_log` | `DocumentService`、`DocumentAiTaskService`、`QualityReportService` |
| 确定性质量检查 | `checkAndSave()` 落报告 + 问题 + 指标 + 操作日志 | `service/QualityReportService.java` |
| 前端主链路 | 优化（SSE 订阅）→ 写回草稿 → 质量检查 → 发布已初步串联 | `static/document-governance.js` |
| 数据表 | `dg_ai_task`、`dg_operation_log` 已在 `V1` 建好 | `db/migration/V1__document_governance.sql` |

## 2. 缺口分析

对照第四周验收目标「优化、检查、保存和发布形成单一流程」，以及产品设计第 3 节实体（AI 风险提示、操作记录、基础看板），仍有 4 处缺口：

1. **AI 风险提示完全未落地，且现有 `warnings` 字段不可复用（硬缺口）**。`MarkdownOptimizeResponse` 虽有 `warnings` 字段，但经核实其来源是 `MarkdownDocumentValidator` 的**前置格式校验**（内容长度上限、`.md` 后缀、代码块围栏闭合），且校验不通过时 `optimize()` 会**直接抛 `ParamValiateException`**、走不到构造响应；校验通过时 `warnings` 为空列表。因此**优化成功路径上 `warnings` 恒为空串**，无法作为风险提示数据源。同时 `MarkdownOptimizationResultParser` 只从模型输出解析 `optimizedMarkdown` 与 `changeSummary`，不提取任何风险信息，`AiTaskEntity`/`AiTaskResponse` 也无承载字段。产品设计明确的「AI 提供优化建议和风险提示」中**风险提示这一半在整条链路上都不存在数据源**，这是第四周的核心缺口。

2. **看板无查询接口**。`DocumentMetricsService` 只把用量写进 Micrometer registry（供 Prometheus 抓取），没有任何面向团队的业务查询接口。产品设计「基础看板：文档数量、检查通过率、常见问题、AI 用量」缺少数据出口，前端无法渲染看板。Micrometer 计数是全局维度、不按团队隔离，也不适合直接作为看板数据源。

3. **操作记录只写不读**。`OperationLogMapper` 是空 Mapper，没有查询方法，也没有任何 Controller 入口。「操作记录」写了一路，却无法回看，验收「操作……指标」无法体现。

4. **工作台未形成闭环单一流程**。前端 `handleTaskEvent` 在任务成功后只 toast「AI 优化已写入草稿」，忽略了 `resultSummary`（修改摘要）与即将新增的风险提示；优化成功后不会自动衔接质量检查；也没有看板视图与操作记录时间线。四个动作（优化、检查、保存、发布）目前是分散按钮，未按验收目标收敛为一条可见的流水线。

## 3. 设计方案

设计取舍：第四周不新增业务能力，而是**把已有的异步任务、指标、日志三条暗线接出到用户可见的单一工作台**，并补齐产品设计点名却缺失的 AI 风险提示。严格遵守「AI 风险提示不阻塞保存、不参与质量评分」——风险提示只作为 AI 任务的附属产物展示，质量结论仍只来自确定性规则。

### 3.1 AI 风险提示落地（缺口 1，核心）

**取舍**：现有 `warnings` 字段来自前置格式校验、成功路径恒为空，不可复用。风险提示必须来自模型输出本身。两条路径对比：

- 方案 A（采用）：**复用同一次优化模型调用**，扩展 prompt 让模型在输出优化结果的同时附带一段风险提示，`MarkdownOptimizationResultParser` 解析出 `riskNote`。不增加调用次数与超时面。
- 方案 B（不采用）：独立一次"AI 风险检查"模型调用。语义更聚焦，但多一次调用、多一份超时/失败/重试处理，与异步任务耦合更复杂，首期不必要。

按方案 A 落地：

- **生成**：在优化 prompt 中要求模型输出结构化的「优化结果 + 修改摘要 + 风险提示」三段；`MarkdownOptimizationResultParser.ParsedResult` 增 `riskNote` 字段（模型未产出时为空串，不报错）。
- **传递**：`MarkdownOptimizeResponse` 复用/新增 `riskNote` 字段承载解析结果（现有 `warnings` 保留给格式校验语义，不混用）。
- **捕获**：`DocumentAiTaskService.process()` 成功分支读取 `optimized.getRiskNote()`，写入任务字段。
- **存储**：
  - `dg_ai_task` 新增 `risk_note VARCHAR(1000) NOT NULL DEFAULT ''`，需 Flyway 迁移 `V3__ai_task_risk_note.sql`（`ADD COLUMN` 带默认值，向后兼容）。
  - `AiTaskEntity` 增 `riskNote`；`reset()` 时清空，避免重试残留旧提示。
- **输出**：`AiTaskResponse` 增 `riskNote`，随 `GET /ai-tasks/{id}` 与 SSE 状态帧一并返回。
- **不阻塞、不评分**：风险提示仅随任务返回，既不写入 `dg_quality_report/issue`，也不进入评分，符合「结构与术语等质量结论必须来自可解释的规则」的边界。
- **数据安全**：`riskNote` 是模型输出的**提示摘要**，不含完整正文与提示词；不得写入应用日志（`operation()` 的 metadata 保持 `{}`，不落风险原文）。

### 3.2 看板查询接口（缺口 2）

- 新增 `DocumentDashboardService`，**从业务表按团队聚合**（不依赖 Micrometer，保证团队隔离与可回溯）：
  - 文档数量：按 `status`（DRAFT/PUBLISHED/ARCHIVED）分组计数。
  - 检查通过率：最近报告 `score >= 阈值` 的文档占比（阈值取规则集或默认 80）。
  - 常见问题 Top N：`dg_quality_issue` 按 `rule_code` 聚合计数，取前 N。
  - AI 用量：`dg_ai_task` 的总 Token、任务数、成功/失败数（可选时间窗）。
- Mapper 补聚合查询：`DocumentMapper.countByStatus`、`QualityIssueMapper.topRuleCodes`、`AiTaskMapper.usageSummary`、`QualityReportMapper.passRate`，全部带 `team_id` 条件。
- 接口：`GET /api/document-governance/dashboard`，返回 `DashboardResponse`（成员可读本团队）。
- 聚合为只读统计，首期不做缓存；数据量小，按需实时查询。

### 3.3 操作记录查询（缺口 3）

- `OperationLogMapper` 补 `selectByTeam(teamId, limit)` 与 `selectByDocument(teamId, documentId, limit)`，按 `created_at` 倒序。
- `DocumentOperationLogService` 增查询方法，组装轻量 `OperationLogResponse`（action、actor、documentId、createdAt、metadata）。
- 接口：
  - `GET /api/document-governance/documents/{documentId}/operations`：单文档操作时间线。
  - `GET /api/document-governance/operations?limit=N`：团队最近操作（看板辅助）。
- 均经团队校验，`metadata_json` 已是脱敏元数据，直接透出。

### 3.4 工作台单一流程整合（缺口 4）

后端接口就绪后，前端把分散动作收敛为一条可见流水线：

```text
编辑草稿
  -> [AI 优化] 提交任务、SSE 进度、写回草稿、展示修改摘要 + 风险提示
  -> [质量检查] 展示得分与问题（确定性）
  -> [保存草稿 / 发布版本]
  -> 看板与操作时间线可回看
```

- **风险提示展示**：`handleTaskEvent` 解析 `resultSummary` 与 `riskNote`，在优化结果区展示「修改摘要」和「AI 风险提示（仅供参考，不阻塞保存）」。
- **流程衔接**：AI 优化成功写回草稿后，提示用户「可运行质量检查」；不强制自动触发，避免额外模型/规则开销与误判。
- **看板视图**：新增看板 Tab，调 `GET /dashboard` 渲染文档数量、通过率、常见问题 Top N、AI 用量。
- **操作时间线**：文档详情侧栏调 `GET /documents/{id}/operations` 展示保存/发布/回滚/优化等记录。

## 4. 改动清单

| 类型 | 文件 | 动作 |
| --- | --- | --- |
| 迁移 | `V3__ai_task_risk_note.sql`（新） | `dg_ai_task` 增 `risk_note` 列 |
| Prompt | 优化 prompt（`MarkdownOptimizerService` 内构造处） | 要求模型附带输出风险提示段 |
| Service | `MarkdownOptimizationResultParser` | 解析 `riskNote`（缺省空串） |
| Model | `MarkdownOptimizeResponse`、`ParsedResult` | 增 `riskNote` |
| Entity | `AiTaskEntity` | 增 `riskNote` |
| Model | `AiTaskResponse` | 增 `riskNote` |
| Model | `DashboardResponse`、`OperationLogResponse`（新） | 看板与操作记录响应体 |
| Service | `DocumentAiTaskService` | `process()` 捕获 `warnings` → `riskNote`；`reset()` 清空；`toResponse` 透出 |
| Service | `DocumentDashboardService`（新） | 按团队聚合文档数量、通过率、常见问题、AI 用量 |
| Service | `DocumentOperationLogService` | 增操作记录查询与响应组装 |
| Mapper | `OperationLogMapper` | 补 `selectByTeam`、`selectByDocument` |
| Mapper | `DocumentMapper`、`QualityIssueMapper`、`QualityReportMapper`、`AiTaskMapper` | 补看板聚合查询（带 team_id） |
| Controller | `DocumentGovernanceController` / `DocumentController` | 新增看板、操作记录查询接口 |
| 前端 | `static/document-governance.{html,css,js}` | 风险提示展示、流程衔接、看板 Tab、操作时间线 |
| 测试 | `DocumentAiTaskServiceTest` 增补 | 风险提示捕获、重试清空、SSE 透出 |
| 测试 | `DocumentDashboardServiceTest`（新） | 聚合正确性、团队隔离 |
| 测试 | `OperationLog` 查询相关增补 | 时间线倒序、团队隔离 |

`dg_operation_log` 已在 `V1` 建好，本周只接查询代码；仅 AI 风险提示需要新迁移 `V3`。

## 5. 验收对照

| 验收项 | 落点 | 状态 |
| --- | --- | --- |
| 优化流程可见 | 已有异步任务 + SSE + 3.4 摘要/进度展示 | 补齐 |
| AI 风险提示 | 3.1 捕获 warnings → riskNote → 前端展示 | 新增 |
| 检查流程 | 已有确定性质量检查 + 报告回看 | 保持 |
| 保存 / 发布 | 已有 | 保持 |
| 单一流程 | 3.4 前端收敛为流水线 + 看板 + 操作时间线 | 补齐 |
| 操作与用量指标 | 3.2 看板接口 + 3.3 操作记录查询 | 新增 |

## 6. 风险与注意

- **数据脱敏**：`riskNote` / `changeSummary` 属模型输出摘要，展示可以，但**不得写入操作日志或指标标签**，避免正文与提示词泄漏进日志/指标系统（产品设计第 3 节硬约束）。
- **模型输出可靠性**：风险提示依赖模型稳定按格式输出三段；parser 必须容错——模型未产出风险段时 `riskNote` 置空、绝不报错或影响优化结果写回。上线前需用真实文档评估提示词产出风险提示的质量与稳定性（呼应产品设计第 8 节）。
- **风险提示与质量结论边界**：AI 风险提示是 advisory，绝不写入质量报告、不参与评分、不阻塞保存；质量结论只来自确定性规则，防止 AI 输出污染可解释性。
- **看板数据源选择**：看板走业务表聚合而非 Micrometer，保证按团队隔离、可回溯、口径稳定；Micrometer 指标继续供运维监控（Prometheus）用，两者不混用。
- **聚合性能**：`dg_quality_issue` 按 `rule_code` 聚合、`dg_ai_task` 用量汇总，首期数据量小可实时查询；若后续膨胀，再评估物化统计表或缓存。
- **迁移兼容**：`V3` 仅 `ADD COLUMN` 带默认值，对存量任务无破坏；上线前确认 Flyway 版本号不与并行分支冲突。
- **重试残留**：`reset()` 必须清空 `riskNote`，否则失败重试后可能沿用上次的风险提示，产生误导。
