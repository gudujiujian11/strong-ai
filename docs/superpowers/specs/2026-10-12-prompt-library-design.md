# Prompt 提示词管理中心设计

## 目标

把高频使用的指令沉淀为可复用模板：独立页面「自动化中心 / 提示词库」管理提示词的分类、变量、版本、收藏、标签与评分，并支持一键把渲染后的提示词填入首页聊天输入框。

## 产品决策

| 决策点 | 结论 |
| --- | --- |
| 形态 | 独立页面 `prompt-center.html`，首页侧边栏新增模块入口 |
| 数据存储 | 提示词本体与版本落 MySQL，收藏数与评分全局共享单值 |
| 分类 | 固定四类：Java 代码审查、SQL 优化、文档编写、故障排查 |
| 标签 | 自由输入多值文本（逗号分隔），仅用于筛选，无独立管理页 |
| 版本 | 手动「发布新版本」；仅正文内容变更才允许发布 |
| 评分 | 全局单值 1-5 星，任何人打分直接覆盖 |
| 收藏 | 全局计数，点击切换增减（下限 0） |
| 变量 | 正文 `{{变量名}}` 占位符，前端自动识别；填入对话时先弹表单填值再渲染 |
| 初始数据 | 迁移脚本预置每个分类 1-2 条示例提示词（含变量演示） |

## 数据库（Flyway `V7__prompt_library.sql`）

```sql
ai_prompt
  id BIGINT AUTO_INCREMENT PK
  category VARCHAR(32)   -- JAVA_REVIEW / SQL_OPTIMIZE / DOC_WRITING / TROUBLESHOOT
  title VARCHAR(100)
  description VARCHAR(500) DEFAULT ''
  tags VARCHAR(500) DEFAULT ''        -- 逗号分隔
  content MEDIUMTEXT                  -- 当前工作副本
  current_version_no BIGINT DEFAULT 0 -- 当前已发布版本号，0 表示从未发布
  rating TINYINT DEFAULT 0            -- 全局单值 0-5，0 表示未评分
  favorite_count INT DEFAULT 0
  created_at BIGINT, updated_at BIGINT
  KEY idx_ai_prompt_category (category, updated_at)

ai_prompt_version
  id BIGINT AUTO_INCREMENT PK
  prompt_id BIGINT
  version_no BIGINT
  content MEDIUMTEXT
  created_at BIGINT
  UNIQUE KEY uk_ai_prompt_version (prompt_id, version_no)
```

- 主表 `content` 是工作副本：编辑直接覆盖它，不产生版本；发布时把它复制进版本表。
- 恢复历史版本 = 把版本正文写回工作副本，不自动发布。
- 预置数据在迁移脚本中直接 INSERT，每条同时写入 v1 版本记录，`current_version_no = 1`。

## 后端

新领域包 `cn.strong.webapp.user.promptlibrary`（controller / service / mapper / entity / model），仿照 `mdoptimizer` 结构；错误用 `ParamValiateException`，写操作 `@Transactional(rollbackFor = Exception.class)`，统一返回 `Result`。

接口 `/api/prompts`：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/prompts?category=&tag=&q=` | 列表（不含正文，含评分、收藏数、版本号） |
| GET | `/api/prompts/{id}` | 详情（含正文） |
| POST | `/api/prompts` | 新建，正文非空时自动生成 v1 |
| PUT | `/api/prompts/{id}` | 编辑标题/分类/标签/说明/正文，不产生版本 |
| DELETE | `/api/prompts/{id}` | 删除，级联删版本 |
| POST | `/api/prompts/{id}/versions` | 发布新版本；正文与最新版本相同则报「内容无变更」 |
| GET | `/api/prompts/{id}/versions` | 版本列表（版本号、时间，不含正文） |
| GET | `/api/prompts/{id}/versions/{versionNo}` | 历史版本正文 |
| POST | `/api/prompts/{id}/versions/{versionNo}/restore` | 恢复为当前工作副本 |
| PUT | `/api/prompts/{id}/rating` | body `{ "score": 1..5 }`，覆盖全局评分 |
| POST | `/api/prompts/{id}/favorite` | body `{ "favorited": true/false }`，增减收藏计数，下限 0 |

## 前端 `prompt-center.html` + `.css` + `.js`

风格对齐 MD 优化台：顶栏品牌 + 跳回聊天 + 主题切换，`<dialog>` 承载编辑器、版本历史、变量填写，`toast` 反馈。

- **列表页**：面包屑「自动化中心 / 提示词库」；左侧固定分类导航 + 标签筛选 + 搜索；卡片展示标题、说明、标签、星级、收藏数、当前版本。
- **详情**：正文预览（`{{变量}}` 高亮）；操作区——填入对话、收藏切换、五星评分、复制、编辑、版本历史。
- **编辑器 dialog**：标题、分类 select、标签、说明、正文 textarea；输入时实时识别 `{{变量}}` 展示变量清单；「保存」与「发布新版本」（正文无变更时禁用发布）。
- **版本历史 dialog**：列表（版本号 + 时间）、查看、恢复、复制。
- **填入对话**：有变量先弹表单逐个填值 → 渲染 → `sessionStorage.setItem("strong-ai.prompt-draft", text)` → 跳转 `/`。

## 首页接入

- `static/index.html` 侧边栏新增 `module-link` → `/prompt-center.html`，图标 `blocks`，文案「提示词库」。
- `static/app.js` 初始化时读取 `strong-ai.prompt-draft`：存在则填入 `messageInput`、触发 `updateComposer()`、删除该 key，不自动发送。
- 仓库根目录的 `index.html` 为过期副本，本次不改。

## 边界与错误

- 分类 code 与标题、标签、正文长度做服务端校验，超长或空标题返回参数错误。
- 发布版本要求正文与最新版本不一致；没有工作副本正文时不允许发布。
- 删除提示词级联删除其版本；不存在的 id 返回参数错误。
- 评分 1-5 之外、收藏计数下限 0 由服务端兜底。
- 提示词正文不含密钥等敏感信息；沿用现有统一错误格式与不记录正文的日志约定。

## 验证

- JDK 8 编译：`MAVEN_OPTS='-Dfile.encoding=UTF-8' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am -DskipTests compile`
- `node --check` 校验 `prompt-center.js`、`app.js` 语法。
- 不启动服务、不跑测试（遵循仓库既有约定）。
