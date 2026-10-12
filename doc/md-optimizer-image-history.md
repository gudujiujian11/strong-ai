# 图片解析历史

## 使用方式

- 打开 `/md-optimizer.html`，点击顶部“图片解析历史”。
- 记录按解析完成时间倒序展示，每页 20 条；可刷新并翻页。
- 点击“查看”恢复原图、解析结果和所用模型，复用预览、Diff、Markdown、复制和下载功能，不调用模型。
- “重新生成”会重新调用模型，并在成功后新增历史记录。
- 点击“删除”并确认后，原图和解析结果一并永久删除。

## 存储与部署

- 新增 Flyway 迁移 `V6__image_parse_history.sql`，创建 `ai_image_parse_history` 表；需在使用该功能前完成迁移。
- 每次成功解析将原图、文件名、MIME 类型、大小、解析结果、模型和 token 信息写入数据库。列表查询不加载图片或解析正文。
- 历史记录无用户隔离，所有访问者均可查看和删除；没有自动清理或保留期限，应关注数据库容量。
- 文档优化仍不保存原文；功能上线前已经完成且未存储的图片解析无法补回历史。
- 保存失败会使解析请求报告失败，不会返回“已保存”提示。
- 预览沿用现有 Markdown 渲染方式，结果中的外部图片链接可能请求第三方站点。

## 接口

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/api/md-optimizer/image-history?page=1&pageSize=20` | 分页查询元信息，`pageSize` 范围 1–100 |
| GET | `/api/md-optimizer/image-history/{id}` | 获取元信息及完整解析结果 |
| GET | `/api/md-optimizer/image-history/{id}/image` | 获取原图，响应禁止缓存 |
| DELETE | `/api/md-optimizer/image-history/{id}` | 永久删除原图及解析结果 |
