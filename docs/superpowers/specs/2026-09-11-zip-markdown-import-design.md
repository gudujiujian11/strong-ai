# ZIP Markdown 导入设计

## 目标

为 Markdown 优化台增加 ZIP 单文档包导入能力。ZIP 必须包含且只能包含一个 Markdown 文件，同时解析压缩包内的图片和其他附件，并在优化结果下载时保留这些资源。

## 方案

采用浏览器端 JSZip 解析 ZIP，不新增后端接口。现有后端继续接收 Markdown 文本和原始文件名，前端额外保存 ZIP 条目和资源映射。

选择该方案的原因：当前优化接口已经以 JSON 接收 Markdown 文本；浏览器端处理可以立即完成资源预览和附件下载，不需要引入临时文件存储、Multipart 接口或服务端清理机制。

## 导入规则

1. 文件选择器和拖拽区支持 `.zip`。
2. ZIP 内递归查找 `.md` 和 `.markdown` 文件，目录不计入文件数量。
3. Markdown 文件数量必须恰好为一个；零个或多个都提示明确错误。
4. ZIP 内的嵌套 ZIP 拒绝处理。
5. 规范化条目路径，拒绝绝对路径、Windows 盘符路径和包含 `..` 的路径。
6. 默认限制：ZIP 原始大小不超过 20 MB，条目不超过 100 个，解压后总大小不超过 50 MB，单个条目不超过 20 MB。
7. Markdown 文本仍遵守现有 120,000 字符限制；按 UTF-8 读取并移除 BOM。

## 资源与预览

以 Markdown 文件所在目录作为相对路径基准，规范化 Markdown 中的本地资源引用：

- 图片引用解析为压缩包内资源，使用 Blob URL 在预览中展示。
- 普通附件引用生成 Blob URL，展示为可下载链接。
- 外部 HTTP(S) 链接保持原样。
- 找不到的本地资源保持原 Markdown 链接，并显示为普通链接。
- 不允许通过资源 URL 注入 `javascript:`、`data:` 等危险协议。
- 生成新的 ZIP 或重新导入文件时释放旧 Blob URL。

## 优化与下载

导入 ZIP 后只将唯一 Markdown 的文本提交现有校验和优化接口，`fileName` 使用 Markdown 条目名称。优化结果中的相对资源路径保持不变。

下载规则：

- ZIP 导入且已有优化结果时，生成原 ZIP 的副本，将原 Markdown 条目替换为优化后的内容，其他条目原样保留。
- ZIP 文件名追加 `-optimized.zip`。
- 普通 Markdown 导入继续下载 `-optimized.md`。

## 前端结构

- `md-optimizer.html`：文件选择器增加 ZIP 类型，上传区提示支持 Markdown ZIP 包。
- `md-optimizer.js`：增加 ZIP 读取、条目校验、资源解析、Blob URL 生命周期管理和 ZIP 下载逻辑；Markdown 渲染器接收资源映射。
- `md-optimizer.css`：为附件链接增加轻量的预览样式，不改变现有工作区布局。
- `WebUiResourceTest`：校验 ZIP 类型、JSZip 使用和 ZIP 资源处理关键代码存在。
- `README.md`：更新 Markdown 优化台支持范围。

## 测试

- 资源测试覆盖 ZIP 文件类型和前端 ZIP 处理入口。
- 手工验证单 Markdown ZIP、无 Markdown ZIP、多 Markdown ZIP、含图片和附件 ZIP、危险路径条目、嵌套 ZIP及普通 Markdown 文件回归。
- 运行 `git diff --check` 和 `WebUiResourceTest`。

