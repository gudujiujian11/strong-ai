package cn.strong.webapp.user.controller;

import cn.strong.webapp.user.model.request.AiChatRequest;
import cn.strong.webapp.user.service.AiChatService;
import cn.strong.webapp.user.service.CodexResponsesService;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CodexControllerTest {

    @Test
    void returnsSseEmitterForStreamingResponses() throws Exception {
        AiChatService aiChatService = mock(AiChatService.class);
        CodexResponsesService responsesService = mock(CodexResponsesService.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        AiChatController aiChatController = mock(AiChatController.class);
        AiChatRequest chatRequest = new AiChatRequest();
        SseEmitter expected = new SseEmitter();
        when(httpRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer inbound-key");
        when(responsesService.toChatRequest(any())).thenReturn(chatRequest);
        when(aiChatController.chatStream(chatRequest)).thenReturn(expected);
        CodexController controller = new CodexController(aiChatService, responsesService,
                "inbound-key", aiChatController);

        Object result = controller.responses(
                new ObjectMapper().readTree("{\"stream\":true}"), httpRequest);

        assertEquals(expected, result);
        verify(aiChatController).chatStream(chatRequest);
    }
}
