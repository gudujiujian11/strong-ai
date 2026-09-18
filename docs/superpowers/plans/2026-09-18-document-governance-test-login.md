# Document Governance Test Login Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow local and test users to switch document-governance roles in the browser while keeping production gateway authentication unchanged.

**Architecture:** A configuration-backed public endpoint exposes only whether test login is enabled. The existing identity interceptor continues to validate all governance APIs and excludes only that endpoint. When enabled, the static page stores a selected identity locally, attaches the established gateway headers to HTTP and streaming requests, and derives the visible permissions from the returned identity.

**Tech Stack:** Java 8, Spring Boot 2.4, JUnit 5, static HTML/CSS/JavaScript, Fetch Streams.

## Global Constraints

- Add no dependencies, database schema changes, credentials, or real authentication flow.
- `strong.document-governance.test-login.enabled` defaults to `false`; production must not expose browser-defined identities.
- Preserve `DocumentIdentityInterceptor` validation and the existing trusted gateway header contract.
- Keep simulated identity out of query parameters and URLs.
- Do not commit unless the user explicitly requests a commit.

---

## File Structure

- Create `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/config/DocumentGovernanceTestLoginProperties.java`: binds the opt-in test-login flag.
- Create `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/controller/DocumentGovernanceTestLoginController.java`: exposes the non-sensitive flag to the static page.
- Modify `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/config/DocumentGovernanceWebConfig.java`: excludes only the flag endpoint from identity interception.
- Modify `strong-ai-webapp/src/main/resources/application.properties`: records the secure default.
- Create `strong-ai-webapp/src/test/java/cn/strong/webapp/user/documentgovernance/controller/DocumentGovernanceTestLoginControllerTest.java`: verifies the endpoint response has no identity data.
- Modify `strong-ai-webapp/src/main/resources/static/document-governance.html`: adds the conditional test identity control.
- Modify `strong-ai-webapp/src/main/resources/static/document-governance.css`: styles the compact top-bar control and its dialog.
- Modify `strong-ai-webapp/src/main/resources/static/document-governance.js`: manages stored identity, attaches headers, and consumes SSE via `fetch`.

### Task 1: Test-login configuration endpoint

**Files:**
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/config/DocumentGovernanceTestLoginProperties.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/controller/DocumentGovernanceTestLoginController.java`
- Modify: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/config/DocumentGovernanceWebConfig.java`
- Modify: `strong-ai-webapp/src/main/resources/application.properties`
- Test: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/documentgovernance/controller/DocumentGovernanceTestLoginControllerTest.java`

**Interfaces:**
- Produces `DocumentGovernanceTestLoginProperties#isEnabled(): boolean` and `DocumentGovernanceTestLoginController#config(): Result<Map<String, Boolean>>`.
- The public path is `GET /api/document-governance/test-login/config` and produces only `{ enabled: boolean }` in the `Result` payload.
- `DocumentGovernanceWebConfig` excludes exactly `/api/document-governance/test-login/config` from `DocumentIdentityInterceptor`.

- [ ] **Step 1: Write the failing controller tests**

```java
class DocumentGovernanceTestLoginControllerTest {
    @Test
    void exposesEnabledFlagWhenTestLoginIsEnabled() {
        DocumentGovernanceTestLoginProperties properties = new DocumentGovernanceTestLoginProperties();
        properties.setEnabled(true);

        Result<Map<String, Boolean>> result = new DocumentGovernanceTestLoginController(properties).config();

        assertEquals(Boolean.TRUE, result.getData().get("enabled"));
        assertEquals(1, result.getData().size());
    }

    @Test
    void defaultsTestLoginToDisabled() {
        Result<Map<String, Boolean>> result = new DocumentGovernanceTestLoginController(
                new DocumentGovernanceTestLoginProperties()).config();

        assertEquals(Boolean.FALSE, result.getData().get("enabled"));
    }
}
```

- [ ] **Step 2: Run the new test and verify it fails because the classes do not exist**

