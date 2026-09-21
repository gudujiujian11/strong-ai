package cn.strong.webapp.user.controller;

import cn.strong.webapp.user.model.request.AiChatRequest;
import cn.strong.webapp.user.service.AiChatService;
import cn.strong.webapp.user.service.CodexResponsesService;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CodexControllerTest {

    @Test
    void returnsEventStreamContentTypeForStreamingResponses() throws Exception {
        AiChatService aiChatService = mock(AiChatService.class);
        CodexResponsesService responsesService = mock(CodexResponsesService.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        AiChatRequest chatRequest = new AiChatRequest();
        when(httpRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer inbound-key");
        when(responsesService.toChatRequest(any())).thenReturn(chatRequest);
        doNothing().when(aiChatService).chatStream(any(), any());
        CodexController controller = new CodexController(aiChatService, responsesService,
                new ObjectMapper(), Runnable::run, "inbound-key");

        Object result = controller.responses(
                new ObjectMapper().readTree("{\"stream\":true}"), httpRequest);

        assertTrue(result instanceof ResponseEntity);
        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertEquals(MediaType.TEXT_EVENT_STREAM, response.getHeaders().getContentType());
    }
}
