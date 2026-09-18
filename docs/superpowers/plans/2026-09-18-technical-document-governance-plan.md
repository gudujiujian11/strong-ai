# Technical Document Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有 Markdown 优化台扩展为具备团队隔离、文档版本、质量检查、团队规范和可追踪 AI 任务的技术文档治理台。

**Architecture:** 在 `mdoptimizer` 旁新增 `documentgovernance` 领域；所有 API 通过网关注入的身份上下文限定团队范围。MySQL 保存文档、不可变版本、规范和报告，Redis 保存短期任务、规则缓存和幂等键；AI 优化继续复用 `AiChatService`。

**Tech Stack:** Java 8、Spring Boot 2.4.2、Spring MVC/SSE、MySQL、Flyway、MyBatis-Plus、Redis、JUnit 5、Mockito、原生 HTML/CSS/JavaScript。

## Global Constraints

- 首期只支持 Markdown；ZIP 与图片解析沿用现有 `md-optimizer` 前端流程，不上传附件到服务端。
- 网关在进入应用前必须移除客户端伪造的身份头，并注入 `X-Strong-User-Id`、`X-Strong-Team-Id`、`X-Strong-Role`；角色仅允许 `member` 或 `admin`。
- 文档、版本、模板、术语、规则、报告与操作记录必须按 `teamId` 隔离；普通成员只能写自己的草稿。
- 已发布版本不可修改；回滚必须复制历史内容生成新版本。
- 文档正文、完整提示词和模型密钥不得写入日志、指标或异常消息。
- 草稿保留 30 天，发布版本长期保留；数据清理由定时任务按团队范围执行。
- AI 风险检查只能提供非阻断建议；标题、链接、代码块、模板章节、术语等结论必须由确定性规则给出。
- 不实现登录、组织管理、细粒度 ACL、多人实时编辑、Git/CI 或知识库问答。
- Maven 测试统一使用 `MAVEN_OPTS='-Dfile.encoding=UTF-8' -DforkCount=0 -DfailIfNoTests=false` 和 `/Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn`。
- 用户未授权创建 commit；完成每项任务后只报告 `git diff --check` 与相关测试结果。

---

## Planned File Structure

| 路径 | 职责 |
| --- | --- |
| `strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/*` | 对外请求、响应与枚举，不含数据库实现 |
| `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/context/*` | 从可信网关注入身份并执行角色校验 |
| `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/entity/*` | MyBatis-Plus 持久化实体 |
| `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/mapper/*` | 数据访问接口 |
| `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/service/*` | 文档版本、规范、质量检查与任务业务边界 |
| `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/controller/*` | `/api/document-governance` REST 与 SSE API |
| `strong-ai-webapp/src/main/resources/db/migration/V1__document_governance.sql` | 首期可重复执行的 MySQL 初始化脚本 |
| `strong-ai-webapp/src/main/resources/static/document-governance.*` | 文档列表、编辑、检查、版本和规范管理页面 |
| `strong-ai-webapp/src/test/java/cn/strong/webapp/user/documentgovernance/*` | 服务、控制器和资源的聚焦测试 |

### Task 1: 建立基础设施、身份上下文和数据库契约

**Files:**
- Modify: `strong-ai-webapp/pom.xml`
- Modify: `strong-ai-webapp/src/main/resources/bootstrap.yml`
- Create: `strong-ai-webapp/src/main/resources/db/migration/V1__document_governance.sql`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/context/DocumentIdentity.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/context/DocumentIdentityResolver.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/context/DocumentIdentityInterceptor.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/config/DocumentGovernanceWebConfig.java`
- Test: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/documentgovernance/context/DocumentIdentityInterceptorTest.java`

**Interfaces:**
- Produces `DocumentIdentityResolver.current(): DocumentIdentity` with `userId`, `teamId`, `role` and `isAdmin()`.
- Produces a request attribute named `documentIdentity`; later controllers obtain identity only through `DocumentIdentityResolver`.

- [ ] **Step 1: Write the failing identity tests**

