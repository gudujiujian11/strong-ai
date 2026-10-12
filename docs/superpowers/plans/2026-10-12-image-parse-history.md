# 图片解析历史实现计划

> **For agentic workers:** 使用 `executing-plans` 在当前工作区逐项实现；不委派、不提交代码。

**Goal:** 图片解析成功后保存到数据库，并在原页面分页查看、恢复和删除历史。

**Architecture:** 复用现有 Spring MVC、MyBatis-Plus 和 Flyway。单表原子保存图片、元信息和结构化 JSON 解析结果；列表不读取图片或正文，详情与原图分开获取。前端复用现有结果渲染及下载逻辑。

**Tech Stack:** Java 8、Spring MVC、MyBatis、Jackson、MySQL、原生 JavaScript/CSS。

**Spec:** 用户在当前会话确认的功能范围。

## Global Constraints

- 不做用户隔离，所有访问者可查看和删除；仅保存成功解析，无保留期限。
- 删除需确认，同时删除原图和结果；不重新解析历史。
- 不新增依赖，不修改已有配置，保留用户对 `bootstrap.yml` 的修改。
- 只使用本机 JDK 8 编译验证受影响模块，不运行测试，不操作实际数据库。

## Review Focus

- 分页必须限制大小并保持确定的倒序，列表不加载大字段。
- 记录已被其他访问者删除时显示明确错误，不破坏当前结果。
- 切换历史图片时释放旧对象 URL，恢复文件名、模型和现有结果视图。
- 删除当前查看记录后清空原图和结果；删除最后一页记录后自动回退。
- 文件名和历史列表使用安全文本输出，原图响应禁止缓存。

### Task 1: 数据库存储与历史接口

**Files:**
- Create: `strong-ai-webapp/src/main/resources/db/migration/V6__image_parse_history.sql`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/mdoptimizer/entity/ImageParseHistoryEntity.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/mdoptimizer/mapper/ImageParseHistoryMapper.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/mdoptimizer/model/ImageParseHistory.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/mdoptimizer/model/ImageParseHistoryPage.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/mdoptimizer/service/ImageParseHistoryService.java`
- Modify: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/mdoptimizer/service/ImageMarkdownParserService.java`
- Modify: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/mdoptimizer/controller/MarkdownOptimizerController.java`

**Interfaces:** `GET /api/md-optimizer/image-history?page=1&pageSize=20` 返回 items/total/page/pageSize；`GET /image-history/{id}` 返回元信息及 result；`GET /image-history/{id}/image` 返回图片；`DELETE /image-history/{id}` 删除单条记录。

- [x] 新增带时间排序索引的单表迁移，以及实体、DTO 和参数化 SQL mapper。
- [x] 新增保存、分页、详情、图片及删除服务，校验分页和记录存在性。
- [x] 解析成功后保存；失败不插入，保存失败向前端报告。
- [x] 增加接口，原图使用受支持 MIME 类型和 no-store 响应。

### Task 2: 页面历史查看与删除

**Files:**
- Modify: `strong-ai-webapp/src/main/resources/static/md-optimizer.html`
- Modify: `strong-ai-webapp/src/main/resources/static/md-optimizer.js`
- Modify: `strong-ai-webapp/src/main/resources/static/md-optimizer.css`

- [x] 增加可折叠历史区域、加载/空/错误状态、分页及保存说明。
- [x] 显示元信息，详情和图片全部获取成功后才恢复原页面。
- [x] 复用现有解析、预览、Diff、复制及下载能力；重新生成仍新建历史。
- [x] 确认删除、刷新分页并清空已删除的当前结果。

### Task 3: 验证与交付

- [x] 检查 JavaScript 语法、改动范围和 diff 空白错误。
- [x] 用本机 JDK 8 和绝对路径 Maven 编译 webapp 及必要依赖，跳过测试。
- [x] 在使用说明及交付说明中明确迁移尚未执行、数据库/浏览器联调未验证。

## 执行记录

- `node --check strong-ai-webapp/src/main/resources/static/md-optimizer.js`、`git diff --check` 均通过。
- 本机 `jdk1.8.0_211` 配合绝对路径 Maven，离线执行 `-pl strong-ai-webapp -am -DskipTests compile`，结果 `BUILD SUCCESS`；未运行测试。
- 审查后补齐调用模型前的文件名/模型长度校验、清理后空正文校验和历史选中状态同步。
- 保持现有 Markdown 外部图片渲染行为，第三方图片请求风险记录在使用说明中；未扩大本次功能范围。
- 未执行数据库迁移，未进行实际模型/数据库/浏览器联调。
