### Task 3: Implement strategy service, version lifecycle and API

**Files:**
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/service/AiOptimizationStrategyService.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/controller/AiOptimizationStrategyController.java`
- Create: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/documentgovernance/service/AiOptimizationStrategyServiceTest.java`
- Create: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/documentgovernance/controller/AiOptimizationStrategyControllerTest.java`
- Modify: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/service/DocumentOperationLogService.java` only if a metadata helper is needed.

**Interfaces:**
- `List<AiOptimizationStrategyResponse> listAvailable()`
- `AiOptimizationStrategyResponse create(AiOptimizationStrategyRequest request)`
- `AiOptimizationStrategyResponse update(Long id, AiOptimizationStrategyRequest request)`
- `AiOptimizationStrategyVersionResponse createDraft(Long strategyId, AiOptimizationStrategyRequest request)`
- `List<AiOptimizationStrategyVersionResponse> listVersions(Long strategyId)`
- `AiOptimizationStrategyVersionResponse publish(Long strategyId, Long versionNo)`
- `AiOptimizationStrategyResponse setDefault(Long strategyId)`
- `AiOptimizationStrategyResponse disable(Long strategyId)`
- `AiOptimizationStrategyCompiledContext resolveForTask(String teamId, Long strategyId)`

- [ ] **Step 1: Write failing service tests**

Test member rejection, cross-team rejection, malformed config rejection, version creation, immutable published versions, default switching, disabled strategy exclusion, operation-log action names, and single-default behavior.

- [ ] **Step 2: Run service tests to verify failure**

Run:

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8 -DforkCount=0 -DfailIfNoTests=false' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am -Dtest=AiOptimizationStrategyServiceTest test
```

Expected: FAIL because the service is not implemented.

- [ ] **Step 3: Implement team and admin guards**

Use `DocumentIdentityResolver.current()`; members may call `listAvailable`, while all mutation methods require `identity.isAdmin()`. Every mapper lookup must include the current team ID.

- [ ] **Step 4: Implement lifecycle transactions**

Create initial draft version on strategy creation. Publishing sets the version status and main-table `currentVersionId`. Setting default clears the previous team default and sets the selected strategy in one transaction. Reject updates to `PUBLISHED` versions. Disabling a strategy leaves versions and historical tasks readable.

- [ ] **Step 5: Implement strategy controller**

Expose:

```text
GET  /api/document-governance/ai-strategies
POST /api/document-governance/ai-strategies
PUT  /api/document-governance/ai-strategies/{id}
POST /api/document-governance/ai-strategies/{id}/versions
GET  /api/document-governance/ai-strategies/{id}/versions
POST /api/document-governance/ai-strategies/{id}/versions/{versionNo}/publish
POST /api/document-governance/ai-strategies/{id}/default
POST /api/document-governance/ai-strategies/{id}/disable
```

- [ ] **Step 6: Run service and controller tests to verify pass**

Run:

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8 -DforkCount=0 -DfailIfNoTests=false' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am -Dtest=AiOptimizationStrategyServiceTest,AiOptimizationStrategyControllerTest test
```

Expected: all strategy lifecycle and endpoint tests pass.