```java
@Test
void resolvesTrustedGatewayIdentity() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Strong-User-Id", "u-1");
    request.addHeader("X-Strong-Team-Id", "team-a");
    request.addHeader("X-Strong-Role", "admin");
    assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    DocumentIdentity identity = (DocumentIdentity) request.getAttribute("documentIdentity");
    assertEquals("u-1", identity.getUserId());
    assertTrue(identity.isAdmin());
}

@Test
void rejectsMissingOrUnsupportedGatewayRole() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Strong-User-Id", "u-1");
    request.addHeader("X-Strong-Team-Id", "team-a");
    request.addHeader("X-Strong-Role", "owner");
    assertThrows(ParamValiateException.class,
            () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
}
```

- [ ] **Step 2: Run the identity test and verify it fails**

Run: `MAVEN_OPTS='-Dfile.encoding=UTF-8' -DforkCount=0 -DfailIfNoTests=false /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -Dtest=DocumentIdentityInterceptorTest test`

Expected: FAIL because the identity classes do not exist.

- [ ] **Step 3: Implement the trusted identity boundary**

```java
public final class DocumentIdentity {
    private final String userId;
    private final String teamId;
    private final String role;

    public DocumentIdentity(String userId, String teamId, String role) {
        this.userId = require(userId, "用户标识不能为空");
        this.teamId = require(teamId, "团队标识不能为空");
        this.role = role;
    }
    public String getUserId() { return userId; }
    public String getTeamId() { return teamId; }
    public String getRole() { return role; }
    public boolean isAdmin() { return "admin".equals(role); }
}

public class DocumentIdentityInterceptor implements HandlerInterceptor {
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String role = request.getHeader("X-Strong-Role");
        if (!"member".equals(role) && !"admin".equals(role)) {
            throw new ParamValiateException("网关身份角色无效");
        }
        request.setAttribute("documentIdentity", new DocumentIdentity(
                required(request, "X-Strong-User-Id"), required(request, "X-Strong-Team-Id"), role));
        return true;
    }
}
```

Register the interceptor for `/api/document-governance/**`. Enable the already-commented MyBatis-Plus and Lettuce dependencies in `pom.xml`, then add the Spring Boot-managed `org.flywaydb:flyway-core` dependency so Spring Boot executes `db/migration/V1__document_governance.sql` at startup. Configure datasource and Redis endpoints only through Nacos `db.properties` and `cache.properties`. Replace every nonempty AI API-key fallback in `bootstrap.yml` with an empty fallback such as `${OPENAI_API_KEY:}`.

Create `V1__document_governance.sql` as the following executable schema. All timestamp values are epoch milliseconds; foreign keys are intentionally omitted because the existing application uses logical deletion and scheduled retention.

