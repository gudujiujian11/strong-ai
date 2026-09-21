package cn.strong.webapp.user.controller;

import cn.strong.webapp.user.model.request.AiChatRequest;
import cn.strong.webapp.user.service.AiChatService;
import cn.strong.webapp.user.service.CodexResponsesService;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
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
    private final AiChatController aiChatController;
    private final String inboundApiKey;

    public CodexController(AiChatService aiChatService,
                           CodexResponsesService responsesService,
                           @Value("${codex.gateway.inbound-api-key:}") String inboundApiKey,
                           AiChatController aiChatController) {
        this.aiChatService = aiChatService;
        this.responsesService = responsesService;
        this.aiChatController = aiChatController;
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
        return aiChatController.chatStream(chatRequest);
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

}
