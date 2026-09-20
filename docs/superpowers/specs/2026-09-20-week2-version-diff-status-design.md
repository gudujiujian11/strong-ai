# 第二周技术设计：版本发布、Diff、回滚与文档状态

> 关联：[技术文档治理台产品设计](./2026-09-18-technical-document-governance-design.md) 第 5 节交付排期第 2 周。
> 验收目标：发布版本不可变，可比较任意两版并回滚，文档状态可流转。

## 1. 现状盘点

第一周实现已提前覆盖第二周大部分能力，以下为已具备项（位置以 `strong-ai-webapp` 源码为准）：

| 能力 | 现状 | 位置 |
| --- | --- | --- |
| 发布版本 | `publish()` 生成自增 `version_no`，写入 `dg_document_version`，更新 `current_version_id` 与状态 | `service/DocumentService.java` |
| 版本不可变 | 版本表只 insert 不 update；回滚也新建版本 | 测试 `publishCreatesAnImmutableIncrementingVersion` |
| 回滚 | `rollback()` 从历史版本内容新建版本，草稿同步 | `service/DocumentService.java` |
| 版本历史 | `listVersions()` 按团队 + 文档倒序 | `service/DocumentService.java` |
| Diff（相邻版本） | 发版时生成与上一版本的行级 diff，存 `diff_content` | `unifiedDiff()` |
| 团队隔离 | 全链路查询携带 `team_id` 条件 | 全链路 |
| 状态枚举 | DRAFT / PUBLISHED / ARCHIVED | `model/documentgovernance/DocumentStatus.java` |

## 2. 缺口分析

对照验收标准「发布版本不可变，**可比较任意两版**并回滚」，仍有 4 处缺口：

1. **无法比较任意两版**：diff 仅在发版时相对前一版预生成并存库，缺少「给定 vX 与 vY 动态算 diff」的能力。属验收硬指标。
2. **状态模型不闭环**：发布后 `saveDraft` 改 `draftContent`，但状态仍为 PUBLISHED，导致草稿与发布版悄悄分叉，用户看不到「有未发布改动」。
3. **缺状态流转接口**：`ARCHIVED` 枚举存在但无任何入口，无法归档或恢复。
4. **单版本详情缺失**：`listVersions` 一次返回全部版本全文（MEDIUMTEXT），版本多时响应膨胀；缺少按版本号取单版详情的接口。

## 3. 设计方案

### 3.1 任意两版 Diff（缺口 1，核心）

- 抽出独立 `DocumentDiffService`，与发版解耦。
- 新增接口 `GET /api/document-governance/documents/{documentId}/diff?from={versionNo}&to={versionNo}`。
  - `to` 支持特殊值 `draft`，表示「与当前草稿比较」，用于展示未发布改动。
- Service：`compare(documentId, fromNo, toNo)` → 校验团队归属 → 取两版内容 → 生成结构化 diff。
- **算法升级**：现有逐行位置比对在插入行时会整体错位。改用基于 LCS 的行级 diff，引入 `java-diff-utils`（Apache-2.0，已确认采用）。
- **返回结构化结果，不在后端拼 HTML**（移除 `escapeHtml` 拼接方式，渲染交给前端）：

```json
{
  "fromVersionNo": 3,
  "toVersionNo": 7,
  "hunks": [
    { "type": "EQUAL|INSERT|DELETE", "oldLine": 12, "newLine": 12, "text": "..." }
  ]
}
```

- 发版时存的 `diff_content` 保留作相邻版本快照；对外比较统一走动态接口，保证任意两版可比。

### 3.2 状态闭环（缺口 2）

- **采用标记位方案**：`dg_document` 新增 `has_unpublished_changes TINYINT NOT NULL DEFAULT 0`。
  - `saveDraft` 在文档已发布时置 1；`publish` 置 0。
  - 状态仍为 PUBLISHED，前端据标记位提示「有未发布修改」。
- 需要 Flyway 迁移 `V2__document_status_dirty.sql`（`ALTER TABLE ADD COLUMN`，带默认值，向后兼容）。
- `DocumentEntity` 与 `DocumentResponse` 增加 `hasUnpublishedChanges` 字段。