```sql
CREATE TABLE dg_document (
  id BIGINT NOT NULL AUTO_INCREMENT, team_id VARCHAR(64) NOT NULL, owner_user_id VARCHAR(64) NOT NULL,
  title VARCHAR(200) NOT NULL, document_type VARCHAR(64) NOT NULL, tags VARCHAR(500) NOT NULL DEFAULT '',
  status VARCHAR(16) NOT NULL, draft_content MEDIUMTEXT NOT NULL, current_version_id BIGINT DEFAULT NULL,
  created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL, PRIMARY KEY (id),
  KEY idx_dg_document_team_updated (team_id, updated_at), KEY idx_dg_document_team_owner (team_id, owner_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE dg_document_version (
  id BIGINT NOT NULL AUTO_INCREMENT, document_id BIGINT NOT NULL, team_id VARCHAR(64) NOT NULL,
  version_no BIGINT NOT NULL, content MEDIUMTEXT NOT NULL, diff_content MEDIUMTEXT NOT NULL,
  release_note VARCHAR(500) NOT NULL DEFAULT '', created_by VARCHAR(64) NOT NULL, created_at BIGINT NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_dg_version_document_no (document_id, version_no),
  KEY idx_dg_version_team_document (team_id, document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE dg_template (
  id BIGINT NOT NULL AUTO_INCREMENT, team_id VARCHAR(64) NOT NULL, name VARCHAR(100) NOT NULL,
  document_type VARCHAR(64) NOT NULL, required_headings TEXT NOT NULL, created_by VARCHAR(64) NOT NULL,
  created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL, PRIMARY KEY (id),
  UNIQUE KEY uk_dg_template_team_name (team_id, name), KEY idx_dg_template_team_type (team_id, document_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE dg_term (
  id BIGINT NOT NULL AUTO_INCREMENT, team_id VARCHAR(64) NOT NULL, term VARCHAR(200) NOT NULL,
  preferred_term VARCHAR(200) NOT NULL DEFAULT '', is_forbidden TINYINT(1) NOT NULL DEFAULT 0,
  description VARCHAR(500) NOT NULL DEFAULT '', created_by VARCHAR(64) NOT NULL,
  created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL, PRIMARY KEY (id),
  UNIQUE KEY uk_dg_term_team_term (team_id, term), KEY idx_dg_term_team (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE dg_rule_set (
  id BIGINT NOT NULL AUTO_INCREMENT, team_id VARCHAR(64) NOT NULL, name VARCHAR(100) NOT NULL,
  config_json TEXT NOT NULL, enabled TINYINT(1) NOT NULL DEFAULT 1, created_by VARCHAR(64) NOT NULL,
  created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL, PRIMARY KEY (id),
  UNIQUE KEY uk_dg_rule_set_team_name (team_id, name), KEY idx_dg_rule_set_team (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE dg_quality_report (
  id BIGINT NOT NULL AUTO_INCREMENT, team_id VARCHAR(64) NOT NULL, document_id BIGINT NOT NULL,
  document_version_id BIGINT DEFAULT NULL, score INT NOT NULL, checked_by VARCHAR(64) NOT NULL,
  created_at BIGINT NOT NULL, PRIMARY KEY (id), KEY idx_dg_report_team_document (team_id, document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE dg_quality_issue (
  id BIGINT NOT NULL AUTO_INCREMENT, team_id VARCHAR(64) NOT NULL, report_id BIGINT NOT NULL,
  rule_code VARCHAR(64) NOT NULL, severity VARCHAR(16) NOT NULL, line_number INT NOT NULL,
  message VARCHAR(500) NOT NULL, suggestion VARCHAR(500) NOT NULL, PRIMARY KEY (id),
  KEY idx_dg_issue_team_report (team_id, report_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE dg_ai_task (
  id BIGINT NOT NULL AUTO_INCREMENT, team_id VARCHAR(64) NOT NULL, document_id BIGINT NOT NULL,
  idempotency_key VARCHAR(100) NOT NULL, status VARCHAR(16) NOT NULL, provider VARCHAR(64) NOT NULL,
  model VARCHAR(100) NOT NULL, error_code VARCHAR(64) NOT NULL DEFAULT '', prompt_tokens BIGINT DEFAULT NULL,
  completion_tokens BIGINT DEFAULT NULL, total_tokens BIGINT DEFAULT NULL, created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL, expires_at BIGINT NOT NULL, PRIMARY KEY (id),
  UNIQUE KEY uk_dg_task_idempotency (team_id, document_id, idempotency_key),
  KEY idx_dg_task_team_expiry (team_id, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE dg_operation_log (
  id BIGINT NOT NULL AUTO_INCREMENT, team_id VARCHAR(64) NOT NULL, document_id BIGINT DEFAULT NULL,
  actor_user_id VARCHAR(64) NOT NULL, action VARCHAR(64) NOT NULL, metadata_json VARCHAR(1000) NOT NULL DEFAULT '{}',
  created_at BIGINT NOT NULL, PRIMARY KEY (id), KEY idx_dg_log_team_created (team_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 4: Run the identity test and static configuration checks**

Run: `MAVEN_OPTS='-Dfile.encoding=UTF-8' -DforkCount=0 -DfailIfNoTests=false /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -Dtest=DocumentIdentityInterceptorTest test && ! rg -n 'api-key: \$\{[^}]*:sk-' strong-ai-webapp/src/main/resources/bootstrap.yml`

Expected: test PASS; `rg` returns no matches.

- [ ] **Step 5: Check changed files**

Run: `git diff --check`

Expected: no output.

### Task 2: 实现文档、草稿和不可变版本服务

**Files:**
- Create: `strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/DocumentStatus.java`
- Create: `strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/DocumentCreateRequest.java`
- Create: `strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/DocumentSaveRequest.java`
- Create: `strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/DocumentResponse.java`
- Create: `strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/DocumentVersionResponse.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/entity/DocumentEntity.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/entity/DocumentVersionEntity.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/mapper/DocumentMapper.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/mapper/DocumentVersionMapper.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/service/DocumentService.java`
- Test: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/documentgovernance/service/DocumentServiceTest.java`

