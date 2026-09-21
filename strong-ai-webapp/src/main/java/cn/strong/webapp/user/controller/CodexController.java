package cn.strong.webapp.user.controller;

import cn.strong.webapp.user.model.request.AiChatRequest;
import cn.strong.webapp.user.service.AiChatService;
import cn.strong.webapp.user.service.CodexResponsesService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lnyocly.ai4j.listener.SseListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 面向 Codex 的固定 DeepSeek Responses 兼容入口。 */
@RestController
@RequestMapping("/api/codex/v1")
public class CodexController {

    private final AiChatService aiChatService;
    private final CodexResponsesService responsesService;
    private final ObjectMapper objectMapper;
    private final TaskExecutor taskExecutor;
    private final String inboundApiKey;

    public CodexController(AiChatService aiChatService,
                           CodexResponsesService responsesService,
                           ObjectMapper objectMapper,
                           @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
                           @Value("${codex.gateway.inbound-api-key:}") String inboundApiKey) {
        this.aiChatService = aiChatService;
        this.responsesService = responsesService;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
        this.inboundApiKey = inboundApiKey;
    }

    @GetMapping("/models")
    public ResponseEntity<String> models(HttpServletRequest request) {
        authenticate(request);
        return ResponseEntity.ok("{\"object\":\"list\",\"data\":[{\"id\":\""
                + CodexResponsesService.PUBLIC_MODEL + "\",\"object\":\"model\",\"owned_by\":\"strong-ai\"}]}");
    }

    @PostMapping(value = "/responses", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object responses(@RequestBody JsonNode request, HttpServletRequest httpRequest) {
        authenticate(httpRequest);
        AiChatRequest chatRequest = responsesService.toChatRequest(request);
        if (!request.path("stream").asBoolean(false)) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responsesService.toResponse(aiChatService.chat(chatRequest)));
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(stream(chatRequest));
    }

    private SseEmitter stream(AiChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        AtomicBoolean completed = new AtomicBoolean(false);
        SseListener listener = new SseListener() {
            @Override
            protected void send() {
                String data = getCurrData();
                try {
                    if ("[DONE]".equalsIgnoreCase(data)) {
                        if (completed.compareAndSet(false, true)) {
                            emitter.send(SseEmitter.event().name("response.completed")
                                    .data(responsesService.completedEvent(), MediaType.APPLICATION_JSON));
                            emitter.complete();
                        }
                        return;
                    }
                    String event = responsesService.toOutputTextDelta(data);
                    if (event != null && !completed.get()) {
                        emitter.send(SseEmitter.event().name("response.output_text.delta")
                                .data(event, MediaType.APPLICATION_JSON));
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
                if (completed.compareAndSet(false, true)) {
                    emitter.send(SseEmitter.event().name("response.completed")
                            .data(responsesService.completedEvent(), MediaType.APPLICATION_JSON));
                    emitter.complete();
                }
            } catch (Exception ex) {
                if (completed.compareAndSet(false, true)) {
                    try {
                        emitter.send(SseEmitter.event().name("error").data(error(ex), MediaType.APPLICATION_JSON));
                    } catch (IOException ignored) {
                        // 客户端已断开连接。
                    }
                    emitter.complete();
                }
            }
        });
        return emitter;
    }

    private void authenticate(HttpServletRequest request) {
        if (inboundApiKey == null || inboundApiKey.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Codex 网关未配置 API Key");
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        String actual = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring("Bearer ".length()).trim() : "";
        if (!MessageDigest.isEqual(inboundApiKey.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "API Key 无效");
        }
    }

    private String error(Exception ex) {
        String message = ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage();
        try {
            return objectMapper.createObjectNode().put("type", "error").put("message",
                    message == null ? ex.getClass().getSimpleName() : message).toString();
        } catch (Exception ignored) {
            return "{\"type\":\"error\",\"message\":\"调用失败\"}";
        }
    }
}
