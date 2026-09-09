package cn.strong.webapp.user.controller;

import cn.strong.common.base.Result;
import cn.strong.webapp.user.model.conversation.Conversation;
import cn.strong.webapp.user.model.request.AiChatRequest;
import cn.strong.webapp.user.model.request.ConversationChatRequest;
import cn.strong.webapp.user.model.request.ConversationCreateRequest;
import cn.strong.webapp.user.model.request.ConversationUpdateRequest;
import cn.strong.webapp.user.service.AiChatService;
import cn.strong.webapp.user.service.ConversationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lnyocly.ai4j.listener.SseListener;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/conversations")
@Api(tags = {"v1.0", "conversation"}, description = "服务端会话历史接口")
public class ConversationController {

    private final ConversationService conversationService;
    private final AiChatService aiChatService;
    private final TaskExecutor taskExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConversationController(ConversationService conversationService, AiChatService aiChatService,
                                  TaskExecutor taskExecutor) {
        this.conversationService = conversationService;
        this.aiChatService = aiChatService;
        this.taskExecutor = taskExecutor;
    }

    @GetMapping
    @ApiOperation("获取全部会话及历史消息")
    public Result<List<Conversation>> list() {
        return Result.ok(conversationService.list());
    }

    @GetMapping("/{id}")
    @ApiOperation("获取单个会话")
    public Result<Conversation> get(@PathVariable("id") String id) {
        return Result.ok(conversationService.get(id));
    }

    @PostMapping
    @ApiOperation("创建会话")
    public Result<Conversation> create(@RequestBody ConversationCreateRequest request) {
        return Result.ok(conversationService.create(request.getProvider(), request.getModel()));
    }

    @PutMapping("/{id}")
    @ApiOperation("更新会话使用的模型")
    public Result<Conversation> update(@PathVariable("id") String id, @RequestBody ConversationUpdateRequest request) {
        return Result.ok(conversationService.updateModel(id, request.getProvider(), request.getModel()));
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除会话")
    public Result<Void> delete(@PathVariable("id") String id) {
        conversationService.delete(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}/messages")
    @ApiOperation("清空会话消息")
    public Result<Conversation> clearMessages(@PathVariable("id") String id) {
        return Result.ok(conversationService.clearMessages(id));
    }

    @PostMapping(value = "/{id}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation("追加用户消息并基于服务端历史流式生成回复")
    public SseEmitter chatStream(@PathVariable("id") String id, @RequestBody ConversationChatRequest request) {
        AiChatRequest chatRequest = conversationService.prepareChatRequest(id, request.getContent());
        return stream(id, chatRequest);
    }

    @PostMapping(value = "/{id}/messages/{messageId}/regenerate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation("删除指定助手回复后重新生成")
    public SseEmitter regenerateStream(@PathVariable("id") String id,
                                       @PathVariable("messageId") String messageId) {
        AiChatRequest chatRequest = conversationService.prepareRegenerateRequest(id, messageId);
        return stream(id, chatRequest);
    }

    private SseEmitter stream(String conversationId, AiChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicBoolean saved = new AtomicBoolean(false);
        StringBuilder content = new StringBuilder();
        StringBuilder reasoning = new StringBuilder();
        Runnable saveReply = () -> {
            if (saved.compareAndSet(false, true)) {
                conversationService.appendAssistantReply(conversationId, content.toString(), reasoning.toString());
            }
        };
        SseListener listener = new SseListener() {
            @Override
            protected void send() {
                String data = getCurrData();
                if (data != null && !"[DONE]".equalsIgnoreCase(data)) {
                    StreamChunk chunk = readChunk(data);
                    content.append(chunk.content);
                    reasoning.append(chunk.reasoning);
                }
                try {
                    if ("[DONE]".equalsIgnoreCase(data)) {
                        if (completed.compareAndSet(false, true)) {
                            saveReply.run();
                            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                            emitter.complete();
                        }
                    } else if (data != null && !data.isEmpty() && !completed.get()) {
                        emitter.send(SseEmitter.event().name("message").data(data, MediaType.APPLICATION_JSON));
                    }
                } catch (IOException ex) {
                    completed.set(true);
                    cancelStream();
                    emitter.complete();
                }
            }
        };

        emitter.onCompletion(() -> {
            completed.set(true);
            listener.cancelStream();
        });
        emitter.onTimeout(() -> {
            completed.set(true);
            listener.cancelStream();
        });

        taskExecutor.execute(() -> {
            try {
                aiChatService.chatStream(request, listener);
            } catch (Exception ex) {
                if (completed.compareAndSet(false, true)) {
                    try {
                        emitter.send(SseEmitter.event().name("error").data(message(ex)));
                    } catch (IOException ignored) {
                        // The client has already disconnected.
                    }
                    emitter.complete();
                }
            } finally {
                saveReply.run();
                if (completed.compareAndSet(false, true)) {
                    emitter.complete();
                }
            }
        });
        return emitter;
    }

    private StreamChunk readChunk(String raw) {
        try {
            JsonNode parsed = objectMapper.readTree(raw);
            if (parsed.isTextual()) {
                return new StreamChunk(parsed.asText(), "");
            }
            JsonNode choice = parsed.path("choices").isArray() && parsed.path("choices").size() > 0
                    ? parsed.path("choices").get(0) : parsed;
            JsonNode delta = choice.has("delta") ? choice.path("delta")
                    : (choice.has("message") ? choice.path("message") : choice);
            return new StreamChunk(text(delta.path("content")),
                    text(delta.has("reasoning_content") ? delta.path("reasoning_content")
                            : delta.path("reasoningContent")));
        } catch (Exception ignored) {
            return new StreamChunk("", "");
        }
    }

    private String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isArray()) {
            StringBuilder result = new StringBuilder();
            for (JsonNode item : node) {
                result.append(text(item.has("text") ? item.path("text") : item));
            }
            return result.toString();
        }
        return node.has("text") ? text(node.path("text")) : "";
    }

    private String message(Exception ex) {
        Throwable cause = ex.getCause() == null ? ex : ex.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    private static class StreamChunk {
        private final String content;
        private final String reasoning;

        private StreamChunk(String content, String reasoning) {
            this.content = content;
            this.reasoning = reasoning;
        }
    }
}