**Interfaces:**
- Consumes `DocumentIdentityResolver.current()` from Task 1.
- Produces `create(DocumentCreateRequest): DocumentResponse`, `saveDraft(String, DocumentSaveRequest): DocumentResponse`, `publish(String, String): DocumentVersionResponse`, `rollback(String, Long, String): DocumentVersionResponse`, `list(String): List<DocumentResponse>`.

- [ ] **Step 1: Write failing version behavior tests**

```java
@Test
void publishCreatesAnImmutableIncrementingVersion() {
    DocumentResponse document = service.create(new DocumentCreateRequest("部署说明", "DEPLOYMENT", "# V1"));
    DocumentVersionResponse first = service.publish(document.getId(), "首次发布");
    service.saveDraft(document.getId(), new DocumentSaveRequest("# V2", "继续补充"));
    DocumentVersionResponse second = service.publish(document.getId(), "第二次发布");
    assertEquals(Long.valueOf(1), first.getVersionNo());
    assertEquals(Long.valueOf(2), second.getVersionNo());
    assertEquals("# V1", versionMapper.selectByDocumentAndNo(document.getId(), 1L).getContent());
}

@Test
void memberCannotOverwriteAnotherMembersDraft() {
    assertThrows(ParamValiateException.class,
            () -> service.saveDraft(documentOwnedByAnotherUser, new DocumentSaveRequest("# altered", "")));
}

@Test
void rollbackCopiesHistoricalContentIntoANewVersion() {
    DocumentVersionResponse restored = service.rollback(documentId, 1L, "回滚到稳定版本");
    assertEquals(Long.valueOf(3), restored.getVersionNo());
    assertEquals("# V1", restored.getContent());
}
```

- [ ] **Step 2: Run the document service test and verify it fails**

Run: `MAVEN_OPTS='-Dfile.encoding=UTF-8' -DforkCount=0 -DfailIfNoTests=false /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -Dtest=DocumentServiceTest test`

Expected: FAIL because document DTOs, mappers and service do not exist.

- [ ] **Step 3: Implement document lifecycle rules**

```java
public DocumentResponse saveDraft(String documentId, DocumentSaveRequest request) {
    DocumentEntity document = requireWritableDraft(documentId, identityResolver.current());
    document.setDraftContent(requireContent(request.getContent()));
    document.setUpdatedAt(System.currentTimeMillis());
    documentMapper.updateById(document);
    return toResponse(document);
}

public DocumentVersionResponse publish(String documentId, String releaseNote) {
    DocumentEntity document = requireWritableDraft(documentId, identityResolver.current());
    long nextVersion = versionMapper.maxVersionNo(documentId) + 1L;
    DocumentVersionEntity version = version(document, nextVersion, releaseNote, identityResolver.current().getUserId());
    versionMapper.insert(version);
    document.setCurrentVersionId(version.getId());
    document.setStatus(DocumentStatus.PUBLISHED.name());
    documentMapper.updateById(document);
    return toVersionResponse(version);
}
```

Keep draft content on `DocumentEntity`; never update a `DocumentVersionEntity` after insertion. Compute `diffContent` using a line-based, HTML-escaped unified diff helper. A member may call write methods only when `ownerUserId` equals the current `userId`; an admin may write any document in the same team. Every lookup must include the current `teamId`.

- [ ] **Step 4: Run focused document tests**

Run: `MAVEN_OPTS='-Dfile.encoding=UTF-8' -DforkCount=0 -DfailIfNoTests=false /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -Dtest=DocumentServiceTest test`

Expected: PASS.

- [ ] **Step 5: Check changed files**

