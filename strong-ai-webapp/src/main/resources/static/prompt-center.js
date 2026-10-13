(() => {
    "use strict";

    const THEME_KEY = "strong-ai.theme";
    const FAVORITES_KEY = "strong-ai.prompt-favorites";
    const PROMPT_DRAFT_KEY = "strong-ai.prompt-draft";
    const ENDPOINT = "/api/prompts";
    const CHAT_LIMIT = 12000;
    const CATEGORIES = [
        { code: "", name: "全部提示词" },
        { code: "JAVA_REVIEW", name: "Java 代码审查" },
        { code: "SQL_OPTIMIZE", name: "SQL 优化" },
        { code: "DOC_WRITING", name: "文档编写" },
        { code: "TROUBLESHOOT", name: "故障排查" }
    ];
    const state = {
        prompts: [],
        category: "",
        selected: null,
        editingPrompt: null,
        latestPublishedContent: null,
        editorVersionReady: false,
        editorBusy: false,
        detailBusy: false,
        historyBusy: false,
        favorites: new Set(),
        versions: [],
        variables: [],
        variableTemplate: "",
        listRequestId: 0,
        detailRequestId: 0,
        editorRequestId: 0,
        toastTimer: null
    };
    const elements = {};

    document.addEventListener("DOMContentLoaded", initialize);

    async function initialize() {
        [
            "themeButton", "newPromptButton", "categoryNav", "tagFilter", "searchInput", "resultCount",
            "listTitle", "listMessage", "clearFiltersButton", "promptList", "emptyState", "detailDialog",
            "detailTitle", "detailMeta", "detailDescription", "detailContent", "usePromptButton", "favoriteButton",
            "ratingControl", "copyPromptButton", "editPromptButton", "historyButton", "deletePromptButton",
            "editorDialog", "editorForm", "editorTitle", "editorTitleInput", "editorCategoryInput", "editorTagsInput",
            "editorDescriptionInput", "editorContentInput", "editorVariables", "editorError", "publishButton",
            "savePromptButton", "historyDialog", "versionList", "historyError", "versionPreview", "versionPreviewTitle",
            "versionPreviewContent", "variablesDialog", "variablesForm", "variablesFields", "variablesError", "toast"
        ].forEach((id) => { elements[id] = document.getElementById(id); });

        state.favorites = loadFavorites();
        applyInitialTheme();
        renderCategoryNav();
        elements.editorCategoryInput.innerHTML = CATEGORIES.filter((category) => category.code)
            .map((category) => `<option value="${category.code}">${category.name}</option>`).join("");
        bindEvents();
        refreshIcons();
        await loadPrompts();
    }

    function bindEvents() {
        elements.themeButton.addEventListener("click", toggleTheme);
        elements.newPromptButton.addEventListener("click", () => openEditor());
        elements.categoryNav.addEventListener("click", (event) => {
            const button = event.target.closest("[data-category]");
            if (!button) return;
            state.category = button.dataset.category;
            renderCategoryNav();
            loadPrompts();
        });
        const refreshList = debounce(loadPrompts, 220);
        elements.tagFilter.addEventListener("input", refreshList);
        elements.searchInput.addEventListener("input", refreshList);
        elements.clearFiltersButton.addEventListener("click", clearFilters);
        elements.promptList.addEventListener("click", handleCardSelection);
        elements.promptList.addEventListener("keydown", (event) => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                handleCardSelection(event);
            }
        });
        elements.usePromptButton.addEventListener("click", preparePromptDraft);
        elements.favoriteButton.addEventListener("click", toggleFavorite);
        elements.copyPromptButton.addEventListener("click", copySelectedPrompt);
        elements.editPromptButton.addEventListener("click", () => openEditor(state.selected));
        elements.historyButton.addEventListener("click", openHistory);
        elements.deletePromptButton.addEventListener("click", deleteSelectedPrompt);
        elements.ratingControl.addEventListener("click", (event) => {
            const star = event.target.closest("[data-score]");
            if (star) rateSelectedPrompt(Number(star.dataset.score));
        });
        elements.editorForm.addEventListener("submit", savePrompt);
        elements.editorContentInput.addEventListener("input", () => {
            renderVariables();
            updatePublishState();
        });
        elements.publishButton.addEventListener("click", publishEditedPrompt);
        elements.versionList.addEventListener("click", handleVersionAction);
        elements.variablesForm.addEventListener("submit", fillPromptDraft);
        document.addEventListener("click", (event) => {
            const close = event.target.closest("[data-close-dialog]");
            if (close) closeDialog(close.dataset.closeDialog);
        });
        elements.editorDialog.addEventListener("cancel", (event) => {
            if (state.editorBusy) event.preventDefault();
        });
        elements.detailDialog.addEventListener("cancel", (event) => {
            if (state.detailBusy) event.preventDefault();
        });
        elements.historyDialog.addEventListener("cancel", (event) => {
            if (state.historyBusy) event.preventDefault();
        });
    }

    async function loadPrompts() {
        const requestId = ++state.listRequestId;
        const params = new URLSearchParams();
        const tag = elements.tagFilter.value.trim();
        const query = elements.searchInput.value.trim();
        if (state.category) params.set("category", state.category);
        if (tag) params.set("tag", tag);
        if (query) params.set("q", query);
        elements.listMessage.textContent = "正在加载提示词…";
        elements.promptList.setAttribute("aria-busy", "true");
        elements.listTitle.textContent = categoryName(state.category);
        elements.clearFiltersButton.hidden = !(state.category || tag || query);
        try {
            const prompts = await requestJson(`${ENDPOINT}?${params.toString()}`);
            if (requestId !== state.listRequestId) return;
            state.prompts = prompts || [];
            elements.listMessage.textContent = "";
            renderList();
        } catch (error) {
            if (requestId === state.listRequestId) elements.listMessage.textContent = cleanError(error.message);
        } finally {
            if (requestId === state.listRequestId) elements.promptList.setAttribute("aria-busy", "false");
        }
    }

    function renderCategoryNav() {
        elements.categoryNav.innerHTML = CATEGORIES.map((category) => `
            <button class="category-button ${state.category === category.code ? "active" : ""}" type="button"
                    data-category="${category.code}" aria-pressed="${state.category === category.code}">
                ${category.name}
            </button>`).join("");
    }

    function renderList() {
        elements.resultCount.textContent = `${state.prompts.length} 条`;
        elements.promptList.innerHTML = state.prompts.map((prompt) => `
            <article class="prompt-card" data-prompt-id="${prompt.id}" tabindex="0" role="button"
                     aria-label="打开 ${escapeHtml(prompt.title)}">
                <div class="card-top">
                    <div><div class="card-category">${escapeHtml(categoryName(prompt.category))}</div><h3>${escapeHtml(prompt.title)}</h3></div>
                    <span class="card-version">${versionLabel(prompt.currentVersionNo)}</span>
                </div>
                <p class="card-description">${escapeHtml(prompt.description || "暂无说明")}</p>
                <div class="tag-list">${renderTags(prompt.tags)}</div>
                <div class="card-meta">
                    <span class="meta-item"><span class="stars" aria-hidden="true">${renderStars(prompt.rating)}</span>${prompt.rating ? `${prompt.rating}.0` : "未评分"}</span>
                    <span class="meta-item"><i data-lucide="heart"></i>${prompt.favoriteCount || 0}</span>
                </div>
            </article>`).join("");
        elements.emptyState.hidden = state.prompts.length > 0;
        elements.promptList.hidden = state.prompts.length === 0;
        refreshIcons();
    }

    function handleCardSelection(event) {
        const card = event.target.closest("[data-prompt-id]");
        if (card) openDetail(Number(card.dataset.promptId));
    }

    async function openDetail(id) {
        const requestId = ++state.detailRequestId;
        try {
            const prompt = await requestJson(`${ENDPOINT}/${id}`);
            if (requestId !== state.detailRequestId) return;
            state.selected = prompt;
            showDetail();
        } catch (error) {
            if (requestId === state.detailRequestId) showToast(cleanError(error.message));
        }
    }

    function showDetail() {
        renderDetail();
        clearDialogNotice(elements.detailDialog);
        if (!elements.detailDialog.open) elements.detailDialog.showModal();
    }

    function renderDetail() {
        const prompt = state.selected;
        const favorited = state.favorites.has(String(prompt.id));
        elements.detailTitle.textContent = prompt.title;
        elements.detailMeta.innerHTML = `<span class="tag-chip">${escapeHtml(categoryName(prompt.category))}</span>
            ${renderTags(prompt.tags)}<span class="tag-chip">${versionLabel(prompt.currentVersionNo)}</span>`;
        elements.detailDescription.textContent = prompt.description || "暂无说明";
        renderPreview(elements.detailContent, prompt.content || "暂无正文");
        elements.usePromptButton.disabled = !prompt.content || !prompt.content.trim();
        elements.favoriteButton.classList.toggle("favorited", favorited);
        elements.favoriteButton.setAttribute("aria-pressed", String(favorited));
        elements.favoriteButton.innerHTML = `<i data-lucide="heart"></i><span>${favorited ? "取消收藏" : "收藏"}（${prompt.favoriteCount || 0}）</span>`;
        elements.ratingControl.innerHTML = [1, 2, 3, 4, 5].map((score) => `
            <button class="rating-star ${score <= (prompt.rating || 0) ? "active" : ""}" type="button" data-score="${score}"
                    aria-label="评分 ${score} 星" aria-pressed="${score === prompt.rating}"><i data-lucide="star"></i></button>`).join("");
        updateDetailControls();
        refreshIcons();
    }

    async function openEditor(prompt) {
        const requestId = ++state.editorRequestId;
        state.editingPrompt = prompt || null;
        state.latestPublishedContent = null;
        state.editorVersionReady = !prompt || !prompt.currentVersionNo;
        elements.editorTitle.textContent = prompt ? "编辑提示词" : "新建提示词";
        elements.editorTitleInput.value = prompt ? prompt.title : "";
        elements.editorCategoryInput.value = prompt ? prompt.category : (state.category || "JAVA_REVIEW");
        elements.editorTagsInput.value = prompt ? prompt.tags || "" : "";
        elements.editorDescriptionInput.value = prompt ? prompt.description || "" : "";
        elements.editorContentInput.value = prompt ? prompt.content || "" : "";
        elements.editorError.textContent = "";
        renderVariables();
        setEditorBusy(false);
        closeDialog("detailDialog");
        clearDialogNotice(elements.editorDialog);
        if (!elements.editorDialog.open) elements.editorDialog.showModal();
        if (prompt && prompt.currentVersionNo) {
            try {
                const latest = await requestJson(`${ENDPOINT}/${prompt.id}/versions/${prompt.currentVersionNo}`);
                if (requestId !== state.editorRequestId) return;
                state.latestPublishedContent = latest.content;
                state.editorVersionReady = true;
                updatePublishState();
            } catch (error) {
                if (requestId === state.editorRequestId) elements.editorError.textContent = cleanError(error.message);
            }
        }
    }

    async function persistEditor() {
        const body = readEditorForm();
        const prompt = state.editingPrompt;
        const saved = await requestJson(prompt ? `${ENDPOINT}/${prompt.id}` : ENDPOINT, {
            method: prompt ? "PUT" : "POST",
            body: JSON.stringify(body)
        });
        state.editingPrompt = saved;
        state.selected = saved;
        return saved;
    }

    async function savePrompt(event) {
        event.preventDefault();
        if (state.editorBusy || !elements.editorForm.reportValidity()) return;
        setEditorBusy(true);
        elements.editorError.textContent = "";
        try {
            await persistEditor();
            closeDialog("editorDialog");
            showDetail();
            showToast("已保存提示词");
            await loadPrompts();
        } catch (error) {
            elements.editorError.textContent = cleanError(error.message);
        } finally {
            setEditorBusy(false);
        }
    }

    async function publishEditedPrompt() {
        if (state.editorBusy || elements.publishButton.disabled || !elements.editorForm.reportValidity()) return;
        setEditorBusy(true);
        elements.editorError.textContent = "";
        try {
            const saved = await persistEditor();
            const published = await requestJson(`${ENDPOINT}/${saved.id}/versions`, { method: "POST" });
            state.editingPrompt = published;
            state.selected = published;
            state.latestPublishedContent = published.content;
            showToast(`已发布 v${published.currentVersionNo}`);
            await loadPrompts();
        } catch (error) {
            elements.editorError.textContent = cleanError(error.message);
        } finally {
            setEditorBusy(false);
        }
    }

    function updatePublishState() {
        const content = elements.editorContentInput.value;
        elements.publishButton.disabled = state.editorBusy || !state.editingPrompt || !state.editorVersionReady
            || !content.trim() || content === state.latestPublishedContent;
    }

    function setEditorBusy(busy) {
        state.editorBusy = busy;
        elements.editorForm.querySelectorAll("input, textarea, select, button")
            .forEach((control) => { control.disabled = busy; });
        updatePublishState();
    }

    async function openHistory() {
        if (!state.selected) return;
        elements.historyError.textContent = "";
        elements.versionList.textContent = "正在加载版本…";
        elements.versionPreview.hidden = true;
        clearDialogNotice(elements.historyDialog);
        if (!elements.historyDialog.open) elements.historyDialog.showModal();
        try {
            state.versions = await requestJson(`${ENDPOINT}/${state.selected.id}/versions`) || [];
            renderVersions();
        } catch (error) {
            elements.versionList.textContent = "";
            elements.historyError.textContent = cleanError(error.message);
        }
    }

    function renderVersions() {
        elements.versionList.innerHTML = state.versions.length ? state.versions.map((version) => `
            <div class="version-item">
                <div class="version-info"><strong>版本 v${version.versionNo}</strong><small>${formatDate(version.createdAt)}</small></div>
                <div class="version-actions">
                    <button type="button" data-version-action="view" data-version-no="${version.versionNo}">查看</button>
                    <button type="button" data-version-action="copy" data-version-no="${version.versionNo}">复制</button>
                    <button type="button" data-version-action="restore" data-version-no="${version.versionNo}">恢复</button>
                </div>
            </div>`).join("") : `<p class="dialog-note">暂无已发布版本。</p>`;
    }

    async function handleVersionAction(event) {
        const button = event.target.closest("[data-version-action]");
        if (!button || !state.selected || state.historyBusy) return;
        const versionNo = Number(button.dataset.versionNo);
        const action = button.dataset.versionAction;
        const url = `${ENDPOINT}/${state.selected.id}/versions/${versionNo}`;
        if (action === "restore" && !window.confirm(`恢复 v${versionNo} 将覆盖当前工作副本，包括未发布的修改；不会产生新版本。继续吗？`)) return;
        state.historyBusy = true;
        elements.historyDialog.querySelectorAll("button").forEach((control) => { control.disabled = true; });
        elements.historyError.textContent = "";
        try {
            if (action === "restore") {
                state.selected = await requestJson(`${url}/restore`, { method: "POST" });
                closeDialog("historyDialog");
                renderDetail();
                showToast(`已恢复 v${versionNo} 为工作副本，未发布新版本`);
                await loadPrompts();
            } else {
                const version = await requestJson(url);
                if (action === "view") {
                    elements.versionPreviewTitle.textContent = `版本 v${versionNo} · ${formatDate(version.createdAt)}`;
                    renderPreview(elements.versionPreviewContent, version.content || "暂无正文");
                    elements.versionPreview.hidden = false;
                } else {
                    await copyText(version.content || "");
                    showToast(`已复制 v${versionNo}`);
                }
            }
        } catch (error) {
            elements.historyError.textContent = cleanError(error.message);
        } finally {
            state.historyBusy = false;
            elements.historyDialog.querySelectorAll("button").forEach((control) => { control.disabled = false; });
        }
    }

    async function rateSelectedPrompt(score) {
        if (!state.selected || state.detailBusy) return;
        state.detailBusy = true;
        updateDetailControls();
        try {
            state.selected = await requestJson(`${ENDPOINT}/${state.selected.id}/rating`, {
                method: "PUT", body: JSON.stringify({ score })
            });
            renderDetail();
            showToast("全局评分已更新");
            await loadPrompts();
        } catch (error) {
            showToast(cleanError(error.message));
        } finally {
            state.detailBusy = false;
            updateDetailControls();
        }
    }

    async function toggleFavorite() {
        if (!state.selected || state.detailBusy) return;
        const id = String(state.selected.id);
        const favorited = !state.favorites.has(id);
        state.detailBusy = true;
        updateDetailControls();
        try {
            state.selected = await requestJson(`${ENDPOINT}/${id}/favorite`, {
                method: "POST", body: JSON.stringify({ favorited })
            });
            if (favorited) state.favorites.add(id);
            else state.favorites.delete(id);
            const remembered = persistFavorites();
            renderDetail();
            showToast(remembered ? (favorited ? "已收藏" : "已取消收藏") : "收藏数已更新，但浏览器无法保存收藏开关状态");
            await loadPrompts();
        } catch (error) {
            showToast(cleanError(error.message));
        } finally {
            state.detailBusy = false;
            updateDetailControls();
        }
    }

    async function deleteSelectedPrompt() {
        if (!state.selected || state.detailBusy) return;
        if (!window.confirm(`确定删除“${state.selected.title}”吗？版本历史也会一并删除。`)) return;
        state.detailBusy = true;
        updateDetailControls();
        const id = String(state.selected.id);
        try {
            await requestJson(`${ENDPOINT}/${id}`, { method: "DELETE" });
            state.favorites.delete(id);
            persistFavorites();
            closeDialog("detailDialog");
            state.selected = null;
            showToast("已删除提示词和版本历史");
            await loadPrompts();
        } catch (error) {
            showToast(cleanError(error.message));
        } finally {
            state.detailBusy = false;
            updateDetailControls();
        }
    }

    function updateDetailControls() {
        elements.detailDialog.querySelectorAll("button")
            .forEach((button) => { button.disabled = state.detailBusy; });
        elements.usePromptButton.disabled = state.detailBusy || !state.selected || !(state.selected.content || "").trim();
    }

    function preparePromptDraft() {
        if (!state.selected) return;
        state.variableTemplate = state.selected.content || "";
        state.variables = extractVariables(state.variableTemplate);
        if (!state.variables.length) {
            storePromptDraft(state.variableTemplate);
            return;
        }
        elements.variablesFields.innerHTML = state.variables.map((name, index) => `
            <div class="variable-field">
                <label for="prompt-variable-${index}">${escapeHtml(name)}</label>
                <textarea id="prompt-variable-${index}" rows="3" maxlength="12000" required
                          placeholder="填写 ${escapeHtml(name)}"></textarea>
            </div>`).join("");
        elements.variablesError.textContent = "";
        clearDialogNotice(elements.variablesDialog);
        elements.variablesDialog.showModal();
    }

    function fillPromptDraft(event) {
        event.preventDefault();
        const fields = Array.from(elements.variablesFields.querySelectorAll("textarea"));
        const values = new Map(state.variables.map((name, index) => [name, fields[index].value]));
        const content = state.variableTemplate.replace(variablePattern(), (placeholder, name) => {
            return values.has(name.trim()) ? values.get(name.trim()) : placeholder;
        });
        storePromptDraft(content);
    }

    function storePromptDraft(content) {
        if (!content.trim()) {
            showDraftError("提示词正文不能为空");
            return;
        }
        if (content.length > CHAT_LIMIT) {
            showDraftError(`渲染后为 ${content.length} 字符，超过首页 ${CHAT_LIMIT} 字符限制，请缩短正文或变量值。`);
            return;
        }
        try {
            sessionStorage.setItem(PROMPT_DRAFT_KEY, content);
            window.location.assign("/");
        } catch (error) {
            showDraftError("无法保存对话草稿，请允许浏览器存储或复制正文后粘贴到聊天。");
        }
    }

    function showDraftError(message) {
        if (elements.variablesDialog.open) elements.variablesError.textContent = message;
        else showToast(message);
    }

    function renderVariables() {
        const variables = extractVariables(elements.editorContentInput.value);
        elements.editorVariables.innerHTML = variables.length
            ? variables.map((name) => `<span class="tag-chip">{{${escapeHtml(name)}}}</span>`).join("")
            : `<span class="muted-chip">暂无变量</span>`;
    }

    function variablePattern() {
        return /\{\{\s*([^{}\r\n]+?)\s*\}\}/g;
    }

    function extractVariables(content) {
        const names = new Set();
        content.replace(variablePattern(), (placeholder, name) => {
            if (name.trim()) names.add(name.trim());
            return placeholder;
        });
        return Array.from(names);
    }

    function renderPreview(container, content) {
        container.innerHTML = escapeHtml(content).replace(variablePattern(), '<mark class="prompt-variable">$&</mark>');
    }

    function readEditorForm() {
        return {
            title: elements.editorTitleInput.value,
            category: elements.editorCategoryInput.value,
            tags: elements.editorTagsInput.value,
            description: elements.editorDescriptionInput.value,
            content: elements.editorContentInput.value
        };
    }

    function renderStars(rating) {
        return [1, 2, 3, 4, 5].map((score) => score <= (rating || 0) ? "★" : "☆").join("");
    }

    function renderTags(tags) {
        return (tags || "").split(",").filter((tag) => tag.trim())
            .map((tag) => `<span class="tag-chip">${escapeHtml(tag.trim())}</span>`).join("");
    }

    function categoryName(code) {
        const category = CATEGORIES.find((item) => item.code === code);
        return category ? category.name : code;
    }

    function versionLabel(versionNo) {
        return versionNo ? `v${versionNo}` : "未发布";
    }

    function clearFilters() {
        state.category = "";
        elements.tagFilter.value = "";
        elements.searchInput.value = "";
        renderCategoryNav();
        loadPrompts();
    }

    function closeDialog(id) {
        const dialog = document.getElementById(id);
        if (dialog && dialog.open) dialog.close();
    }

    function applyInitialTheme() {
        let saved;
        try {
            saved = localStorage.getItem(THEME_KEY);
        } catch (error) {
            saved = null;
        }
        const preferred = window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
        document.documentElement.dataset.theme = saved || preferred;
        updateThemeIcon();
    }

    function toggleTheme() {
        const next = document.documentElement.dataset.theme === "dark" ? "light" : "dark";
        document.documentElement.dataset.theme = next;
        try {
            localStorage.setItem(THEME_KEY, next);
        } catch (error) {
            showToast("主题已切换，但浏览器无法保存偏好");
        }
        updateThemeIcon();
    }

    function updateThemeIcon() {
        const dark = document.documentElement.dataset.theme === "dark";
        elements.themeButton.innerHTML = `<i data-lucide="${dark ? "sun" : "moon"}"></i>`;
        elements.themeButton.setAttribute("aria-label", dark ? "切换浅色模式" : "切换深色模式");
        refreshIcons();
    }

    function loadFavorites() {
        try {
            const stored = JSON.parse(localStorage.getItem(FAVORITES_KEY) || "[]");
            return new Set(Array.isArray(stored) ? stored.map(String) : []);
        } catch (error) {
            return new Set();
        }
    }

    function persistFavorites() {
        try {
            localStorage.setItem(FAVORITES_KEY, JSON.stringify(Array.from(state.favorites)));
            return true;
        } catch (error) {
            return false;
        }
    }

    function formatDate(value) {
        return value ? new Date(value).toLocaleString("zh-CN", {
            year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit"
        }) : "未知时间";
    }

    async function copySelectedPrompt() {
        if (!state.selected) return;
        try {
            await copyText(state.selected.content || "");
            showToast("已复制提示词正文");
        } catch (error) {
            showToast("复制失败，请手动选择正文复制");
        }
    }

    async function copyText(text) {
        if (navigator.clipboard && window.isSecureContext) return navigator.clipboard.writeText(text);
        const area = document.createElement("textarea");
        area.value = text;
        area.style.position = "fixed";
        area.style.opacity = "0";
        const dialog = Array.from(document.querySelectorAll("dialog[open]")).pop();
        (dialog || document.body).appendChild(area);
        try {
            area.select();
            if (!document.execCommand("copy")) throw new Error("复制失败");
        } finally {
            area.remove();
        }
    }

    function debounce(callback, delay) {
        let timer;
        return () => {
            window.clearTimeout(timer);
            timer = window.setTimeout(callback, delay);
        };
    }

    function escapeHtml(value) {
        return String(value == null ? "" : value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    async function requestJson(url, options = {}) {
        const response = await fetch(url, {
            ...options,
            headers: { "Content-Type": "application/json", ...(options.headers || {}) }
        });
        if (!response.ok) throw new Error(await responseError(response));
        const result = await response.json();
        if (result && result.success === false) throw new Error(result.message || "请求失败");
        return result ? result.data : null;
    }

    async function responseError(response) {
        const text = await response.text();
        if (!text) return `请求失败（HTTP ${response.status}）`;
        try {
            const json = JSON.parse(text);
            return json.message || json.msg || json.error || text;
        } catch (error) {
            return text;
        }
    }

    function cleanError(message) {
        if (!message) return "请求失败，请稍后重试";
        return message.length > 280 ? `${message.slice(0, 280)}…` : message;
    }

    function clearDialogNotice(dialog) {
        const notice = dialog.querySelector(".dialog-notice");
        if (notice) notice.remove();
    }

    function showToast(message) {
        window.clearTimeout(state.toastTimer);
        elements.toast.textContent = message;
        elements.toast.classList.add("visible");
        const dialog = Array.from(document.querySelectorAll("dialog[open]")).pop();
        if (dialog) {
            let notice = dialog.querySelector(".dialog-notice");
            if (!notice) {
                notice = document.createElement("p");
                notice.className = "dialog-notice";
                notice.setAttribute("role", "status");
                dialog.appendChild(notice);
            }
            notice.textContent = message;
        }
        state.toastTimer = window.setTimeout(() => elements.toast.classList.remove("visible"), 2500);
    }

    function refreshIcons() {
        if (window.lucide) window.lucide.createIcons({ attrs: { "aria-hidden": "true" } });
    }
})();