Run:

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -DforkCount=0 -DfailIfNoTests=false -Dtest=DocumentGovernanceTestLoginControllerTest test
```

Expected: compilation failure referencing `DocumentGovernanceTestLoginProperties` and `DocumentGovernanceTestLoginController`.

- [ ] **Step 3: Implement the minimal bound property and controller**

```java
@Component
@ConfigurationProperties(prefix = "strong.document-governance.test-login")
public class DocumentGovernanceTestLoginProperties {
    private boolean enabled;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
```

```java
@RestController
@RequestMapping("/api/document-governance/test-login")
public class DocumentGovernanceTestLoginController {
    private final DocumentGovernanceTestLoginProperties properties;

    public DocumentGovernanceTestLoginController(DocumentGovernanceTestLoginProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/config")
    public Result<Map<String, Boolean>> config() {
        return Result.ok(Collections.singletonMap("enabled", properties.isEnabled()));
    }
}
```

Append the secure application default:

```properties
strong.document-governance.test-login.enabled = false
```

Change interception registration to:

```java
registry.addInterceptor(new DocumentIdentityInterceptor())
        .addPathPatterns("/api/document-governance/**")
        .excludePathPatterns("/api/document-governance/test-login/config");
```

- [ ] **Step 4: Run the controller and identity-interceptor tests**

Run:

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -DforkCount=0 -DfailIfNoTests=false -Dtest=DocumentGovernanceTestLoginControllerTest,DocumentIdentityInterceptorTest test
```

Expected: both test classes pass.

### Task 2: Role-switching test control and request identity

**Files:**
- Modify: `strong-ai-webapp/src/main/resources/static/document-governance.html`
- Modify: `strong-ai-webapp/src/main/resources/static/document-governance.css`
- Modify: `strong-ai-webapp/src/main/resources/static/document-governance.js`

**Interfaces:**
- Reads `GET /api/document-governance/test-login/config` without identity headers.
- Stores `{ userId: string, teamId: string, role: "member" | "admin" }` under `strong-ai.document-governance.test-identity`.
- `request(url, options)` conditionally appends `X-Strong-User-Id`, `X-Strong-Team-Id`, and `X-Strong-Role`.

- [ ] **Step 1: Add static checks that currently fail**

Run:

```bash
rg -n 'test-login/config|X-Strong-User-Id|X-Strong-Team-Id|X-Strong-Role|testIdentityDialog' strong-ai-webapp/src/main/resources/static/document-governance.{html,js}
```

Expected: no matches, because the test identity UI and header injection do not exist yet.

- [ ] **Step 2: Add the test-login controls and state management**

Add a hidden top-bar button and `dialog` to `document-governance.html`. The dialog contains labeled user-ID and team-ID inputs, a select restricted to `member` and `admin`, a cancel icon/button, a clear action, and a confirm action. Add all matching IDs to the JavaScript `els` collection.

Add these constants and state fields near the existing local-storage constants:

```javascript
const TEST_IDENTITY_KEY = "strong-ai.document-governance.test-identity";
const TEST_LOGIN_CONFIG_API = `${API}/test-login/config`;
const DEFAULT_TEST_IDENTITY = { userId: "test-user", teamId: "test-team", role: "member" };
const state = { /* existing fields */, testLoginEnabled: false, testIdentity: null };
```

At initialization, load the flag before document identity and data. If it is enabled, load a valid stored identity or the default, display the control, and refresh identity and document data. On submit, trim user and team IDs, reject blank values with the existing toast, save the three-field object, close the dialog, and call the same reload sequence. On clear, remove the storage key and reset to the default identity while test login remains enabled.

Implement header construction without mutating callers' header objects:

```javascript
function identityHeaders() {
    if (!state.testLoginEnabled || !state.testIdentity) return {};
    return {
        "X-Strong-User-Id": state.testIdentity.userId,
        "X-Strong-Team-Id": state.testIdentity.teamId,
        "X-Strong-Role": state.testIdentity.role
    };
}
```

Update `request` so caller headers take precedence only for non-identity headers, while simulated identity headers are constructed from the selected identity:

```javascript
const headers = { "Content-Type": "application/json", ...options.headers, ...identityHeaders() };
const response = await fetch(url, { ...options, headers });
```

Style the control using the existing `top-actions`, `icon-button`, `term-dialog`, form, and responsive conventions. Keep it hidden by the HTML `hidden` attribute unless the endpoint reports `enabled: true`.

- [ ] **Step 3: Run static validation for the new identity contract**

Run:

```bash
rg -n 'test-login/config|X-Strong-User-Id|X-Strong-Team-Id|X-Strong-Role|testIdentityDialog|strong-ai.document-governance.test-identity' strong-ai-webapp/src/main/resources/static/document-governance.{html,js}
git diff --check
```

Expected: all identity control and header strings are present, and `git diff --check` exits with code 0.

### Task 3: Header-aware AI task status stream

**Files:**
- Modify: `strong-ai-webapp/src/main/resources/static/document-governance.js`

**Interfaces:**
- Replaces `subscribeTask(taskId)` native `EventSource` use with `fetch(`${API}/ai-tasks/${taskId}/events`, { headers: identityHeaders() })`.
- Consumes `event: status` and `data: <JSON>` frames; calls the existing success and failure handling paths.
- Exposes no identity values in an event-stream URL or query string.

- [ ] **Step 1: Verify the current stream cannot carry identity headers**

Run:

```bash
rg -n 'new EventSource|subscribeTask|ai-tasks/.*/events' strong-ai-webapp/src/main/resources/static/document-governance.js
```

Expected: `subscribeTask` creates `new EventSource(...)`, which cannot set request headers.

- [ ] **Step 2: Replace the stream reader with minimal Fetch Streams parsing**

Implement `subscribeTask` as an async function. Call `fetch` with `Accept: "text/event-stream"` merged with `identityHeaders()`, reject non-OK responses through the existing JSON error behavior, then consume `response.body.getReader()` through `TextDecoder`. Buffer chunks until a blank-line frame boundary, parse `event:` and `data:` lines, and ignore frames whose event is not `status`.

Extract the current `status` listener body into `handleTaskStatus(task)`, preserving the existing state updates, `source.close()` equivalents, data refresh, and toasts. Stop reading on `SUCCEEDED`, `FAILED`, or `CANCELLED`; use `AbortController` so a subsequent task cancels an older subscription. Do not append any identity data to the URL.

- [ ] **Step 3: Verify no URL-based identity or native EventSource remains**

Run:

```bash
! rg -n 'new EventSource|[?&](userId|teamId|role)=' strong-ai-webapp/src/main/resources/static/document-governance.js
rg -n 'getReader\(\)|TextDecoder|text/event-stream|X-Strong-Role' strong-ai-webapp/src/main/resources/static/document-governance.js
git diff --check
```

Expected: first command exits 0 with no match; second finds Fetch Streams and identity-header handling; whitespace check passes.

### Task 4: Focused regression verification

**Files:**
- Modify: `README.md`

**Interfaces:**
- Documents that test-login configuration is opt-in and must remain disabled in production.

- [ ] **Step 1: Document the test-only enablement contract**

Under the technical-document-governance section, add:

```markdown
未接入网关的本地或测试环境可显式设置 `strong.document-governance.test-login.enabled=true`，页面才会显示模拟身份入口。生产环境必须保持默认值 `false`，并由可信网关注入身份头。
```

- [ ] **Step 2: Run focused backend tests**

Run:

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -DforkCount=0 -DfailIfNoTests=false -Dtest=DocumentGovernanceTestLoginControllerTest,DocumentIdentityInterceptorTest test
```

Expected: Maven exits with code 0 and both test classes pass.

- [ ] **Step 3: Run final source and diff checks**

Run:

```bash
git diff --check
! rg -n 'new EventSource|[?&](userId|teamId|role)=' strong-ai-webapp/src/main/resources/static/document-governance.js
rg -n 'strong\.document-governance\.test-login\.enabled|X-Strong-User-Id|X-Strong-Team-Id|X-Strong-Role' README.md strong-ai-webapp/src/main strong-ai-webapp/src/test
```

Expected: formatting check exits 0, no identity is placed in event URLs, and the configuration plus all three contract headers are found.