Run: `git diff --check`

Expected: no output.

### Task 3: 实现模板、术语库和确定性质量检查

**Files:**
- Create: `strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/QualitySeverity.java`
- Create: `strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/QualityIssue.java`
- Create: `strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/QualityCheckResponse.java`
- Create: `strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/TermRequest.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/entity/TemplateEntity.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/entity/TermEntity.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/entity/QualityReportEntity.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/entity/QualityIssueEntity.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/mapper/TemplateMapper.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/mapper/TermMapper.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/mapper/QualityReportMapper.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/mapper/QualityIssueMapper.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/service/TemplateService.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/service/TermService.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/service/MarkdownQualityChecker.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/service/QualityReportService.java`
- Test: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/documentgovernance/service/MarkdownQualityCheckerTest.java`
- Test: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/documentgovernance/service/TermServiceTest.java`

**Interfaces:**
- Produces `MarkdownQualityChecker.check(String content, List<String> requiredHeadings, List<TermEntity> terms): List<QualityIssue>`.
- Produces `QualityReportService.checkAndSave(String documentId): QualityCheckResponse`.
- `TermService.upsert(TermRequest)` and all template/rule mutation methods require `DocumentIdentity.isAdmin()`.

- [ ] **Step 1: Write failing rule and authorization tests**

```java
@Test
void findsMissingRequiredHeadingBrokenLinkAndForbiddenTermWithLineNumbers() {
    List<QualityIssue> issues = checker.check("# 概述\n访问 [站点](javascript:alert(1))\n使用 OldSDK",
            Arrays.asList("安装"), Arrays.asList(forbidden("OldSDK", "NewSDK")));
    assertThat(issues).extracting(QualityIssue::getRuleCode)
            .contains("REQUIRED_HEADING", "UNSAFE_LINK", "FORBIDDEN_TERM");
    assertThat(issues).allMatch(issue -> issue.getLineNumber() != null && issue.getLineNumber() > 0);
}

@Test
void ignoresHeadingsAndTermsInsideFencedCode() {
    assertThat(checker.check("```bash\n# 安装 OldSDK\n```", Arrays.asList("安装"),
            Arrays.asList(forbidden("OldSDK", "NewSDK"))))
            .extracting(QualityIssue::getRuleCode).doesNotContain("FORBIDDEN_TERM");
}

