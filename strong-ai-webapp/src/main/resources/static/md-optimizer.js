(() => {
    "use strict";

    const THEME_KEY = "strong-ai.theme";
    const state = { sourceType: "text", mode: "format", result: null, view: "preview", loading: false };
    const $ = (id) => document.getElementById(id);
    const els = {};

    document.addEventListener("DOMContentLoaded", initialize);

    function initialize() {
        ["themeButton", "markdownInput", "fileInput", "fileDrop", "fileName", "contentCount", "validationMessage", "documentType", "audience", "modelSelect", "preserveCode", "preserveLinks", "optimizeButton", "resultState", "resultEmpty", "markdownPreview", "diffView", "sourceView", "resultSummary", "summaryText", "copyButton", "downloadButton", "retryButton", "toast"].forEach((id) => { els[id] = $(id); });
        applyTheme();
        bindEvents();
        refreshIcons();
        updateInputState();
    }

    function bindEvents() {
        els.themeButton.addEventListener("click", toggleTheme);
        els.markdownInput.addEventListener("input", () => { updateInputState(); clearValidation(); });
        els.fileInput.addEventListener("change", handleFile);
        ["dragenter", "dragover"].forEach((eventName) => els.fileDrop.addEventListener(eventName, (event) => { event.preventDefault(); els.fileDrop.classList.add("dragging"); }));
        ["dragleave", "drop"].forEach((eventName) => els.fileDrop.addEventListener(eventName, (event) => { event.preventDefault(); els.fileDrop.classList.remove("dragging"); }));
        els.fileDrop.addEventListener("drop", (event) => { const file = event.dataTransfer.files[0]; if (file) readFile(file); });
        document.querySelectorAll("[data-source-tab]").forEach((button) => button.addEventListener("click", () => selectSource(button.dataset.sourceTab)));
        document.querySelectorAll("[data-mode]").forEach((button) => button.addEventListener("click", () => selectMode(button.dataset.mode)));
        document.querySelectorAll("[data-result-tab]").forEach((button) => button.addEventListener("click", () => selectView(button.dataset.resultTab)));
        els.optimizeButton.addEventListener("click", optimize);
        els.retryButton.addEventListener("click", optimize);
        els.copyButton.addEventListener("click", copyResult);
        els.downloadButton.addEventListener("click", downloadResult);
        document.addEventListener("keydown", (event) => { if ((event.metaKey || event.ctrlKey) && event.key === "Enter") { event.preventDefault(); if (!els.optimizeButton.disabled) optimize(); } });
    }

    function selectSource(sourceType) {
        state.sourceType = sourceType;
        document.querySelectorAll("[data-source-tab]").forEach((button) => { const active = button.dataset.sourceTab === sourceType; button.classList.toggle("active", active); button.setAttribute("aria-selected", String(active)); });
        updateInputState();
    }

    function updateInputState() {
        const fileMode = state.sourceType === "file";
        els.fileDrop.hidden = !fileMode;
        els.markdownInput.classList.toggle("file-mode", fileMode);
        els.markdownInput.placeholder = fileMode ? "文件内容会显示在这里，可继续编辑" : "# 输入或粘贴 Markdown 文档\n\n从一段文字开始，右侧会生成更清晰的版本。";
        els.contentCount.textContent = `${els.markdownInput.value.length.toLocaleString()} 字`;
        els.optimizeButton.disabled = state.loading || !els.markdownInput.value.trim();
    }

    function selectMode(mode) {
        state.mode = mode;
        document.querySelectorAll("[data-mode]").forEach((button) => { const active = button.dataset.mode === mode; button.classList.toggle("active", active); button.setAttribute("aria-checked", String(active)); });
    }

    function handleFile(event) { const file = event.target.files[0]; if (file) readFile(file); }

    function readFile(file) {
        if (!/\.(md|markdown)$/i.test(file.name)) { showValidation("只支持 .md 或 .markdown 文件"); return; }
        const reader = new FileReader();
        reader.onload = () => { els.markdownInput.value = String(reader.result || "").replace(/^\uFEFF/, ""); els.fileName.textContent = file.name; updateInputState(); clearValidation(); };
        reader.onerror = () => showValidation("文件读取失败，请重试");
        reader.readAsText(file, "UTF-8");
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
        return { sourceType: state.sourceType, content: els.markdownInput.value, fileName: els.fileName.textContent || null, mode: state.mode, documentType: els.documentType.value, audience: els.audience.value, tone: "专业、简洁", preserveCode: els.preserveCode.checked, preserveLinks: els.preserveLinks.checked, provider: modelParts[0], model: modelParts.slice(1).join(":"), deepThinking: false, maxTokens: 8192 };
    }

    function renderResult() {
        const result = state.result;
        els.resultEmpty.hidden = true;
        els.copyButton.disabled = false;
        els.downloadButton.disabled = false;
        els.resultSummary.hidden = false;
        els.summaryText.textContent = result.changeSummary || "已完成 Markdown 优化。";
        els.markdownPreview.innerHTML = renderMarkdown(result.optimizedContent);
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

    function renderMarkdown(source) {
        if (!source) return "";
        const blocks = [];
        let text = source.replace(/```([^\n]*)\n?([\s\S]*?)```/g, (_, language, code) => { const token = `@@CODE_${blocks.length}@@`; blocks.push(`<pre><code>${escapeHtml(code.replace(/\n$/, ""))}</code></pre>`); return token; });
        text = escapeHtml(text).replace(/^### (.+)$/gm, "<h3>$1</h3>").replace(/^## (.+)$/gm, "<h2>$1</h2>").replace(/^# (.+)$/gm, "<h1>$1</h1>").replace(/^> (.+)$/gm, "<blockquote>$1</blockquote>").replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>").replace(/`([^`\n]+)`/g, "<code>$1</code>");
        const output = []; let list = false; let paragraph = [];
        const flush = () => { if (paragraph.length) output.push(`<p>${paragraph.join("<br>")}</p>`); paragraph = []; };
        const closeList = () => { if (list) output.push("</ul>"); list = false; };
        text.split("\n").forEach((line) => { if (/^@@CODE_\d+@@$/.test(line) || /^<h[123]>/.test(line) || /^<blockquote>/.test(line)) { flush(); closeList(); output.push(line); } else if (/^[-*] /.test(line)) { flush(); if (!list) output.push("<ul>"); list = true; output.push(`<li>${line.slice(2)}</li>`); } else if (!line.trim()) { flush(); closeList(); } else { closeList(); paragraph.push(line); } });
        flush(); closeList(); return output.join("").replace(/@@CODE_(\d+)@@/g, (_, index) => blocks[Number(index)] || "");
    }

    async function copyResult() { if (!state.result) return; try { await navigator.clipboard.writeText(state.result.optimizedContent); showToast("已复制优化结果"); } catch (error) { showToast("复制失败，请手动选择文本"); } }

    function downloadResult() { if (!state.result) return; const blob = new Blob([state.result.optimizedContent], { type: "text/markdown;charset=utf-8" }); const url = URL.createObjectURL(blob); const link = document.createElement("a"); link.href = url; link.download = (els.fileName.textContent || "optimized-document.md").replace(/\.(markdown|md)$/i, "") + "-optimized.md"; link.click(); URL.revokeObjectURL(url); }

    async function requestJson(url, body) { const response = await fetch(url, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) }); const payload = await response.json().catch(() => null); if (!response.ok || (payload && payload.success === false)) throw new Error(payload && (payload.message || payload.msg) || "请求失败"); return payload && payload.data !== undefined ? payload.data : payload; }

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
