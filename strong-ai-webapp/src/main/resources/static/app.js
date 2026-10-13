(() => {
    "use strict";

    const THEME_KEY = "strong-ai.theme";
    const CONVERSATIONS_ENDPOINT = "/api/conversations";

    const MODEL_GROUPS = [
        {
            provider: "openai",
            name: "OpenAI",
            mark: "O",
            models: [
                { id: "gpt-5.6-sol", name: "GPT-5.6 Sol", description: "复杂推理与代码任务" },
                { id: "gpt-5.6-luna", name: "GPT-5.6 Luna", description: "快速推理与日常任务" },
                { id: "gpt-4o", name: "GPT-4o", description: "通用多模态模型", supportsImage: true },
                { id: "gpt-4o-mini", name: "GPT-4o mini", description: "快速、轻量的日常问答", supportsImage: true }
            ]
        },
        {
            provider: "xai",
            name: "xAI",
            mark: "G",
            models: [
                { id: "grok-4.7", name: "Grok 4.7", description: "通用对话与复杂推理" }
            ]
        },
        {
            provider: "deepseek",
            name: "DeepSeek",
            mark: "D",
            models: [
                { id: "deepseek-v4.1-flash", name: "DeepSeek V4.1 Flash", description: "通用对话与内容生成" },
                { id: "deepseek-reasoner", name: "DeepSeek Reasoner", description: "深度推理与复杂分析" }
            ]
        }
    ];

    const DEFAULT_MODEL = { provider: "deepseek", model: "deepseek-v4.1-flash" };
    const state = {
        conversations: [],
        activeId: null,
        search: "",
        streaming: false,
        controller: null,
        toastTimer: null,
        drafts: {}
    };

    const elements = {};

    document.addEventListener("DOMContentLoaded", () => { initialize(); });

    async function initialize() {
        [
            "sidebar", "sidebarScrim", "sidebarOpen", "sidebarClose", "newChatButton",
            "sessionSearch", "sessionList", "modelTrigger", "modelMenu", "providerMark",
            "selectedModelName", "selectedProviderName", "clearChatButton", "themeButton",
            "chatViewport", "messageColumn", "composer", "messageInput", "composerModelChip",
            "characterCount", "thinkingToggle", "sendButton", "stopButton", "toast",
            "imageButton", "imageInput", "attachmentDraft"
        ].forEach((id) => { elements[id] = document.getElementById(id); });

        applyInitialTheme();
        bindEvents();
        try {
            await ensureConversation();
            renderModelMenu();
            renderAll();
        } catch (error) {
            showToast(cleanError(error.message));
        }
        restorePromptDraft();
        refreshIcons();
    }

    function bindEvents() {
        elements.newChatButton.addEventListener("click", createConversation);
        elements.sidebarOpen.addEventListener("click", () => setSidebar(true));
        elements.sidebarClose.addEventListener("click", () => setSidebar(false));
        elements.sidebarScrim.addEventListener("click", () => setSidebar(false));
        elements.sessionSearch.addEventListener("input", (event) => {
            state.search = event.target.value.trim().toLowerCase();
            renderSessions();
        });
        elements.sessionList.addEventListener("click", handleSessionClick);
        elements.modelTrigger.addEventListener("click", toggleModelMenu);
        elements.modelMenu.addEventListener("click", handleModelSelection);
        elements.clearChatButton.addEventListener("click", clearCurrentConversation);
        elements.themeButton.addEventListener("click", toggleTheme);
        elements.thinkingToggle.addEventListener("click", toggleDeepThinking);
        elements.composer.addEventListener("submit", handleSubmit);
        elements.messageInput.addEventListener("input", updateComposer);
        elements.messageInput.addEventListener("keydown", handleComposerKeydown);
        elements.imageButton.addEventListener("click", () => elements.imageInput.click());
        elements.imageInput.addEventListener("change", handleImageSelection);
        elements.composer.addEventListener("paste", handleComposerPaste);
        elements.composer.addEventListener("dragover", (event) => {
            if (event.dataTransfer && Array.from(event.dataTransfer.items).some((item) => item.type.startsWith("image/"))) {
                event.preventDefault();
                elements.composer.classList.add("dragging");
            }
        });
        elements.composer.addEventListener("dragleave", () => elements.composer.classList.remove("dragging"));
        elements.composer.addEventListener("drop", handleImageDrop);
        elements.stopButton.addEventListener("click", stopGeneration);
        elements.messageColumn.addEventListener("click", handleMessageAction);

        document.addEventListener("click", (event) => {
            if (!event.target.closest(".model-picker")) {
                closeModelMenu();
            }
        });
        document.addEventListener("keydown", handleGlobalKeydown);
    }

    async function ensureConversation() {
        state.conversations = await requestJson(CONVERSATIONS_ENDPOINT);
        if (state.conversations.length) {
            state.activeId = state.conversations[0].id;
            return;
        }
        const conversation = await requestJson(CONVERSATIONS_ENDPOINT, {
            method: "POST",
            body: JSON.stringify(DEFAULT_MODEL)
        });
        state.conversations = [conversation];
        state.activeId = conversation.id;
    }

    async function createConversation() {
        if (state.streaming) {
            stopGeneration();
        }
        const current = activeConversation();
        if (current && current.messages.length === 0) {
            elements.messageInput.focus();
            setSidebar(false);
            return;
        }
        const conversation = await requestJson(CONVERSATIONS_ENDPOINT, {
            method: "POST",
            body: JSON.stringify(DEFAULT_MODEL)
        });
        state.conversations.unshift(conversation);
        state.activeId = conversation.id;
        renderAll();
        setSidebar(false);
        elements.messageInput.focus();
    }

    function activeConversation() {
        return state.conversations.find((item) => item.id === state.activeId);
    }

    async function reloadConversations() {
        state.conversations = await requestJson(CONVERSATIONS_ENDPOINT);
        if (!state.conversations.some((conversation) => conversation.id === state.activeId)) {
            state.activeId = state.conversations.length ? state.conversations[0].id : null;
        }
        renderSessions();
        renderSelectedModel();
        renderModelMenu();
    }

    function replaceConversation(updated) {
        const index = state.conversations.findIndex((conversation) => conversation.id === updated.id);
        if (index >= 0) state.conversations[index] = updated;
    }

    function renderAll() {
        renderSessions();
        renderSelectedModel();
        renderMessages();
        updateComposer();
        renderAttachmentDraft();
    }

    function restorePromptDraft() {
        try {
            const draft = sessionStorage.getItem("strong-ai.prompt-draft");
            if (draft === null) return;
            elements.messageInput.value = draft;
            updateComposer();
            sessionStorage.removeItem("strong-ai.prompt-draft");
            elements.messageInput.focus();
        } catch (error) {
            showToast("无法读取提示词草稿，请检查浏览器存储设置");
        }
    }

    function renderSessions() {
        const filtered = state.conversations
            .filter((conversation) => conversation.title.toLowerCase().includes(state.search))
            .sort((a, b) => b.updatedAt - a.updatedAt);

        if (!filtered.length) {
            elements.sessionList.innerHTML = `<div class="sessions-empty">没有找到相关会话</div>`;
            return;
        }

        const groups = groupConversations(filtered);
        elements.sessionList.innerHTML = groups.map((group) => `
            <section class="session-group">
                <h2 class="session-group-title">${group.label}</h2>
                ${group.items.map((conversation) => `
                    <div class="session-item ${conversation.id === state.activeId ? "active" : ""}">
                        <button class="session-select" type="button" data-session-id="${conversation.id}" title="${escapeHtml(conversation.title)}">
                            ${escapeHtml(conversation.title)}
                        </button>
                        <button class="session-delete" type="button" data-delete-session="${conversation.id}" aria-label="删除会话" title="删除">
                            <i data-lucide="trash-2"></i>
                        </button>
                    </div>
                `).join("")}
            </section>
        `).join("");
        refreshIcons();
    }

    function groupConversations(conversations) {
        const buckets = [
            { label: "今天", items: [] },
            { label: "昨天", items: [] },
            { label: "近 7 天", items: [] },
            { label: "更早", items: [] }
        ];
        const startToday = new Date();
        startToday.setHours(0, 0, 0, 0);
        const day = 24 * 60 * 60 * 1000;

        conversations.forEach((conversation) => {
            const age = startToday.getTime() - conversation.updatedAt;
            if (age < 0) buckets[0].items.push(conversation);
            else if (age < day) buckets[1].items.push(conversation);
            else if (age < day * 6) buckets[2].items.push(conversation);
            else buckets[3].items.push(conversation);
        });
        return buckets.filter((bucket) => bucket.items.length);
    }

    async function handleSessionClick(event) {
        const deleteButton = event.target.closest("[data-delete-session]");
        if (deleteButton) {
            deleteConversation(deleteButton.dataset.deleteSession);
            return;
        }
        const sessionButton = event.target.closest("[data-session-id]");
        if (!sessionButton || sessionButton.dataset.sessionId === state.activeId) return;
        if (state.streaming) stopGeneration();
        state.activeId = sessionButton.dataset.sessionId;
        renderAll();
        setSidebar(false);
    }

    async function deleteConversation(id) {
        const target = state.conversations.find((item) => item.id === id);
        if (!target) return;
        if (target.messages.length && !window.confirm(`删除“${target.title}”？此操作无法撤销。`)) return;
        if (state.streaming && state.activeId === id) stopGeneration();
        await requestJson(`${CONVERSATIONS_ENDPOINT}/${encodeURIComponent(id)}`, { method: "DELETE" });
        state.conversations = state.conversations.filter((item) => item.id !== id);
        if (!state.conversations.length) {
            const conversation = await requestJson(CONVERSATIONS_ENDPOINT, {
                method: "POST",
                body: JSON.stringify(DEFAULT_MODEL)
            });
            state.conversations.push(conversation);
        }
        if (state.activeId === id) state.activeId = state.conversations[0].id;
        renderAll();
        showToast("会话已删除");
    }

    async function clearCurrentConversation() {
        const conversation = activeConversation();
        if (!conversation || !conversation.messages.length) return;
        if (!window.confirm("清空当前会话中的全部消息？")) return;
        if (state.streaming) stopGeneration();
        const updated = await requestJson(`${CONVERSATIONS_ENDPOINT}/${encodeURIComponent(conversation.id)}/messages`, {
            method: "DELETE"
        });
        replaceConversation(updated);
        renderAll();
    }

    function renderModelMenu() {
        const conversation = activeConversation();
        elements.modelMenu.innerHTML = MODEL_GROUPS.map((group) => `
            <section class="model-menu-group">
                <div class="model-menu-heading">${group.name}</div>
                ${group.models.map((model) => {
                    const selected = conversation && conversation.provider === group.provider && conversation.model === model.id;
                    return `
                        <button class="model-option ${selected ? "selected" : ""}" type="button" role="option"
                                aria-selected="${selected}" data-provider="${group.provider}" data-model="${model.id}">
                            <span class="provider-mark ${group.provider === "deepseek" ? "deepseek" : ""}">${group.mark}</span>
                            <span class="model-option-copy">
                                <strong>${model.name}</strong>
                                <small>${model.description}</small>
                            </span>
                            ${selected ? `<i data-lucide="check"></i>` : ""}
                        </button>
                    `;
                }).join("")}
            </section>
        `).join("");
        refreshIcons();
    }

    function toggleModelMenu() {
        const willOpen = elements.modelMenu.hidden;
        elements.modelMenu.hidden = !willOpen;
        elements.modelTrigger.setAttribute("aria-expanded", String(willOpen));
    }

    function closeModelMenu() {
        elements.modelMenu.hidden = true;
        elements.modelTrigger.setAttribute("aria-expanded", "false");
    }

    async function handleModelSelection(event) {
        const option = event.target.closest("[data-model]");
        if (!option || state.streaming) return;
        const conversation = activeConversation();
        if (conversationHasImage(conversation) && !modelSupportsImage(option.dataset.provider, option.dataset.model)) {
            showToast("当前会话包含图片，不能切换到不支持图片的模型");
            return;
        }
        const updated = await requestJson(`${CONVERSATIONS_ENDPOINT}/${encodeURIComponent(conversation.id)}`, {
            method: "PUT",
            body: JSON.stringify({ provider: option.dataset.provider, model: option.dataset.model,
                deepThinking: conversation.deepThinking })
        });
        replaceConversation(updated);
        renderSelectedModel();
        renderModelMenu();
        closeModelMenu();
    }

    function renderSelectedModel() {
        const conversation = activeConversation();
        const modelInfo = findModel(conversation.provider, conversation.model);
        elements.providerMark.textContent = modelInfo.group.mark;
        elements.providerMark.classList.toggle("deepseek", conversation.provider === "deepseek");
        elements.selectedModelName.textContent = modelInfo.model.name;
        elements.selectedProviderName.textContent = modelInfo.group.name;
        elements.composerModelChip.textContent = `${modelInfo.group.name} · ${modelInfo.model.name}`;
        renderDeepThinkingToggle(conversation);
    }

    function renderDeepThinkingToggle(conversation) {
        const supported = supportsDeepThinking(conversation);
        const enabled = Boolean(conversation.deepThinking);
        elements.thinkingToggle.hidden = !supported;
        elements.thinkingToggle.disabled = state.streaming;
        elements.thinkingToggle.classList.toggle("active", enabled);
        elements.thinkingToggle.setAttribute("aria-pressed", String(enabled));
        elements.thinkingToggle.setAttribute("aria-label", enabled ? "关闭深度思考" : "开启深度思考");
        elements.thinkingToggle.title = enabled ? "关闭深度思考" : "开启深度思考";
    }

    function supportsDeepThinking(conversation) {
        const supportsOpenAi = conversation.provider === "openai"
            && conversation.model.startsWith("gpt-5.6");
        const supportsDeepSeek = conversation.provider === "deepseek" && conversation.model === "deepseek-v4.1-flash";
        return supportsOpenAi || supportsDeepSeek;
    }

    async function toggleDeepThinking() {
        const conversation = activeConversation();
        if (!conversation || state.streaming || !supportsDeepThinking(conversation)) return;
        const updated = await requestJson(`${CONVERSATIONS_ENDPOINT}/${encodeURIComponent(conversation.id)}`, {
            method: "PUT",
            body: JSON.stringify({ provider: conversation.provider, model: conversation.model,
                deepThinking: !conversation.deepThinking })
        });
        replaceConversation(updated);
        renderSelectedModel();
    }

    function findModel(provider, modelId) {
        const group = MODEL_GROUPS.find((item) => item.provider === provider) || MODEL_GROUPS[0];
        const model = group.models.find((item) => item.id === modelId) || group.models[0];
        return { group, model };
    }

    function renderMessages() {
        const conversation = activeConversation();
        if (!conversation.messages.length) {
            elements.messageColumn.innerHTML = welcomeTemplate();
            refreshIcons();
            return;
        }
        elements.messageColumn.innerHTML = conversation.messages.map(messageTemplate).join("");
        refreshIcons();
        scrollToBottom(false);
    }

    function welcomeTemplate() {
        const name = findModel(activeConversation().provider, activeConversation().model).model.name;
        return `
            <div class="welcome">
                <div class="welcome-mark">S</div>
                <h1>今天想一起解决什么？</h1>
                <p>当前使用 ${escapeHtml(name)}，可以直接开始提问。</p>
                <div class="suggestion-grid">
                    <button class="suggestion" type="button" data-suggestion="帮我梳理一个复杂问题，并给出清晰的行动步骤">
                        <i data-lucide="route"></i><span>梳理复杂问题，并给出清晰的行动步骤</span>
                    </button>
                    <button class="suggestion" type="button" data-suggestion="审查下面这段代码，找出潜在问题并给出修改建议：\n">
                        <i data-lucide="code-2"></i><span>审查代码，找出潜在问题与改进方向</span>
                    </button>
                    <button class="suggestion" type="button" data-suggestion="将下面的内容整理成结构清晰的摘要：\n">
                        <i data-lucide="notebook-tabs"></i><span>把长内容整理成结构清晰的摘要</span>
                    </button>
                    <button class="suggestion" type="button" data-suggestion="从不同角度分析这个想法的优势、风险和验证方式：\n">
                        <i data-lucide="scan-search"></i><span>分析一个想法的优势、风险与验证方式</span>
                    </button>
                </div>
            </div>
        `;
    }

    function messageTemplate(message, index) {
        const isUser = message.role === "user";
        const statusClass = message.status === "error" ? "error" : "";
        const attachments = (message.attachments || []).map((attachment) => `
            <a class="message-attachment" href="${escapeHtml(attachment.previewUrl)}" target="_blank" rel="noreferrer">
                <img src="${escapeHtml(attachment.previewUrl)}" alt="${escapeHtml(attachment.name || "图片")}" loading="lazy">
                <span>${escapeHtml(attachment.name || "图片")}</span>
            </a>
        `).join("");
        const body = message.status === "error"
            ? `<div class="message-error">${escapeHtml(message.error || "请求失败，请稍后重试。")}</div>`
            : `${attachments}${isUser ? escapeHtml(message.content) : renderMarkdown(message.content)}`;
        const reasoning = !isUser && message.reasoning
            ? `<details class="reasoning" ${message.status === "streaming" && !message.content ? "open" : ""}>
                   <summary>思考过程</summary>
                   <div class="reasoning-content">${escapeHtml(message.reasoning)}</div>
               </details>`
            : "";
        const cursor = message.status === "streaming" ? `<span class="stream-cursor" aria-label="正在生成"></span>` : "";
        const model = !isUser && message.model ? `<span class="message-model">${escapeHtml(findModel(message.provider, message.model).model.name)}</span>` : "";
        const actions = message.status === "streaming" ? "" : `
            <div class="message-actions">
                ${message.content ? `<button class="message-action" type="button" data-copy-message="${index}" aria-label="复制消息" title="复制"><i data-lucide="copy"></i></button>` : ""}
                ${!isUser ? `<button class="message-action" type="button" data-retry-message="${index}" aria-label="重新生成" title="重新生成"><i data-lucide="refresh-cw"></i></button>` : ""}
            </div>`;
        return `
            <article class="message ${message.role} ${statusClass}" data-message-id="${message.id}">
                <div class="message-avatar">${isUser ? "你" : "S"}</div>
                <div class="message-main">
                    <div class="message-meta"><span class="message-author">${isUser ? "你" : "Strong AI"}</span>${model}</div>
                    ${reasoning}
                    <div class="message-body">${body}${cursor}</div>
                    ${actions}
                </div>
            </article>
        `;
    }

    function handleMessageAction(event) {
        const suggestion = event.target.closest("[data-suggestion]");
        if (suggestion) {
            elements.messageInput.value = suggestion.dataset.suggestion;
            updateComposer();
            elements.messageInput.focus();
            return;
        }
        const copyButton = event.target.closest("[data-copy-message]");
        if (copyButton) {
            const message = activeConversation().messages[Number(copyButton.dataset.copyMessage)];
            navigator.clipboard.writeText(message.content)
                .then(() => showToast("已复制到剪贴板"))
                .catch(() => showToast("复制失败，请手动选择文本"));
            return;
        }
        const retryButton = event.target.closest("[data-retry-message]");
        if (retryButton && !state.streaming) retryMessage(Number(retryButton.dataset.retryMessage));
    }

    function retryMessage(index) {
        const conversation = activeConversation();
        if (!conversation || index < 0 || conversation.messages[index].role !== "assistant") return;
        generateReply(conversation, conversation.messages[index].id);
    }

    async function handleSubmit(event) {
        event.preventDefault();
        const content = elements.messageInput.value.trim();
        const conversation = activeConversation();
        const draft = currentDraft();
        if ((!content && !draft) || state.streaming) return;
        if (draft && !modelSupportsImage(conversation.provider, conversation.model)) {
            showToast("当前模型不支持图片输入，请切换到 GPT-4o 或 GPT-4o mini");
            return;
        }

        elements.messageInput.value = "";
        updateComposer();
        await generateReply(conversation, null, content, draft);
    }

    async function generateReply(conversation, regenerateMessageId, userContent, draft) {
        let endpoint;
        let requestOptions = { method: "POST", headers: { "Content-Type": "application/json", "Accept": "text/event-stream" } };
        if (regenerateMessageId) {
            endpoint = `${CONVERSATIONS_ENDPOINT}/${encodeURIComponent(conversation.id)}/messages/${encodeURIComponent(regenerateMessageId)}/regenerate/stream`;
        } else {
            endpoint = `${CONVERSATIONS_ENDPOINT}/${encodeURIComponent(conversation.id)}/messages/stream`;
            if (draft) {
                try {
                    draft.attachmentId = draft.attachmentId || await uploadAttachment(conversation.id, draft);
                } catch (error) {
                    elements.messageInput.value = userContent || "";
                    updateComposer();
                    showToast(cleanError(error.message));
                    return;
                }
            }
            requestOptions.body = JSON.stringify({ content: userContent, attachmentIds: draft ? [draft.attachmentId] : [] });
        }
        state.streaming = true;
        state.controller = new AbortController();
        updateStreamingControls();
        renderMessages();
        let assistant = { id: `streaming-${Date.now()}`, role: "assistant", content: "", reasoning: "", provider: conversation.provider, model: conversation.model, status: "streaming" };
        if (!regenerateMessageId && (userContent || draft)) {
            conversation.messages.push({ id: createId(), role: "user", content: userContent,
                attachments: draft ? [{ name: draft.name, previewUrl: draft.previewUrl }] : [], createdAt: Date.now() });
        } else {
            conversation.messages = conversation.messages.filter((message) => message.id !== regenerateMessageId);
        }
        conversation.messages.push(assistant);
        renderMessages();

        try {
            requestOptions.signal = state.controller.signal;
            const response = await fetch(endpoint, requestOptions);
            if (!response.ok) {
                throw new Error(await responseError(response));
            }
            if (!response.body) throw new Error("浏览器未收到流式响应");
            await consumeEventStream(response.body, (eventName, data) => {
                if (eventName === "accepted") {
                    if (draft && state.drafts[conversation.id] === draft) {
                        if (draft.previewUrl) URL.revokeObjectURL(draft.previewUrl);
                        delete state.drafts[conversation.id];
                        renderAttachmentDraft();
                    }
                    return;
                }
                if (eventName === "error") throw new Error(data || "模型服务返回错误");
                if (eventName === "done" || data === "[DONE]") return;
                const chunk = extractChunk(data);
                assistant.reasoning += chunk.reasoning;
                assistant.content += chunk.content;
                assistant.status = "streaming";
                updateMessageElement(assistant);
            });
            assistant.status = "done";
        } catch (error) {
            if (error.name === "AbortError") {
                assistant.status = "done";
                if (!assistant.content && !assistant.reasoning) {
                    conversation.messages = conversation.messages.filter((message) => message.id !== assistant.id);
                }
            } else {
                assistant.status = "error";
                assistant.error = cleanError(error.message);
            }
        } finally {
            state.streaming = false;
            state.controller = null;
            await reloadConversations();
            updateStreamingControls();
            renderMessages();
        }
    }

    async function consumeEventStream(stream, onEvent) {
        const reader = stream.getReader();
        const decoder = new TextDecoder("utf-8");
        let buffer = "";

        while (true) {
            const { value, done } = await reader.read();
            buffer += decoder.decode(value || new Uint8Array(), { stream: !done });
            const blocks = buffer.split(/\r?\n\r?\n/);
            buffer = blocks.pop() || "";
            blocks.forEach((block) => dispatchEventBlock(block, onEvent));
            if (done) break;
        }
        if (buffer.trim()) dispatchEventBlock(buffer, onEvent);
    }

    function dispatchEventBlock(block, onEvent) {
        let eventName = "message";
        const data = [];
        block.split(/\r?\n/).forEach((line) => {
            if (line.startsWith("event:")) eventName = line.slice(6).trim();
            if (line.startsWith("data:")) data.push(line.slice(5).replace(/^ /, ""));
        });
        if (data.length) onEvent(eventName, data.join("\n"));
    }

    function extractChunk(raw) {
        let parsed = raw;
        try {
            parsed = JSON.parse(raw);
            if (typeof parsed === "string") {
                try { parsed = JSON.parse(parsed); } catch (ignored) { return { content: parsed, reasoning: "" }; }
            }
        } catch (ignored) {
            return { content: raw, reasoning: "" };
        }
        if (!parsed || typeof parsed !== "object") return { content: "", reasoning: "" };

        const choice = Array.isArray(parsed.choices) ? parsed.choices[0] : null;
        const delta = choice && (choice.delta || choice.message) ? (choice.delta || choice.message) : parsed;
        return {
            content: textValue(delta.content || parsed.content),
            reasoning: textValue(delta.reasoning_content || delta.reasoningContent || parsed.reasoning_content)
        };
    }

    function textValue(value) {
        if (typeof value === "string") return value;
        if (value && typeof value.text === "string") return value.text;
        if (Array.isArray(value)) return value.map((item) => textValue(item)).join("");
        return "";
    }

    function updateMessageElement(message) {
        const element = elements.messageColumn.querySelector(`[data-message-id="${message.id}"]`);
        if (!element) return;
        const reasoning = element.querySelector(".reasoning");
        if (message.reasoning) {
            if (reasoning) {
                reasoning.querySelector(".reasoning-content").textContent = message.reasoning;
            } else {
                const main = element.querySelector(".message-main");
                const body = element.querySelector(".message-body");
                body.insertAdjacentHTML("beforebegin", `<details class="reasoning" open><summary>思考过程</summary><div class="reasoning-content">${escapeHtml(message.reasoning)}</div></details>`);
                main.querySelector(".reasoning-content").scrollTop = main.querySelector(".reasoning-content").scrollHeight;
            }
        }
        element.querySelector(".message-body").innerHTML = `${renderMarkdown(message.content)}<span class="stream-cursor" aria-label="正在生成"></span>`;
        scrollToBottom(true);
    }

    function stopGeneration() {
        if (state.controller) state.controller.abort();
    }

    function updateStreamingControls() {
        elements.sendButton.hidden = state.streaming;
        elements.stopButton.hidden = !state.streaming;
        elements.messageInput.disabled = state.streaming;
        elements.imageButton.disabled = state.streaming;
        elements.modelTrigger.disabled = state.streaming;
        renderDeepThinkingToggle(activeConversation());
        updateComposer();
        refreshIcons();
    }

    function updateComposer() {
        const input = elements.messageInput;
        input.style.height = "auto";
        input.style.height = `${Math.min(input.scrollHeight, 190)}px`;
        elements.characterCount.textContent = `${input.value.length} / 12000`;
        elements.sendButton.disabled = state.streaming || (!input.value.trim() && !currentDraft());
        renderAttachmentDraft();
    }

    function currentDraft() {
        return state.activeId ? state.drafts[state.activeId] : null;
    }

    function handleImageSelection(event) {
        const file = event.target.files && event.target.files[0];
        event.target.value = "";
        if (file) selectImage(file);
    }

    function handleComposerPaste(event) {
        const files = Array.from((event.clipboardData && event.clipboardData.files) || []);
        const image = files.find((file) => file.type.startsWith("image/"));
        if (image) {
            event.preventDefault();
            selectImage(image);
        }
    }

    function handleImageDrop(event) {
        event.preventDefault();
        elements.composer.classList.remove("dragging");
        const image = Array.from((event.dataTransfer && event.dataTransfer.files) || [])
            .find((file) => file.type.startsWith("image/"));
        if (image) selectImage(image);
    }

    function selectImage(file) {
        if (!/^image\/(png|jpeg|webp)$/.test(file.type)) {
            showToast("仅支持 PNG、JPEG 或 WebP 图片");
            return;
        }
        if (file.size > 10 * 1024 * 1024) {
            showToast("图片大小不能超过 10 MB");
            return;
        }
        if (!state.activeId) return;
        const previous = currentDraft();
        if (previous && !window.confirm("当前已有图片，是否替换？")) return;
        if (previous && previous.previewUrl) URL.revokeObjectURL(previous.previewUrl);
        state.drafts[state.activeId] = { file, name: file.name || "图片", size: file.size,
            previewUrl: URL.createObjectURL(file), attachmentId: null };
        renderAttachmentDraft();
        updateComposer();
    }

    function renderAttachmentDraft() {
        const draft = currentDraft();
        if (!draft) {
            elements.attachmentDraft.hidden = true;
            elements.attachmentDraft.innerHTML = "";
            return;
        }
        elements.attachmentDraft.hidden = false;
        elements.attachmentDraft.innerHTML = `<img src="${escapeHtml(draft.previewUrl)}" alt=""><span>${escapeHtml(draft.name)} · ${formatBytes(draft.size)}</span><button type="button" data-remove-attachment aria-label="移除图片" title="移除"><i data-lucide="x"></i></button>`;
        elements.attachmentDraft.querySelector("[data-remove-attachment]").onclick = removeDraft;
        refreshIcons();
    }

    function removeDraft() {
        const draft = currentDraft();
        if (!draft) return;
        if (draft.previewUrl) URL.revokeObjectURL(draft.previewUrl);
        if (draft.attachmentId && state.activeId) {
            fetch(`${CONVERSATIONS_ENDPOINT}/${encodeURIComponent(state.activeId)}/attachments/${encodeURIComponent(draft.attachmentId)}`, { method: "DELETE" }).catch(() => {});
        }
        delete state.drafts[state.activeId];
        renderAttachmentDraft();
        updateComposer();
    }

    async function uploadAttachment(conversationId, draft) {
        const formData = new FormData();
        formData.append("image", draft.file);
        const response = await fetch(`${CONVERSATIONS_ENDPOINT}/${encodeURIComponent(conversationId)}/attachments`, { method: "POST", body: formData });
        if (!response.ok) throw new Error(await responseError(response));
        const result = await response.json();
        if (result && result.success === false) throw new Error(result.message || "图片上传失败");
        return result.data.id;
    }

    function formatBytes(size) {
        if (size < 1024 * 1024) return `${Math.max(1, Math.round(size / 1024))} KB`;
        return `${(size / 1024 / 1024).toFixed(1)} MB`;
    }

    function conversationHasImage(conversation) {
        return Boolean(conversation && conversation.messages.some((message) => message.attachments && message.attachments.length));
    }

    function modelSupportsImage(provider, model) {
        const info = findModel(provider, model);
        return Boolean(info.model.supportsImage);
    }

    function handleComposerKeydown(event) {
        if (event.key === "Enter" && !event.shiftKey && !event.isComposing) {
            event.preventDefault();
            if (!elements.sendButton.disabled) elements.composer.requestSubmit();
        }
    }

    function handleGlobalKeydown(event) {
        if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
            event.preventDefault();
            createConversation();
        }
        if (event.key === "Escape") {
            closeModelMenu();
            setSidebar(false);
        }
    }

    function setSidebar(open) {
        elements.sidebar.classList.toggle("open", open);
        elements.sidebarScrim.classList.toggle("visible", open);
        elements.sidebarScrim.setAttribute("aria-hidden", String(!open));
    }

    function toggleTheme() {
        const next = document.documentElement.dataset.theme === "dark" ? "light" : "dark";
        document.documentElement.dataset.theme = next;
        localStorage.setItem(THEME_KEY, next);
        updateThemeIcon();
    }

    function applyInitialTheme() {
        const saved = localStorage.getItem(THEME_KEY);
        const preferred = window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
        document.documentElement.dataset.theme = saved || preferred;
        updateThemeIcon();
    }

    function updateThemeIcon() {
        if (!elements.themeButton) return;
        const dark = document.documentElement.dataset.theme === "dark";
        elements.themeButton.innerHTML = `<i data-lucide="${dark ? "sun" : "moon"}"></i>`;
        elements.themeButton.setAttribute("aria-label", dark ? "切换浅色模式" : "切换深色模式");
        refreshIcons();
    }

    function renderMarkdown(source) {
        if (!source) return "";
        const codeBlocks = [];
        let text = source.replace(/```([^\n]*)\n?([\s\S]*?)```/g, (_, language, code) => {
            const token = `@@CODE_BLOCK_${codeBlocks.length}@@`;
            const label = language.trim() ? ` data-language="${escapeHtml(language.trim())}"` : "";
            codeBlocks.push(`<pre${label}><code>${escapeHtml(code.replace(/\n$/, ""))}</code></pre>`);
            return token;
        });

        text = escapeHtml(text);
        text = text
            .replace(/^### (.+)$/gm, "<h4>$1</h4>")
            .replace(/^## (.+)$/gm, "<h3>$1</h3>")
            .replace(/^# (.+)$/gm, "<h2>$1</h2>")
            .replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>")
            .replace(/`([^`\n]+)`/g, "<code>$1</code>");

        const lines = text.split("\n");
        const output = [];
        let listOpen = false;
        let paragraph = [];
        const flushParagraph = () => {
            if (paragraph.length) output.push(`<p>${paragraph.join("<br>")}</p>`);
            paragraph = [];
        };
        const closeList = () => {
            if (listOpen) output.push("</ul>");
            listOpen = false;
        };

        lines.forEach((line) => {
            if (/^@@CODE_BLOCK_\d+@@$/.test(line) || /^<h[234]>/.test(line)) {
                flushParagraph();
                closeList();
                output.push(line);
            } else if (/^[-*] /.test(line)) {
                flushParagraph();
                if (!listOpen) output.push("<ul>");
                listOpen = true;
                output.push(`<li>${line.slice(2)}</li>`);
            } else if (!line.trim()) {
                flushParagraph();
                closeList();
            } else {
                closeList();
                paragraph.push(line);
            }
        });
        flushParagraph();
        closeList();

        return output.join("").replace(/@@CODE_BLOCK_(\d+)@@/g, (_, index) => codeBlocks[Number(index)] || "");
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
        } catch (ignored) {
            return text;
        }
    }

    function cleanError(message) {
        if (!message) return "模型服务暂时不可用，请稍后重试。";
        return message.length > 280 ? `${message.slice(0, 280)}…` : message;
    }

    function scrollToBottom(smooth) {
        const distance = elements.chatViewport.scrollHeight - elements.chatViewport.scrollTop - elements.chatViewport.clientHeight;
        if (!smooth || distance < 240) {
            elements.chatViewport.scrollTo({ top: elements.chatViewport.scrollHeight, behavior: smooth ? "smooth" : "auto" });
        }
    }

    function showToast(message) {
        window.clearTimeout(state.toastTimer);
        elements.toast.textContent = message;
        elements.toast.classList.add("visible");
        state.toastTimer = window.setTimeout(() => elements.toast.classList.remove("visible"), 2200);
    }

    function createId() {
        if (window.crypto && typeof window.crypto.randomUUID === "function") return window.crypto.randomUUID();
        return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`;
    }

    function refreshIcons() {
        if (window.lucide) window.lucide.createIcons({ attrs: { "aria-hidden": "true" } });
    }
})();