@Test
void memberCannotChangeTeamTerminology() {
    assertThrows(ParamValiateException.class, () -> termService.upsert(new TermRequest("SDK", "SDK", "")));
}
```

- [ ] **Step 2: Run quality tests and verify they fail**

Run: `MAVEN_OPTS='-Dfile.encoding=UTF-8' -DforkCount=0 -DfailIfNoTests=false /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -Dtest=MarkdownQualityCheckerTest,TermServiceTest test`

Expected: FAIL because the quality types and services do not exist.

- [ ] **Step 3: Implement deterministic rules and report persistence**

Implement five rule codes: `HEADING_ORDER` for a heading level that increases by more than one, `UNCLOSED_FENCE` for an odd number of Markdown fences, `UNSAFE_LINK` for `javascript:` or `data:` URLs, `REQUIRED_HEADING` for missing template headings, and `FORBIDDEN_TERM` for a configured forbidden term outside fenced code. Give each issue `ruleCode`, `severity`, `lineNumber`, `message`, and `suggestion`; use `HIGH` for unsafe links and unclosed fences, `MEDIUM` for missing headings and forbidden terms, `LOW` for heading order.

```java
public QualityCheckResponse checkAndSave(String documentId) {
    DocumentEntity document = documentService.requireReadable(documentId, identityResolver.current());
    List<QualityIssue> issues = checker.check(document.getDraftContent(),
            templateService.requiredHeadings(document.getTeamId(), document.getDocumentType()),
            termService.list(document.getTeamId()));
    QualityReportEntity report = report(document, score(issues));
    reportMapper.insert(report);
    issues.forEach(issue -> issueMapper.insert(entity(report.getId(), issue)));
    return response(report, issues);
}
```

Cache a team's template headings and terms for five minutes; invalidate the team cache after every successful admin mutation. Do not invoke an LLM in `MarkdownQualityChecker`.

- [ ] **Step 4: Run focused quality tests**

Run: `MAVEN_OPTS='-Dfile.encoding=UTF-8' -DforkCount=0 -DfailIfNoTests=false /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -Dtest=MarkdownQualityCheckerTest,TermServiceTest test`

Expected: PASS.

- [ ] **Step 5: Check changed files**

Run: `git diff --check`

Expected: no output.

### Task 4: 提供文档治理 REST API 和 AI 任务可靠性

**Files:**
- Create: `strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/AiTaskResponse.java`
- Create: `strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/AiOptimizeRequest.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/entity/AiTaskEntity.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/mapper/AiTaskMapper.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/service/DocumentAiTaskService.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/controller/DocumentController.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/controller/DocumentGovernanceController.java`
- Test: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/documentgovernance/controller/DocumentGovernanceControllerTest.java`
- Test: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/documentgovernance/service/DocumentAiTaskServiceTest.java`

**Interfaces:**
- Produces `POST /api/document-governance/documents`, `GET /api/document-governance/documents`, `PUT /api/document-governance/documents/{id}/draft`, `POST /api/document-governance/documents/{id}/publish`, `GET /api/document-governance/documents/{id}/versions`, `POST /api/document-governance/documents/{id}/rollback`, `POST /api/document-governance/documents/{id}/quality-check`.
- Produces `POST /api/document-governance/documents/{id}/ai-tasks`, `GET /api/document-governance/ai-tasks/{taskId}`, and `GET /api/document-governance/ai-tasks/{taskId}/events`.
- Consumes `MarkdownOptimizerService.buildChatRequest(MarkdownOptimizeRequest)` and persists only the returned optimized Markdown, summary, provider, model and token metadata.

- [ ] **Step 1: Write failing controller and task retry tests**

```java
@Test
void createsDocumentOnlyInsideGatewayTeam() throws Exception {
    mockMvc.perform(post("/api/document-governance/documents")
            .header("X-Strong-User-Id", "u-1").header("X-Strong-Team-Id", "team-a")
            .header("X-Strong-Role", "member").contentType(APPLICATION_JSON)
            .content("{\"title\":\"README\",\"documentType\":\"README\",\"content\":\"# README\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.teamId").value("team-a"));
}

@Test
void retryReusesIdempotencyKeyAndNeverCreatesDuplicateTask() {
    AiTaskResponse first = service.submit(documentId, request("request-7"));
    AiTaskResponse retry = service.submit(documentId, request("request-7"));
    assertEquals(first.getId(), retry.getId());
    assertEquals(1, taskMapper.countByDocumentAndKey(documentId, "request-7"));
}