### 3.3 状态流转接口（缺口 3）

- 新增 `POST /api/document-governance/documents/{documentId}/status`，body `{ "status": "ARCHIVED" }`。
- 允许流转（状态机白名单）：PUBLISHED→ARCHIVED、ARCHIVED→PUBLISHED（恢复）、DRAFT→ARCHIVED。
- 归档文档不可 `saveDraft` / `publish`，需先恢复。
- 仅 owner 或 admin 可操作，记 `OperationLog`（DOCUMENT_ARCHIVED / DOCUMENT_RESTORED）。
- 非法流转抛 `ParamValiateException`。

### 3.4 单版本详情（缺口 4）

- 新增 `GET /api/document-governance/documents/{documentId}/versions/{versionNo}` 返回单版全文。
- `listVersions` 列表响应改为轻量 `DocumentVersionSummaryResponse`（id / versionNo / releaseNote / createdBy / createdAt），去掉 content，全文按需取。

### 3.5 不可变加固（可选）

- 版本表维持「只增不改」；`DocumentVersionMapper` 不暴露 update/delete 业务入口。
- 不引入 MySQL 触发器（维护成本高），靠代码约束 + 测试守住不可变。

## 4. 改动清单

| 类型 | 文件 | 动作 |
| --- | --- | --- |
| 迁移 | `V2__document_status_dirty.sql`（新） | 新增 `has_unpublished_changes` 列 |
| Service | `DocumentDiffService`（新） | 任意两版 / 版本-草稿 LCS diff |
| Service | `DocumentService` | saveDraft 置脏、publish 清脏、状态流转、单版取详情 |
| Controller | `DocumentGovernanceController` | 新增 diff、status、单版详情三个接口 |
| Model | `DiffResponse`、`DocumentStatusChangeRequest`、`DocumentVersionSummaryResponse`（新）；`DocumentResponse` 加 `hasUnpublishedChanges` | 新增/调整 |
| Entity | `DocumentEntity` | 新增 `hasUnpublishedChanges` |
| 依赖 | `java-diff-utils`（pom） | LCS diff |
| 测试 | `DocumentDiffServiceTest`（新）、`DocumentServiceTest` 增补 | 任意两版比较、状态流转、脏标记、归档禁止编辑 |

## 5. 验收对照

| 验收项 | 落点 | 状态 |
| --- | --- | --- |
| 发布版本不可变 | 已有 + 补测试守住 | 保持 |
| 可比较任意两版 | 3.1 动态 diff 接口 | 新增 |
| 可回滚 | 已有 | 保持 |
| 文档状态可流转 | 3.2 / 3.3 状态闭环与归档 | 新增 |

## 5.1 前端配套（static/document-governance.*）

后端 `GET /versions` 由全文响应改为轻量摘要，会打破旧的版本 diff 展示，故前端一并维护：

- **修复回归**：版本历史不再依赖 `version.diffContent`，改为调用动态 diff 接口。
- **任意两版比较**：版本项支持多选（最多 2 个），选中 2 个自动比较；结构化 hunk 渲染为带 +/- 与增删配色的行，不再是纯文本 `<pre>`。
- **对比草稿**：选中单个版本可与当前草稿比较（`to=draft`）。
- **回滚**：选中单个版本可回滚，生成新版本并刷新。
- **归档 / 恢复**：编辑区新增归档按钮，调 `POST /status`；归档态禁用编辑与发布；状态筛选新增「已归档」。
- **未发布改动**：文档列表红点与编辑区元信息展示 `hasUnpublishedChanges`。

## 6. 风险与注意

- **依赖新增**：引入 `java-diff-utils`（Apache-2.0），需在 pom 中固定版本。
- **迁移兼容**：`V2` 仅 `ADD COLUMN` 带默认值，对存量数据无破坏；上线前确认 Flyway 版本号不冲突。
- **大文本 diff 性能**：LCS 对超大文档为 O(n·m)，若单文档行数极大需评估上限；首期技术文档规模可控，暂不做分块。
- **状态与草稿一致性**：标记位在同一事务内更新，避免并发下状态与脏标记不一致。
