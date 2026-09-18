# 技术文档治理台 AI 模型选择 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在技术文档治理台的 AI 优化操作区增加模型下拉选择，并将当前选择提交到现有 AI 优化任务接口。

**Architecture:** 复用原生 `select` 控件和现有 `document-governance.js` 状态/请求流程。模型选项在前端固定为 OpenAI 的 `gpt-5.6-luna`，不新增后端接口、配置或数据库字段。

**Tech Stack:** 原生 HTML、CSS、JavaScript；Spring Boot Maven 测试。

## Global Constraints

- 当前只支持 `provider=openai`、`model=gpt-5.6-luna`。
- 保持现有 AI 任务创建、重试和 SSE 状态流逻辑。
- 不修改后端接口、数据库、依赖或环境变量。
- 默认使用中文界面文案并遵循现有静态资源风格。

### Task 1: Add regression assertions for the model selector

**Files:**
- Modify: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/ui/WebUiResourceTest.java`

**Interfaces:**
- Consumes: Existing HTML and JavaScript resource loading in `WebUiResourceTest`.
- Produces: Assertions that prevent removal of the selector and old hard-coded model submission.

- [ ] **Step 1: Write the failing test assertions**

  Extend the existing document governance resource test to load `document-governance.html` and `document-governance.js`, then assert the HTML contains `id="aiModelSelect"`, the JavaScript contains `gpt-5.6-luna`, and the optimize request reads `els.aiModelSelect.value`.

- [ ] **Step 2: Run the focused test to verify it fails**

  Run: `MAVEN_OPTS='-Dfile.encoding=UTF-8 -DforkCount=0 -DfailIfNoTests=false' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am -Dtest=WebUiResourceTest test`

  Expected: FAIL because the selector id and value-based request are not yet present.

### Task 2: Implement the model selector and request wiring

**Files:**
- Modify: `strong-ai-webapp/src/main/resources/static/document-governance.html`
- Modify: `strong-ai-webapp/src/main/resources/static/document-governance.js`
- Modify: `strong-ai-webapp/src/main/resources/static/document-governance.css`

**Interfaces:**
- Consumes: Existing editor action controls and `optimize()` task creation flow.
- Produces: `els.aiModelSelect` whose selected option has `data-provider="openai"` and value `gpt-5.6-luna`; `optimize()` submits those values.

- [ ] **Step 1: Add the fixed model selector markup**

  Insert this control immediately before `#optimizeButton` in `.editor-actions`:

  ```html
  <label class="model-select" aria-label="AI 优化模型">
      <span>模型</span>
      <select id="aiModelSelect">
          <option value="gpt-5.6-luna" data-provider="openai">OpenAI / GPT-5.6 Luna</option>
      </select>
  </label>
  ```

- [ ] **Step 2: Style the control using existing design tokens**

  Add compact label/select styles beside `.editor-actions`, reusing `--surface`, `--line`, `--muted`, and the existing 34px control height. At the existing `max-width:1120px` breakpoint hide the label text only if needed to preserve the current action-row width; at `max-width:540px` hide `.model-select` with the other compact action controls so the publish action remains usable.

- [ ] **Step 3: Wire the selector into initialization and optimize**

  Add `aiModelSelect` to the `els` initialization list. Replace the hard-coded request fields in `optimize()` with:

  ```javascript
  const option = els.aiModelSelect.selectedOptions[0];
  const provider = option.dataset.provider;
  const model = option.value;
  state.task = await request(`${API}/documents/${state.active.id}/ai-tasks`, {
      method: "POST",
      body: JSON.stringify({ idempotencyKey: state.taskKey, provider, model })
  });
  ```

  Keep the existing retry key behavior and task subscription unchanged.

- [ ] **Step 4: Run the focused test to verify it passes**

  Run: `MAVEN_OPTS='-Dfile.encoding=UTF-8 -DforkCount=0 -DfailIfNoTests=false' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am -Dtest=WebUiResourceTest test`

  Expected: PASS with no test failures.

### Task 3: Verify the affected module

**Files:**
- Verify: `strong-ai-webapp/src/main/resources/static/document-governance.html`
- Verify: `strong-ai-webapp/src/main/resources/static/document-governance.js`
- Verify: `strong-ai-webapp/src/main/resources/static/document-governance.css`

**Interfaces:**
- Consumes: Completed selector implementation and focused UI resource test.
- Produces: Evidence that the affected Maven module compiles and tests pass.

- [ ] **Step 1: Run the complete webapp test suite**

  Run: `MAVEN_OPTS='-Dfile.encoding=UTF-8 -DforkCount=0 -DfailIfNoTests=false' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am test`

  Expected: exit code 0 and no test failures.

- [ ] **Step 2: Inspect the final diff**

  Run: `git diff --check && git diff -- strong-ai-webapp/src/main/resources/static/document-governance.html strong-ai-webapp/src/main/resources/static/document-governance.js strong-ai-webapp/src/main/resources/static/document-governance.css strong-ai-webapp/src/test/java/cn/strong/webapp/user/ui/WebUiResourceTest.java`

  Expected: no whitespace errors; diff contains only the selector, request wiring, styles, and focused assertions.