@Test
void failedAiTaskPreservesDraftAndCanRetry() {
    taskService.process(taskId); // mocked AiChatService throws IOException
    assertEquals("FAILED", taskService.get(taskId).getStatus());
    assertEquals("# original", documentService.get(documentId).getDraftContent());
}
```

- [ ] **Step 2: Run REST and task tests and verify they fail**

Run: `MAVEN_OPTS='-Dfile.encoding=UTF-8' -DforkCount=0 -DfailIfNoTests=false /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -Dtest=DocumentGovernanceControllerTest,DocumentAiTaskServiceTest test`

Expected: FAIL because controller endpoints and task service do not exist.

- [ ] **Step 3: Implement APIs, idempotency and retry states**

Use `PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED`, and `CANCELLED` task states. `submit` creates one row scoped by `(team_id, document_id, idempotency_key)` and returns an existing non-expired row for the same key. Execute through the existing Spring `TaskExecutor`; on success update only the document draft, never publish automatically. On failure store a short user-safe error code such as `MODEL_UNAVAILABLE`, not the provider exception body. `GET /events` returns an `SseEmitter(0L)` that emits `status` when the persisted status changes and ends at terminal status.

```java
public AiTaskResponse submit(String documentId, AiOptimizeRequest request) {
    DocumentEntity document = documentService.requireWritableDraft(documentId, identityResolver.current());
    AiTaskEntity task = taskMapper.findByIdempotency(document.getTeamId(), documentId, request.getIdempotencyKey());
    if (task != null && task.getExpiresAt() > System.currentTimeMillis()) return toResponse(task);
    task = pendingTask(document, request);
    taskMapper.insert(task);
    taskExecutor.execute(() -> process(task.getId()));
    return toResponse(task);
}
```

Ensure every controller maps `ParamValiateException` to the existing `Result` error convention. Add a scheduled cleanup that deletes only `PENDING`, `FAILED`, `SUCCEEDED`, and `CANCELLED` task rows whose `expires_at` is in the past; it must never delete document or version rows.

- [ ] **Step 4: Run focused API and task tests**

Run: `MAVEN_OPTS='-Dfile.encoding=UTF-8' -DforkCount=0 -DfailIfNoTests=false /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -Dtest=DocumentGovernanceControllerTest,DocumentAiTaskServiceTest test`

Expected: PASS.

- [ ] **Step 5: Check changed files**

Run: `git diff --check`

Expected: no output.

### Task 5: 增加治理工作台与规范管理界面

**Files:**
- Modify: `strong-ai-webapp/src/main/resources/static/index.html`
- Create: `strong-ai-webapp/src/main/resources/static/document-governance.html`
- Create: `strong-ai-webapp/src/main/resources/static/document-governance.css`
- Create: `strong-ai-webapp/src/main/resources/static/document-governance.js`
- Test: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/ui/WebUiResourceTest.java`

**Interfaces:**
- Consumes all Task 4 REST endpoints with `fetch`; task status is subscribed through `/api/document-governance/ai-tasks/{taskId}/events`.
- Produces no client-side persistence of document text, task outputs or team rules; browser storage may only retain theme and last selected non-sensitive UI tab.

- [ ] **Step 1: Write failing static-resource assertions**

```java
@Test
void documentGovernanceResourcesExposeTheRequiredWorkflow() throws Exception {
    assertThat(resource("static/document-governance.html"))
            .contains("document-governance.js", "文档列表", "质量检查", "版本历史", "术语库");
    assertThat(resource("static/document-governance.js"))
            .contains("/api/document-governance/documents", "ai-tasks", "quality-check", "versions");
}
```

- [ ] **Step 2: Run the UI resource test and verify it fails**

Run: `MAVEN_OPTS='-Dfile.encoding=UTF-8' -DforkCount=0 -DfailIfNoTests=false /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -Dtest=WebUiResourceTest test`

Expected: FAIL because the governance resources do not exist.

- [ ] **Step 3: Implement the work-focused interface**

Create a three-column desktop layout: document list with search/status filters, editor with save/AI optimize/quality-check commands, and inspection pane with quality issues, version Diff and version history. On screens narrower than 900px, render the list, editor and inspection pane as tabs; preserve draft text in the active textarea while switching tabs. Use the existing Lucide icon loading convention and existing theme storage key pattern. Add one navigation link from `index.html` to `/document-governance.html`.

Only administrators see template, term and rule management controls. Show quality issues with severity, line number and suggestion; clicking an issue scrolls the source editor to the associated line. Disable publish when any `HIGH` issue exists, but allow saving a draft. AI task errors must keep the editor content unchanged and provide a retry icon button that reuses the original idempotency key only after the prior task reaches `FAILED`.

- [ ] **Step 4: Run resource tests and perform browser smoke test**

Run: `MAVEN_OPTS='-Dfile.encoding=UTF-8' -DforkCount=0 -DfailIfNoTests=false /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -Dtest=WebUiResourceTest test`

Expected: PASS.

Manual check: start the application with test Nacos/MySQL/Redis configuration, open `http://localhost:11028/document-governance.html` through the authenticated gateway, then create a draft, run checks, start a failed AI task, retry it, publish a clean document, compare versions and roll back.

- [ ] **Step 5: Check changed files**

Run: `git diff --check`

Expected: no output.

### Task 6: 添加指标、操作记录、数据清理和发布验证

