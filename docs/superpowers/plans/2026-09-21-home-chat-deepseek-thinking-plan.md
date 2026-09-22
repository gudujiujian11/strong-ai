# 首页会话 DeepSeek 深度思考 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让首页新建会话默认使用 DeepSeek V4.1 Flash，并支持切换该模型的深度思考模式。

**Architecture:** 复用现有会话级 `deepThinking` 状态和输入框按钮，不新增数据结构。前端扩大按钮支持范围并修改新会话默认模型；后端把 DeepSeek 开关转换为请求顶层 `thinking.type` 扩展参数，同时保留 GPT-5.6 的 `reasoning_effort` 行为。

**Tech Stack:** Java 8、Spring Boot、JUnit 5、Mockito、原生 HTML/CSS/JavaScript、ai4j 2.4.2

## Global Constraints

- 新建会话默认模型为 `deepseek:deepseek-v4.1-flash`，深度思考默认关闭。
- DeepSeek V4.1 Flash 和 OpenAI GPT-5.6 显示切换按钮；DeepSeek Reasoner 不显示。
- 已有会话数据不迁移。
- 不修改 Markdown 优化台或文档治理台。
- 不新增依赖，不创建 Git commit。
- Maven 必须使用 `/Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn`。

---

### Task 1: DeepSeek 请求参数

**Files:**
- Modify: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/service/AiChatServiceTest.java`
- Modify: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/service/AiChatService.java`

**Interfaces:**
- Consumes: `AiChatRequest.getProvider()`、`getModel()`、`getDeepThinking()`。
- Produces: DeepSeek V4.1 Flash 的 `ChatCompletion.extraBody["thinking"]`，值为 `{ "type": "enabled" }` 或 `{ "type": "disabled" }`。

- [x] **Step 1: 写开启深度思考的失败测试**

在 `AiChatServiceTest` 增加测试，调用真实 `AiChatService.chat` 并捕获传给 ai4j 的请求：

```java
@Test
void shouldEnableDeepThinkingForDeepSeekFlash() throws Exception {
    AiServiceRegistry registry = mock(AiServiceRegistry.class);
    IChatService chatService = mock(IChatService.class);
    when(registry.contains("deepseek")).thenReturn(true);
    when(registry.getChatService("deepseek")).thenReturn(chatService);
    when(chatService.chatCompletion(any(ChatCompletion.class)))
            .thenReturn(chatResponse("deepseek-v4.1-flash"));

    AiChatRequest request = chatRequest("deepseek", "deepseek-v4.1-flash");
    request.setDeepThinking(Boolean.TRUE);
    new AiChatService(registry).chat(request);

    ArgumentCaptor<ChatCompletion> captor = ArgumentCaptor.forClass(ChatCompletion.class);
    verify(chatService).chatCompletion(captor.capture());
    assertEquals("enabled", ((java.util.Map<?, ?>) captor.getValue()
            .getExtraBody().get("thinking")).get("type"));
}
```

- [x] **Step 2: 运行测试并确认因参数缺失而失败**

Run:

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am -DforkCount=0 -DfailIfNoTests=false -Dtest=AiChatServiceTest#shouldEnableDeepThinkingForDeepSeekFlash test
```

Expected: FAIL，`getExtraBody()` 中不存在 `thinking`。

- [x] **Step 3: 实现最小 DeepSeek 参数映射**

在 `AiChatService.setDeepThinking` 中保留 GPT-5.6 分支，并增加 DeepSeek V4.1 Flash 分支：

```java
if ("deepseek".equals(provider)
        && "deepseek-v4.1-flash".equals(request.getModel().trim().toLowerCase(Locale.ROOT))) {
    builder.extraBody("thinking", java.util.Collections.singletonMap(
            "type", Boolean.TRUE.equals(request.getDeepThinking()) ? "enabled" : "disabled"));
}
```

- [x] **Step 4: 补充关闭态测试并运行服务测试**

增加 `shouldDisableDeepThinkingForDeepSeekFlash`，结构与开启态测试一致，设置 `Boolean.FALSE` 并断言 `thinking.type == "disabled"`。

Run:

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am -DforkCount=0 -DfailIfNoTests=false -Dtest=AiChatServiceTest test
```

Expected: `AiChatServiceTest` 全部 PASS，原有 GPT-5.6 参数断言继续通过。

### Task 2: 首页默认模型与按钮可见性

**Files:**
- Modify: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/ui/WebUiResourceTest.java`
- Modify: `strong-ai-webapp/src/main/resources/static/app.js`

**Interfaces:**
- Consumes: 会话的 `provider` 和 `model` 字段。
- Produces: `DEFAULT_MODEL = { provider: "deepseek", model: "deepseek-v4.1-flash" }`；`supportsDeepThinking(conversation)` 对 GPT-5.6 和 DeepSeek V4.1 Flash 返回 `true`。

- [x] **Step 1: 写默认模型和可见性失败测试**

在 `WebUiResourceTest.shouldPackageChatUiResources` 增加：

```java
assertTrue(javascript.contains(
        "const DEFAULT_MODEL = { provider: \"deepseek\", model: \"deepseek-v4.1-flash\" };"));
assertTrue(javascript.contains(
        "conversation.provider === \"deepseek\" && conversation.model === \"deepseek-v4.1-flash\""));
```

- [x] **Step 2: 运行测试并确认因旧默认值和旧支持条件而失败**

Run:

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am -DforkCount=0 -DfailIfNoTests=false -Dtest=WebUiResourceTest test
```

Expected: FAIL，缺少 DeepSeek 默认模型或支持条件。

- [x] **Step 3: 修改默认模型与支持范围**

在 `app.js` 修改常量：

```javascript
const DEFAULT_MODEL = { provider: "deepseek", model: "deepseek-v4.1-flash" };
```

将支持判断改为：

```javascript
function supportsDeepThinking(conversation) {
    const supportsOpenAi = conversation.provider === "openai"
        && conversation.model.startsWith("gpt-5.6");
    const supportsDeepSeek = conversation.provider === "deepseek"
        && conversation.model === "deepseek-v4.1-flash";
    return supportsOpenAi || supportsDeepSeek;
}
```

- [x] **Step 4: 运行 UI 测试并确认通过**

Run:

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am -DforkCount=0 -DfailIfNoTests=false -Dtest=WebUiResourceTest test
```

Expected: `WebUiResourceTest` 全部 PASS。

### Task 3: 集成验证

**Files:**
- Verify: `strong-ai-webapp/src/main/resources/static/app.js`
- Verify: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/service/AiChatService.java`

**Interfaces:**
- Consumes: Task 1 和 Task 2 的完整行为。
- Produces: 可交付的首页 DeepSeek 深度思考功能。

- [x] **Step 1: 运行全部相关测试**

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am -DforkCount=0 -DfailIfNoTests=false -Dtest=AiChatServiceTest,WebUiResourceTest test
```

Expected: 两个测试类全部 PASS，构建为 `BUILD SUCCESS`。

- [x] **Step 2: 检查改动范围**

```bash
git diff --check
git status --short
git diff -- strong-ai-webapp/src/main/resources/static/app.js strong-ai-webapp/src/main/java/cn/strong/webapp/user/service/AiChatService.java strong-ai-webapp/src/test/java/cn/strong/webapp/user/ui/WebUiResourceTest.java strong-ai-webapp/src/test/java/cn/strong/webapp/user/service/AiChatServiceTest.java
```

Expected: 无空白错误，只包含默认模型、按钮支持范围、DeepSeek 请求参数和对应测试。
