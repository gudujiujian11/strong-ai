# Codex Upstream Compatibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Codex Responses adapter use an explicitly configured, authorized upstream while normalizing request roles and returning the correct streaming content type.

**Architecture:** Keep `CodexController` responsible for inbound authentication and HTTP response framing. Keep `CodexResponsesService` responsible for translating the Responses request into the existing `AiChatRequest`, with the upstream provider/model injected from configuration. Continue using `AiChatService` so Codex and conversation streaming share the same tested outbound client.

**Tech Stack:** Java 8, Spring MVC, Jackson, JUnit 5, Mockito, Maven.

## Global Constraints

- Do not add dependencies or change the database schema.
- Do not bypass or suppress an upstream security policy; the selected provider and credentials must be authorized for this traffic.
- Preserve the public Codex model name `deepseek-v4.1-flash` independently of the configured upstream model.
- Do not commit or push changes.

---

### Task 1: Configurable upstream and role normalization

**Files:**
- Modify: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/service/CodexResponsesServiceTest.java`
- Modify: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/service/CodexResponsesService.java`
- Modify: `strong-ai-webapp/src/main/resources/bootstrap.yml`

**Interfaces:**
- Consumes: Responses request JSON accepted by `toChatRequest(JsonNode)`.
- Produces: `CodexResponsesService(ObjectMapper, String, String)` and an `AiChatRequest` containing the configured provider/model and only supported Chat Completions roles.

- [ ] **Step 1: Write failing tests**

Add tests proving that configured provider/model values are used and a `developer` input message is converted to `system`.

- [ ] **Step 2: Verify the tests fail**

Run:

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am -DforkCount=0 -DfailIfNoTests=false -Dtest=CodexResponsesServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation or assertion failure because the configurable constructor and role normalization do not exist.

- [ ] **Step 3: Implement the minimal adapter changes**

Inject `${codex.gateway.upstream-provider:deepseek}` and `${codex.gateway.upstream-model:deepseek-v4.1-flash}`, assign them to `AiChatRequest`, normalize `developer` to `system`, and reject unsupported roles before the request reaches the upstream.

- [ ] **Step 4: Verify the focused tests pass**

Run the command from Step 2 and expect zero failures.

### Task 2: Explicit SSE response framing

**Files:**
- Create: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/controller/CodexControllerTest.java`
- Modify: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/controller/CodexController.java`

**Interfaces:**
- Consumes: `POST /api/codex/v1/responses` with `stream=true`.
- Produces: `ResponseEntity<SseEmitter>` with `Content-Type: text/event-stream`; synchronous calls remain JSON.

- [ ] **Step 1: Write a failing controller test**

Instantiate the controller with mocked collaborators and a synchronous task executor, submit a streaming request, and assert the returned `ResponseEntity` has `MediaType.TEXT_EVENT_STREAM`.

- [ ] **Step 2: Verify the controller test fails**

Run the focused Maven test command with `-Dtest=CodexControllerTest`; expect failure because the streaming branch currently returns a bare `SseEmitter`.

- [ ] **Step 3: Implement explicit streaming response framing**

Wrap the emitter with `ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(...)` while leaving the JSON branch unchanged.

- [ ] **Step 4: Verify focused and related tests**

Run `CodexControllerTest,CodexResponsesServiceTest,AiChatServiceTest` and expect zero failures.

### Task 3: Final verification

**Files:**
- Verify all modified files and the working tree diff.

- [ ] **Step 1: Run the webapp test suite**

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am -DforkCount=0 -DfailIfNoTests=false test
```

Expected: build success with zero test failures.

- [ ] **Step 2: Review the diff**

Confirm only the planned adapter, controller, configuration, tests, and this plan changed, and that no credential values were added.
