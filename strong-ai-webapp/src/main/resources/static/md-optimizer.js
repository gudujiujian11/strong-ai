(() => {
    "use strict";

    const THEME_KEY = "strong-ai.theme";
    const MAX_ARCHIVE_SIZE = 20 * 1024 * 1024;
    const MAX_ARCHIVE_ENTRIES = 100;
    const MAX_EXTRACTED_SIZE = 50 * 1024 * 1024;
    const MAX_ENTRY_SIZE = 20 * 1024 * 1024;
    const state = { sourceType: "text", mode: "format", result: null, view: "preview", loading: false, archive: null, objectUrls: [], image: null, imageUrl: null };
    const $ = (id) => document.getElementById(id);
    const els = {};

    document.addEventListener("DOMContentLoaded", initialize);

    function initialize() {
        ["themeButton", "markdownInput", "fileInput", "fileDrop", "fileName", "imageDrop", "imageInput", "imagePreview", "imageName", "contentCount", "validationMessage", "documentType", "audience", "modelSelect", "preserveCode", "preserveLinks", "optimizeButton", "parseImageButton", "resultState", "resultEmpty", "markdownPreview", "diffView", "sourceView", "resultSummary", "summaryText", "copyButton", "downloadButton", "retryButton", "toast"].forEach((id) => { els[id] = $(id); });
        applyTheme();
        bindEvents();
        refreshIcons();
        updateInputState();
    }

    function bindEvents() {
        els.themeButton.addEventListener("click", toggleTheme);
        els.markdownInput.addEventListener("input", () => { updateInputState(); clearValidation(); });
        els.fileInput.addEventListener("change", handleFile);
        els.imageInput.addEventListener("change", (event) => { const file = event.target.files[0]; if (file) selectImage(file); });
        document.addEventListener("paste", handlePaste);
        ["dragenter", "dragover"].forEach((eventName) => els.fileDrop.addEventListener(eventName, (event) => { event.preventDefault(); els.fileDrop.classList.add("dragging"); }));
        ["dragleave", "drop"].forEach((eventName) => els.fileDrop.addEventListener(eventName, (event) => { event.preventDefault(); els.fileDrop.classList.remove("dragging"); }));
        els.fileDrop.addEventListener("drop", (event) => { const file = event.dataTransfer.files[0]; if (file) readFile(file); });
        ["dragenter", "dragover"].forEach((eventName) => els.imageDrop.addEventListener(eventName, (event) => { event.preventDefault(); els.imageDrop.classList.add("dragging"); }));
        ["dragleave", "drop"].forEach((eventName) => els.imageDrop.addEventListener(eventName, (event) => { event.preventDefault(); els.imageDrop.classList.remove("dragging"); }));
        els.imageDrop.addEventListener("drop", (event) => { const file = event.dataTransfer.files[0]; if (file) selectImage(file); });
        document.querySelectorAll("[data-source-tab]").forEach((button) => button.addEventListener("click", () => selectSource(button.dataset.sourceTab)));
        document.querySelectorAll("[data-mode]").forEach((button) => button.addEventListener("click", () => selectMode(button.dataset.mode)));
        document.querySelectorAll("[data-result-tab]").forEach((button) => button.addEventListener("click", () => selectView(button.dataset.resultTab)));
        els.optimizeButton.addEventListener("click", optimize);
        els.parseImageButton.addEventListener("click", parseImage);
        els.retryButton.addEventListener("click", () => state.sourceType === "image" ? parseImage() : optimize());
        els.copyButton.addEventListener("click", copyResult);
        els.downloadButton.addEventListener("click", downloadResult);
        document.addEventListener("keydown", (event) => { if ((event.metaKey || event.ctrlKey) && event.key === "Enter") { event.preventDefault(); if (!els.optimizeButton.disabled) optimize(); } });
    }

    function selectSource(sourceType) {
        state.sourceType = sourceType;
        if (sourceType === "text") {
            clearArchive();
            els.fileName.textContent = "";
        }
        if (sourceType !== "image") clearImage();
        document.querySelectorAll("[data-source-tab]").forEach((button) => { const active = button.dataset.sourceTab === sourceType; button.classList.toggle("active", active); button.setAttribute("aria-selected", String(active)); });
        updateInputState();
    }

    function updateInputState() {
        const fileMode = state.sourceType === "file";
        const imageMode = state.sourceType === "image";
        els.fileDrop.hidden = !fileMode;
        els.imageDrop.hidden = !imageMode;
        els.markdownInput.classList.toggle("file-mode", fileMode);
        els.markdownInput.hidden = imageMode;
        els.markdownInput.placeholder = fileMode ? "文件内容会显示在这里，可继续编辑" : "# 输入或粘贴 Markdown 文档\n\n从一段文字开始，右侧会生成更清晰的版本。";
        els.contentCount.textContent = imageMode ? (state.image ? `${Math.round(state.image.size / 1024)} KB` : "0 KB") : `${els.markdownInput.value.length.toLocaleString()} 字`;
        els.optimizeButton.disabled = state.loading || !els.markdownInput.value.trim();
        els.optimizeButton.hidden = imageMode;
        els.parseImageButton.hidden = !imageMode;
        els.parseImageButton.disabled = state.loading || !state.image;
    }

    function selectMode(mode) {
        state.mode = mode;
        document.querySelectorAll("[data-mode]").forEach((button) => { const active = button.dataset.mode === mode; button.classList.toggle("active", active); button.setAttribute("aria-checked", String(active)); });
    }

    function handleFile(event) { const file = event.target.files[0]; if (file) readFile(file); }

    function handlePaste(event) {
        const items = event.clipboardData && event.clipboardData.items;
        if (!items) return;
        const extensions = { "image/png": "png", "image/jpeg": "jpg", "image/webp": "webp", "image/gif": "gif" };
        for (let index = 0; index < items.length; index++) {
            const item = items[index];
            if (item.kind !== "file" || !extensions[item.type]) continue;
            const file = item.getAsFile();
            if (!file) continue;
            event.preventDefault();
            selectSource("image");
            const namedFile = /\.(png|jpe?g|webp|gif)$/i.test(file.name || "")
                ? file
                : new File([file], `pasted-image.${extensions[file.type]}`, { type: file.type });
            selectImage(namedFile);
            return;
        }
    }

    function selectImage(file) {
        const supported = ["image/png", "image/jpeg", "image/webp", "image/gif"];
        if (!supported.includes(file.type) || !/\.(png|jpe?g|webp|gif)$/i.test(file.name)) { showValidation("仅支持 PNG、JPEG、WebP 或 GIF 图片"); return; }
        if (file.size > 10 * 1024 * 1024) { showValidation("图片大小不能超过 10 MB"); return; }
        if (state.imageUrl) URL.revokeObjectURL(state.imageUrl);
        state.image = file;
        state.imageUrl = URL.createObjectURL(file);
        els.imagePreview.src = state.imageUrl;
        els.imagePreview.hidden = false;
        els.imageName.textContent = file.name;
        clearValidation();
        updateInputState();
    }

    function clearImage() {
        if (state.imageUrl) URL.revokeObjectURL(state.imageUrl);
        state.image = null;
        state.imageUrl = null;
        if (els.imagePreview) { els.imagePreview.hidden = true; els.imagePreview.removeAttribute("src"); }
        if (els.imageName) els.imageName.textContent = "";
        if (els.imageInput) els.imageInput.value = "";
    }

    async function parseImage() {
        if (state.loading || !state.image) return;
        state.loading = true;
        updateInputState();
        els.resultSummary.hidden = true;
        setState("正在解析图片", "busy");
        try {
            const parts = els.modelSelect.value.split(":");
            const form = new FormData();
            form.append("image", state.image);
            form.append("provider", parts[0]);
            form.append("model", parts.slice(1).join(":"));
            form.append("maxTokens", "8192");
            state.result = await requestMultipart("/api/md-optimizer/parse-image", form);
            renderResult();
            setState("已完成", "success");
            showToast("图片解析完成");
        } catch (error) {
            setState("处理失败", "error");
            showValidation(cleanError(error.message));
        } finally {
            state.loading = false;
            updateInputState();
            refreshIcons();
        }
    }

    function readFile(file) {
        if (/\.zip$/i.test(file.name)) { readZip(file); return; }
        if (!/\.(md|markdown)$/i.test(file.name)) { showValidation("只支持 .md、.markdown 或 .zip 文件"); return; }
        clearArchive();
        const reader = new FileReader();
        reader.onload = () => { els.markdownInput.value = String(reader.result || "").replace(/^\uFEFF/, ""); els.fileName.textContent = file.name; updateInputState(); clearValidation(); };
        reader.onerror = () => showValidation("文件读取失败，请重试");
        reader.readAsText(file, "UTF-8");
    }

    async function readZip(file) {
        const previousContent = els.markdownInput.value;
        const previousFileName = els.fileName.textContent;
        if (file.size > MAX_ARCHIVE_SIZE) { showValidation("ZIP 文件不能超过 20 MB"); return; }
        if (!window.JSZip) { showValidation("ZIP 解析组件加载失败，请刷新页面后重试"); return; }
        state.loading = true;
        updateInputState();
        setState("正在读取压缩包", "busy");
        clearArchive();
        try {
            const zip = await window.JSZip.loadAsync(file);
            const entries = [];
            const names = Object.keys(zip.files);
            if (names.length > MAX_ARCHIVE_ENTRIES) throw new Error("ZIP 文件条目不能超过 100 个");
            const seenPaths = new Set();
            let extractedSize = 0;
            for (const name of names) {
                const zipEntry = zip.files[name];
                const rawPath = zipEntry.unsafeOriginalName || name;
                const path = normalizeArchivePath(rawPath);
                if (zipEntry.dir) continue;
                if (seenPaths.has(path)) throw new Error("ZIP 内存在重复文件路径: " + path);
                seenPaths.add(path);
                if (/\.zip$/i.test(path)) throw new Error("不支持 ZIP 内嵌套 ZIP 文件");
                const declaredSize = zipEntry._data && zipEntry._data.uncompressedSize;
                if (declaredSize > MAX_ENTRY_SIZE || extractedSize + declaredSize > MAX_EXTRACTED_SIZE) throw new Error("ZIP 解压后的文件大小超过限制");
                const bytes = await zipEntry.async("uint8array");
                if (bytes.byteLength > MAX_ENTRY_SIZE) throw new Error("ZIP 内单个文件不能超过 20 MB: " + path);
                extractedSize += bytes.byteLength;
                if (extractedSize > MAX_EXTRACTED_SIZE) throw new Error("ZIP 解压后的文件总大小不能超过 50 MB");
                entries.push({ path: path, bytes: bytes });
            }
            const markdownEntries = entries.filter((entry) => isMarkdownPath(entry.path));
            if (markdownEntries.length !== 1) throw new Error("ZIP 必须恰好包含一个 Markdown 文件");
            const markdownEntry = markdownEntries[0];
            const assets = new Map();
            entries.forEach((entry) => {
                if (entry.path === markdownEntry.path) return;
                const type = mimeType(entry.path);
                const url = URL.createObjectURL(new Blob([entry.bytes], { type: type }));
                state.objectUrls.push(url);
                assets.set(entry.path, { url: url, name: fileName(entry.path), isImage: type.indexOf("image/") === 0 });
            });
            state.archive = { fileName: file.name, markdownPath: markdownEntry.path, entries: entries, assets: assets };
            els.markdownInput.value = decodeUtf8(markdownEntry.bytes);
            els.fileName.textContent = file.name;
            clearValidation();
            setState("已读取 ZIP", "success");
        } catch (error) {
            clearArchive();
            els.markdownInput.value = previousContent;
            els.fileName.textContent = previousFileName;
            showValidation(cleanError(error.message));
        } finally {
            state.loading = false;
            updateInputState();
            refreshIcons();
        }
    }

    function normalizeArchivePath(rawPath) {
        const path = String(rawPath || "").replace(/\\/g, "/");
        if (!path || path.startsWith("/") || /^[A-Za-z]:\//.test(path)) throw new Error("ZIP 内存在不安全的文件路径");
        const parts = path.split("/");
        if (parts.some((part) => part === "..")) throw new Error("ZIP 内不允许包含 .. 路径");
        const normalized = parts.filter((part) => part && part !== ".").join("/");
        if (!normalized) throw new Error("ZIP 内存在无效的文件路径");
        return normalized;
    }

    function isMarkdownPath(path) { return /\.(md|markdown)$/i.test(path); }
    function decodeUtf8(bytes) {
        try { return new TextDecoder("utf-8", { fatal: true }).decode(bytes).replace(/^\uFEFF/, ""); }
        catch (error) { throw new Error("Markdown 文件必须使用 UTF-8 编码"); }
    }
    function fileName(path) { return path.slice(path.lastIndexOf("/") + 1); }
    function mimeType(path) {
        const extension = path.split(".").pop().toLowerCase();
        const types = { png: "image/png", jpg: "image/jpeg", jpeg: "image/jpeg", gif: "image/gif", webp: "image/webp", svg: "image/svg+xml", avif: "image/avif", bmp: "image/bmp", pdf: "application/pdf", txt: "text/plain", json: "application/json", csv: "text/csv", doc: "application/msword", docx: "application/vnd.openxmlformats-officedocument.wordprocessingml.document", xls: "application/vnd.ms-excel", xlsx: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" };
        return types[extension] || "application/octet-stream";
    }
    function clearArchive() {
        state.objectUrls.forEach((url) => URL.revokeObjectURL(url));
        state.objectUrls = [];
        state.archive = null;
    }

    async function optimize() {
        if (state.loading || !els.markdownInput.value.trim()) return;
        state.loading = true;
        updateInputState();
        setState("正在校验", "busy");
        els.resultSummary.hidden = true;
        try {
            const request = buildRequest();
            const validation = await requestJson("/api/md-optimizer/validate", request);
            if (!validation.valid) throw new Error(validation.message || "Markdown 校验失败");
            setState("正在优化", "busy");
            state.result = await requestJson("/api/md-optimizer/optimize", request);
            renderResult();
            setState("已完成", "success");
            showToast("Markdown 优化完成");
        } catch (error) {
            setState("处理失败", "error");
            showValidation(cleanError(error.message));
        } finally {
            state.loading = false;
            updateInputState();
            refreshIcons();
        }
    }

    function buildRequest() {
        const modelParts = els.modelSelect.value.split(":");
        return { sourceType: state.sourceType, content: els.markdownInput.value, fileName: state.archive ? state.archive.markdownPath : els.fileName.textContent || null, mode: state.mode, documentType: els.documentType.value, audience: els.audience.value, tone: "专业、简洁", preserveCode: els.preserveCode.checked, preserveLinks: els.preserveLinks.checked, provider: modelParts[0], model: modelParts.slice(1).join(":"), deepThinking: false, maxTokens: 8192 };
    }

    function renderResult() {
        const result = state.result;
        els.resultEmpty.hidden = true;
        els.copyButton.disabled = false;
        els.downloadButton.disabled = false;
        els.resultSummary.hidden = false;
        els.summaryText.textContent = result.changeSummary || "已完成 Markdown 优化。";
        els.markdownPreview.innerHTML = renderMarkdown(result.optimizedContent, state.archive && state.archive.assets, state.archive && state.archive.markdownPath);
        els.sourceView.textContent = result.optimizedContent || "";
        els.diffView.innerHTML = renderDiff(result.originalContent || "", result.optimizedContent || "");
        selectView(state.view);
    }

    function selectView(view) {
        state.view = view;
        document.querySelectorAll("[data-result-tab]").forEach((button) => { const active = button.dataset.resultTab === view; button.classList.toggle("active", active); button.setAttribute("aria-selected", String(active)); });
        els.markdownPreview.hidden = view !== "preview" || !state.result;
        els.diffView.hidden = view !== "diff" || !state.result;
        els.sourceView.hidden = view !== "source" || !state.result;
    }

    function renderDiff(original, optimized) {
        const oldLines = original.split(/\r?\n/); const newLines = optimized.split(/\r?\n/); const lines = []; const max = Math.max(oldLines.length, newLines.length);
        for (let i = 0; i < max; i++) {
            if (oldLines[i] === newLines[i]) lines.push(`<span class="diff-same">  ${escapeHtml(oldLines[i] || "")}</span>`);
            else { if (oldLines[i] !== undefined) lines.push(`<span class="diff-remove">- ${escapeHtml(oldLines[i])}</span>`); if (newLines[i] !== undefined) lines.push(`<span class="diff-add">+ ${escapeHtml(newLines[i])}</span>`); }
        }
        return lines.join("\n");
    }

    function renderMarkdown(source, assets, markdownPath) {
        if (!source) return "";
        const blocks = [];
        let text = source.replace(/```([^\n]*)\n?([\s\S]*?)```/g, (_, language, code) => { const token = `@@CODE_${blocks.length}@@`; blocks.push(`<pre><code>${escapeHtml(code.replace(/\n$/, ""))}</code></pre>`); return token; });
        const inlineAssets = [];
        const assetToken = (html) => { const token = `@@ASSET_${inlineAssets.length}@@`; inlineAssets.push(html); return token; };
        text = text.replace(/!\[([^\]]*)\]\(\s*<?([^)>\s]+)>?(?:\s+["'][^"']*["'])?\s*\)/g, (match, label, target) => {
            const html = renderAsset(label, target, true, assets, markdownPath);
            return html ? assetToken(html) : match;
        });
        text = text.replace(/\[([^\]]+)\]\(\s*<?([^)>\s]+)>?(?:\s+["'][^"']*["'])?\s*\)/g, (match, label, target) => {
            const html = renderAsset(label, target, false, assets, markdownPath);
            return html ? assetToken(html) : match;
        });
        text = escapeHtml(text).replace(/^### (.+)$/gm, "<h3>$1</h3>").replace(/^## (.+)$/gm, "<h2>$1</h2>").replace(/^# (.+)$/gm, "<h1>$1</h1>").replace(/^> (.+)$/gm, "<blockquote>$1</blockquote>").replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>").replace(/`([^`\n]+)`/g, "<code>$1</code>");
        inlineAssets.forEach((html, index) => { text = text.replace(`@@ASSET_${index}@@`, html); });
        const output = []; let list = false; let paragraph = [];
        const flush = () => { if (paragraph.length) output.push(`<p>${paragraph.join("<br>")}</p>`); paragraph = []; };
        const closeList = () => { if (list) output.push("</ul>"); list = false; };
        text.split("\n").forEach((line) => { if (/^@@CODE_\d+@@$/.test(line) || /^<h[123]>/.test(line) || /^<blockquote>/.test(line)) { flush(); closeList(); output.push(line); } else if (/^[-*] /.test(line)) { flush(); if (!list) output.push("<ul>"); list = true; output.push(`<li>${line.slice(2)}</li>`); } else if (!line.trim()) { flush(); closeList(); } else { closeList(); paragraph.push(line); } });
        flush(); closeList(); return output.join("").replace(/@@CODE_(\d+)@@/g, (_, index) => blocks[Number(index)] || "");
    }

    function renderAsset(label, target, image, assets, markdownPath) {
        const asset = resolveAsset(target, assets, markdownPath);
        const external = isSafeExternalUrl(target) ? target : null;
        const local = asset ? asset.url : isSafeRelativeReference(target) ? target : null;
        const url = asset ? asset.url : external || local;
        if (!url) return null;
        if (image && (asset && asset.isImage || external)) return `<img src="${escapeHtml(url)}" alt="${escapeHtml(label)}" loading="lazy">`;
        return `<a class="asset-link" href="${escapeHtml(url)}"${asset ? " download" : ""}>${escapeHtml(label || (asset && asset.name) || target)}</a>`;
    }

    function resolveAsset(target, assets, markdownPath) {
        if (!assets || !target || isSafeExternalUrl(target) || target.startsWith("#")) return null;
        let reference;
        try { reference = decodeURIComponent(target.split(/[?#]/)[0]).replace(/\\/g, "/"); }
        catch (error) { return null; }
        if (reference.startsWith("/") || /^[A-Za-z]:\//.test(reference)) return null;
        const directory = markdownPath && markdownPath.indexOf("/") >= 0 ? markdownPath.slice(0, markdownPath.lastIndexOf("/")) : "";
        const path = normalizeReferencePath((directory ? directory + "/" : "") + reference);
        return path && assets.get(path);
    }

    function normalizeReferencePath(path) {
        const parts = String(path || "").split("/");
        if (parts.some((part) => part === "..")) return null;
        return parts.filter((part) => part && part !== ".").join("/");
    }

    function isSafeExternalUrl(target) {
        try { const url = new URL(target, window.location.href); return url.protocol === "http:" || url.protocol === "https:"; }
        catch (error) { return false; }
    }

    function isSafeRelativeReference(target) {
        if (!target || target.startsWith("/") || target.startsWith("//") || target.startsWith("#")) return target.startsWith("#");
        return !/^[A-Za-z][A-Za-z0-9+.-]*:/.test(target) && normalizeReferencePath(target) !== null;
    }

    async function copyResult() { if (!state.result) return; try { await navigator.clipboard.writeText(state.result.optimizedContent); showToast("已复制优化结果"); } catch (error) { showToast("复制失败，请手动选择文本"); } }

    async function downloadResult() {
        if (!state.result) return;
        if (state.archive) { await downloadArchiveResult(); return; }
        const baseName = state.sourceType === "image" ? (els.imageName.textContent || "image") : (els.fileName.textContent || "optimized-document.md");
        downloadBlob(new Blob([state.result.optimizedContent], { type: "text/markdown;charset=utf-8" }), baseName.replace(/\.(markdown|md|png|jpe?g|webp|gif)$/i, "") + "-parsed.md");
    }

    async function downloadArchiveResult() {
        if (!window.JSZip) { showToast("ZIP 组件加载失败，请刷新页面后重试"); return; }
        showToast("正在生成 ZIP 文件");
        const outputZip = new window.JSZip();
        state.archive.entries.forEach((entry) => {
            const content = entry.path === state.archive.markdownPath ? new TextEncoder().encode(state.result.optimizedContent || "") : entry.bytes;
            outputZip.file(entry.path, content, { binary: true });
        });
        const blob = await outputZip.generateAsync({ type: "blob" });
        const baseName = (state.archive.fileName || "document.zip").replace(/\.zip$/i, "");
        downloadBlob(blob, baseName + "-optimized.zip");
    }

    function downloadBlob(blob, name) {
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = name;
        link.click();
        URL.revokeObjectURL(url);
    }

    async function requestJson(url, body) { const response = await fetch(url, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) }); const payload = await response.json().catch(() => null); if (!response.ok || (payload && payload.success === false)) throw new Error(payload && (payload.message || payload.msg) || "请求失败"); return payload && payload.data !== undefined ? payload.data : payload; }
    async function requestMultipart(url, body) { const response = await fetch(url, { method: "POST", body: body }); const payload = await response.json().catch(() => null); if (!response.ok || (payload && payload.success === false)) throw new Error(payload && (payload.message || payload.msg) || "请求失败"); return payload && payload.data !== undefined ? payload.data : payload; }

    function setState(text, type) { els.resultState.innerHTML = `<span class="state-dot ${type}"></span>${text}`; }
    function clearValidation() { els.validationMessage.textContent = ""; els.validationMessage.className = "validation-message"; }
    function showValidation(message) { els.validationMessage.textContent = message; els.validationMessage.className = "validation-message visible"; }
    function cleanError(message) { return message || "请求失败，请稍后重试"; }
    function escapeHtml(value) { return String(value == null ? "" : value).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;"); }
    function showToast(message) { els.toast.textContent = message; els.toast.classList.add("visible"); window.clearTimeout(showToast.timer); showToast.timer = window.setTimeout(() => els.toast.classList.remove("visible"), 2600); }
    function applyTheme() { const saved = localStorage.getItem(THEME_KEY); const preferred = window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light"; document.documentElement.dataset.theme = saved || preferred; updateThemeIcon(); }
    function toggleTheme() { const next = document.documentElement.dataset.theme === "dark" ? "light" : "dark"; document.documentElement.dataset.theme = next; localStorage.setItem(THEME_KEY, next); updateThemeIcon(); }
    function updateThemeIcon() { const dark = document.documentElement.dataset.theme === "dark"; els.themeButton.innerHTML = `<i data-lucide="${dark ? "sun" : "moon"}"></i>`; els.themeButton.setAttribute("aria-label", dark ? "切换浅色模式" : "切换深色模式"); refreshIcons(); }
    function refreshIcons() { if (window.lucide) window.lucide.createIcons(); }
})();
