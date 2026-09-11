# 图片解析 Markdown Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 MD 优化台增加单图上传、多模态解析并输出 Markdown 的完整链路。

**Architecture:** 新增图片解析领域服务和 multipart Controller；服务复用 `AiChatService` 的模型路由，但通过 ai4j 原生多模态消息发送 data URL。前端新增图片页签，结果复用现有 Markdown 预览、源码、复制和下载区。

**Tech Stack:** Spring Boot 2.4.2、Java、ai4j 2.4.2、原生 HTML/CSS/JavaScript、JUnit 5/MockMvc。

## Global Constraints

- 支持 PNG、JPEG、WebP、GIF，单图最大 10 MB。
- 不新增依赖，不记录图片内容、base64、提示词或密钥。
- 保持现有 Markdown/ZIP 优化协议和行为不变。
- Maven 命令使用 `/Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn`。

### Task 1: Add Multimodal AI Call Boundary

**Files:**
- Modify: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/service/AiChatService.java`
- Test: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/service/AiChatServiceTest.java`

**Interfaces:**
- Produces `AiChatResponse chatWithImage(String provider, String model, String systemPrompt, String userPrompt, String imageDataUrl, Integer maxTokens)`.
- Existing `chat` and `chatStream` behavior remains unchanged.

- [ ] **Step 1: Write the failing test** asserting the registry receives a user `ChatMessage` whose serialized content contains `image_url` and the supplied data URL, and that provider/model/token metadata is returned.
- [ ] **Step 2: Run the focused test** with `MAVEN_OPTS='-Dfile.encoding=UTF-8 -DforkCount=0 -DfailIfNoTests=false' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -Dtest=AiChatServiceTest test`; expect failure because the method is absent.
- [ ] **Step 3: Implement the method** by validating provider/model/prompts/data URL, selecting the registry service, building a system message and `ChatMessage.withUser(userPrompt, imageDataUrl)`, applying the same max-token/reasoning rules as `chat`, and mapping the response through the existing response mapper.
- [ ] **Step 4: Run the focused test** and expect PASS.

### Task 2: Implement Image Parsing Domain API

**Files:**
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/mdoptimizer/model/ImageMarkdownParseResponse.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/mdoptimizer/service/ImageMarkdownParserService.java`
- Modify: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/mdoptimizer/controller/MarkdownOptimizerController.java`
- Create: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/mdoptimizer/ImageMarkdownParserServiceTest.java`
- Create: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/mdoptimizer/controller/MarkdownOptimizerImageControllerTest.java`

**Interfaces:**
- `ImageMarkdownParserService.parse(MultipartFile image, String provider, String model, Integer maxTokens)` returns `MarkdownOptimizeResponse`.
- `POST /api/md-optimizer/parse-image` accepts multipart fields `image`, `provider`, `model`, `maxTokens`.

- [ ] **Step 1: Write failing service tests** for supported MIME/extension, empty image, >10 MB image, data URL construction, prompt requiring Markdown-only output, and stripping fenced ` ```markdown ` wrappers.
- [ ] **Step 2: Run focused tests** and expect validation/compile failures.
- [ ] **Step 3: Implement validation and service** with an allowlist (`image/png`, `image/jpeg`, `image/webp`, `image/gif`), 10 MB limit, safe filename handling, `Base64` encoding, and a prompt requesting headings, lists, tables, code blocks, and alt text without invented facts. Reuse `MarkdownOptimizeResponse` fields and token metadata.
- [ ] **Step 4: Add Controller endpoint** with `@RequestParam MultipartFile image`, `@RequestParam(required=false, defaultValue="openai") provider`, `model`, and `maxTokens`; return `Result.ok(...)`.
- [ ] **Step 5: Add MockMvc tests** for valid multipart delegation and invalid file response.
- [ ] **Step 6: Run focused tests** and expect PASS.

### Task 3: Add Image Parsing UI

**Files:**
- Modify: `strong-ai-webapp/src/main/resources/static/md-optimizer.html`
- Modify: `strong-ai-webapp/src/main/resources/static/md-optimizer.js`
- Modify: `strong-ai-webapp/src/main/resources/static/md-optimizer.css`
- Modify: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/ui/WebUiResourceTest.java`

**Interfaces:**
- New tab uses `data-source-tab="image"`; image input id `imageInput`; parse button id `parseImageButton`.
- JavaScript posts `FormData` to `/api/md-optimizer/parse-image` and feeds `optimizedContent` into the existing result renderer.

- [ ] **Step 1: Add resource assertions** for image tab/input, endpoint, `FormData`, allowed image accept list, and parse button.
- [ ] **Step 2: Run Web UI test** and expect failure.
- [ ] **Step 3: Add HTML controls** beside existing source tabs, an image drop/preview area, and a parse action while keeping existing optimize controls hidden/disabled in image mode.
- [ ] **Step 4: Implement JS state and upload flow**: validate one image and 10 MB client-side, show preview via object URL, submit selected model and max tokens, handle success/error/loading, and reuse `renderMarkdown`, copy, and download behavior.
- [ ] **Step 5: Add scoped CSS** for image drop area, preview dimensions, and responsive layout using existing theme variables.
- [ ] **Step 6: Run Web UI test** and expect PASS.

### Task 4: Verify the Integrated Module

**Files:**
- Modify: `README.md` only if the existing usage section does not mention image parsing.

- [ ] **Step 1: Run all md-optimizer tests** with `MAVEN_OPTS='-Dfile.encoding=UTF-8 -DforkCount=0 -DfailIfNoTests=false' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -Dtest='*mdoptimizer*,*WebUiResourceTest' test`.
- [ ] **Step 2: Run the module test suite** with `MAVEN_OPTS='-Dfile.encoding=UTF-8 -DforkCount=0 -DfailIfNoTests=false' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp test`.
- [ ] **Step 3: Inspect `git diff --check`** and verify no secrets or image payloads were added.
