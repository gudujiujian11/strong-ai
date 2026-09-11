# ZIP Markdown 导入 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 Markdown 优化台导入仅含一个 Markdown 的 ZIP 文档包，并在预览、附件下载和优化结果导出中保留压缩包资源。

**Architecture:** 在浏览器端使用 JSZip 解压并校验 ZIP，将唯一 Markdown 文本提交现有 JSON API；前端以规范化相对路径维护资源映射并创建 Blob URL。优化结果下载时重新生成 ZIP，替换 Markdown 条目并保留其他条目。

**Tech Stack:** 原生 HTML/CSS/JavaScript、JSZip 3.10.1 CDN、现有 Spring Boot/JUnit 资源测试。

## Global Constraints

- ZIP 原始大小不超过 20 MB。
- ZIP 条目不超过 100 个，解压后总大小不超过 50 MB，单个条目不超过 20 MB。
- ZIP 内必须且只能有一个 `.md` 或 `.markdown` 文件。
- 拒绝嵌套 ZIP、绝对路径、Windows 盘符路径和包含 `..` 的路径。
- Markdown 文本继续遵守现有 120,000 字符限制。
- 不新增后端接口或后端依赖。
- 不提交 commit，保留工作区中已有用户改动。

---

### Task 1: Add ZIP import regression checks

**Files:**
- Modify: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/ui/WebUiResourceTest.java:35-47`

**Interfaces:**
- Consumes: packaged `md-optimizer.html`, `md-optimizer.js`, and `md-optimizer.css` resources.
- Produces: resource assertions for ZIP input, JSZip loading, archive validation, asset rendering, and ZIP export.

- [ ] **Step 1: Write the failing test**

Add these assertions to `shouldPackageMarkdownOptimizerUiResources` after the existing optimizer resource assertions:

```java
assertTrue(html.contains(".zip"));
assertTrue(javascript.contains("JSZip"));
assertTrue(javascript.contains("MAX_ARCHIVE_SIZE"));
assertTrue(javascript.contains("恰好包含一个 Markdown 文件"));
assertTrue(javascript.contains("generateAsync"));
assertTrue(styles.contains(".asset-link"));
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8 -DforkCount=0 -DfailIfNoTests=false' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am -Dtest=WebUiResourceTest test
```

Expected: `shouldPackageMarkdownOptimizerUiResources` fails because the current UI has no ZIP support markers.

### Task 2: Implement secure ZIP import and export

**Files:**
- Modify: `strong-ai-webapp/src/main/resources/static/md-optimizer.html:10-60`
- Modify: `strong-ai-webapp/src/main/resources/static/md-optimizer.js:1-180`
- Modify: `strong-ai-webapp/src/main/resources/static/md-optimizer.css:112-120`

**Interfaces:**
- Consumes: File API input, existing Markdown optimization request/response, and JSZip global.
- Produces: `state.archive` with `markdownPath`, `entries`, and `assets`; `readZip(file)` import flow; `downloadArchiveResult()` optimized ZIP download flow.

- [ ] **Step 1: Add the JSZip browser dependency and ZIP input affordance**

In `md-optimizer.html`, load JSZip before `md-optimizer.js`, extend the file input accept list with `.zip,application/zip`, and update the drop-zone copy to mention ZIP packages and the single-Markdown rule.

- [ ] **Step 2: Add archive limits, state, and path validation**

Extend the JavaScript state with archive metadata and Blob URL tracking. Define constants for 20 MB archive size, 100 entries, 50 MB extracted size, and 20 MB per entry. Normalize entry paths by converting backslashes to slashes and rejecting absolute paths, drive-letter paths, and `..` segments. Reject nested ZIP entries and duplicate normalized paths.

- [ ] **Step 3: Read and validate ZIP entries**

Use `JSZip.loadAsync(file)` and iterate non-directory entries. Decompress each entry as `uint8array`, enforce per-entry and total limits, count Markdown files case-insensitively, and reject anything other than exactly one Markdown file. Decode the selected Markdown as UTF-8, remove BOM, store all entries in memory, and build Blob URLs for resources. On any failure, release created URLs, clear archive state, keep the previous text input intact, and show a precise validation message.

- [ ] **Step 4: Integrate ZIP content with existing optimize flow**

Keep the existing JSON endpoints and set `fileName` to the Markdown entry path when `state.archive` exists. Preserve ZIP state through validate, optimize, retry, and result rendering. Release archive URLs when switching back to text input or importing another file.

- [ ] **Step 5: Render images and attachments safely**

Pass the archive asset map to `renderMarkdown`. Resolve local references relative to the Markdown entry directory. Render mapped image files as lazy `<img>` elements and mapped non-image files as `<a class="asset-link" download>`. Preserve safe HTTP(S) links, anchors, and unresolved local links. Reject dangerous protocols such as `javascript:` and `data:` before generating HTML; keep all labels and URLs HTML-escaped.

- [ ] **Step 6: Preserve resources when downloading optimized results**

Make `downloadResult` use the existing `.md` download path for normal files. For ZIP imports, create a new JSZip archive, replace only the original Markdown entry with UTF-8 optimized content, copy all other stored entry bytes unchanged, generate a Blob, and download with an `-optimized.zip` suffix.

- [ ] **Step 7: Add attachment presentation styles**

Add compact styles for preview images and `.asset-link` so resource links are visibly distinguishable while following the existing theme variables and layout.

### Task 3: Document and verify the feature

**Files:**
- Modify: `README.md:43`

**Interfaces:**
- Consumes: implemented ZIP import and export behavior.
- Produces: user-facing usage documentation.

- [ ] **Step 1: Update usage documentation**

Change the Markdown optimizer description to state that it accepts a single Markdown file or a ZIP containing exactly one Markdown file, previews images, preserves attachments, and exports optimized ZIP packages.

- [ ] **Step 2: Run focused verification**

Run:

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8 -DforkCount=0 -DfailIfNoTests=false' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am -Dtest=WebUiResourceTest test
git diff --check
```

Expected: `WebUiResourceTest` passes all tests and `git diff --check` exits successfully.

- [ ] **Step 3: Inspect final scope**

Run `git status --short` and verify only the intended UI, test, README, and design/plan files are newly changed; do not alter or revert unrelated existing worktree changes.