**Files:**
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/service/DocumentOperationLogService.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/service/DocumentMetricsService.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/service/DocumentRetentionCleanupJob.java`
- Modify: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/service/DocumentService.java`
- Modify: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/service/QualityReportService.java`
- Modify: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/service/DocumentAiTaskService.java`
- Modify: `README.md`
- Test: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/documentgovernance/service/DocumentRetentionCleanupJobTest.java`
- Test: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/documentgovernance/service/DocumentMetricsServiceTest.java`

**Interfaces:**
- Produces counters tagged only by `documentType`, `operation`, `outcome`, `ruleCode`, `provider`, `model` and `teamId` where the metric backend is approved for team identifiers.
- Produces operation log actions `DRAFT_SAVED`, `VERSION_PUBLISHED`, `VERSION_ROLLED_BACK`, `QUALITY_CHECKED`, `AI_TASK_SUBMITTED`, `AI_TASK_RETRIED` and `AI_TASK_FAILED`.

- [ ] **Step 1: Write failing retention and metrics tests**

```java
@Test
void cleanupDeletesExpiredDraftsAndTerminalTasksButNeverPublishedDocumentsOrVersions() {
    cleanupJob.cleanupExpiredData();
    assertFalse(documentMapper.exists(expiredDraftId));
    assertFalse(taskMapper.exists(expiredSucceededTaskId));
    assertTrue(documentMapper.exists(publishedDocumentId));
    assertTrue(versionMapper.exists(publishedVersionId));
}

@Test
void recordsTokenAndLatencyWithoutDocumentContent() {
    metrics.recordAiTask("README", "openai", "gpt-5.6-sol", 123L, 456L, 789L, 1200L, "SUCCEEDED");
    assertThat(registry.find("document_governance_ai_task_total").counter().count()).isEqualTo(1D);
    assertThat(capturedLogMessages).noneMatch(message -> message.contains("# internal document"));
}
```

- [ ] **Step 2: Run observability tests and verify they fail**

Run: `MAVEN_OPTS='-Dfile.encoding=UTF-8' -DforkCount=0 -DfailIfNoTests=false /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -Dtest=DocumentRetentionCleanupJobTest,DocumentMetricsServiceTest test`

Expected: FAIL because cleanup and metrics services do not exist.

- [ ] **Step 3: Implement safe observability and retention**

Record timestamps, IDs, document type, status, rule count, token counts and latency only. Do not put title, content, AI result, prompt or exception body into metric tags or operation log details. Schedule cleanup once daily; delete drafts only when `status = DRAFT`, `updated_at < now - 30 days`, and no published version exists. Add a README section describing the required identity headers, Nacos database/cache configuration, migration execution, 30-day draft retention, and the policy that API keys exist only in environment variables or encrypted Nacos values.

- [ ] **Step 4: Run affected module tests and full verification**

Run: `MAVEN_OPTS='-Dfile.encoding=UTF-8' -DforkCount=0 -DfailIfNoTests=false /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp test && git diff --check`

Expected: Maven exits `0`; `git diff --check` has no output.

- [ ] **Step 5: Perform release-readiness checks**

Verify in the test environment that: the gateway strips client-provided identity headers; the migration tables and indexes exist; missing model keys cause a safe configuration error; a member cannot read another team’s document by ID; an admin can maintain team terms; a `HIGH` issue blocks publishing; and restart recovery preserves drafts, versions and terminal task records.

## Plan Self-Review

- Spec coverage: Tasks 1–2 cover gateway identity, data persistence, team isolation, drafts, versions and rollback. Task 3 covers templates, terminology and deterministic quality reporting. Task 4 covers AI task tracking, retries and SSE. Task 5 covers the full document workflow and admin-only management. Task 6 covers metrics, audit metadata, retention, security documentation and release validation. Git/CI, knowledge search, object storage, live collaboration and granular ACL are explicitly excluded.
- Placeholder scan: the plan defines concrete paths, API routes, state values, rule codes, commands and expected results; no implementation placeholder remains.
- Type consistency: all controllers use `DocumentIdentityResolver`, document methods use the `Document*` DTOs defined in Task 2, quality uses `QualityIssue`/`QualityCheckResponse` from Task 3, and asynchronous flows use `AiOptimizeRequest`/`AiTaskResponse` from Task 4.
