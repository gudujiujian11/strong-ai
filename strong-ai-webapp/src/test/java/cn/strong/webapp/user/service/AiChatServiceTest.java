package cn.strong.webapp.user.service;

import cn.strong.common.exception.ParamValiateException;
import cn.strong.webapp.user.model.request.AiChatMessageRequest;
import cn.strong.webapp.user.model.request.AiChatRequest;
import cn.strong.webapp.user.model.response.AiChatResponse;
import io.github.lnyocly.ai4j.listener.SseListener;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatCompletion;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatCompletionResponse;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatMessage;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.Choice;
import io.github.lnyocly.ai4j.service.IChatService;
import io.github.lnyocly.ai4j.service.factory.AiServiceRegistry;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiChatServiceTest {

    @Test
    void shouldRouteDashscopeAliasToConfiguredQwenService() throws Exception {
        AiServiceRegistry registry = mock(AiServiceRegistry.class);
        IChatService chatService = mock(IChatService.class);
        when(registry.contains("qwen")).thenReturn(true);
        when(registry.getChatService("qwen")).thenReturn(chatService);
        when(chatService.chatCompletion(any(ChatCompletion.class))).thenReturn(chatResponse());

        AiChatResponse response = new AiChatService(registry).chat(chatRequest("dashscope"));

        assertEquals("qwen", response.getProvider());
        assertEquals("deepseek-chat", response.getModel());
        assertEquals("你好", response.getContent());
        ArgumentCaptor<ChatCompletion> captor = ArgumentCaptor.forClass(ChatCompletion.class);
        verify(chatService).chatCompletion(captor.capture());
        assertEquals("deepseek-chat", captor.getValue().getModel());
        assertEquals(Boolean.FALSE, captor.getValue().getStream());
    }

    @Test
    void shouldRejectProviderMissingFromStarterRegistry() {
        AiServiceRegistry registry = mock(AiServiceRegistry.class);
        when(registry.contains("unknown")).thenReturn(false);

        assertThrows(ParamValiateException.class,
                () -> new AiChatService(registry).chat(chatRequest("unknown")));
    }

    @Test
    void shouldCreateAStreamRequestForConfiguredProvider() throws Exception {
        AiServiceRegistry registry = mock(AiServiceRegistry.class);
        IChatService chatService = mock(IChatService.class);
        when(registry.contains("deepseek")).thenReturn(true);
        when(registry.getChatService("deepseek")).thenReturn(chatService);
        doNothing().when(chatService).chatCompletionStream(any(ChatCompletion.class), any(SseListener.class));

        new AiChatService(registry).chatStream(chatRequest("deepseek"), new SseListener() {
            @Override
            protected void send() {
            }
        });

        ArgumentCaptor<ChatCompletion> captor = ArgumentCaptor.forClass(ChatCompletion.class);
        verify(chatService).chatCompletionStream(captor.capture(), any(SseListener.class));
        assertEquals(Boolean.TRUE, captor.getValue().getStream());
    }

    private AiChatRequest chatRequest(String provider) {
        AiChatRequest request = new AiChatRequest();
        request.setProvider(provider);
        request.setModel("deepseek-chat");
        AiChatMessageRequest message = new AiChatMessageRequest();
        message.setRole("user");
        message.setContent("你好");
        request.setMessages(Collections.singletonList(message));
        return request;
    }

    private ChatCompletionResponse chatResponse() {
        Choice choice = new Choice();
        choice.setMessage(new ChatMessage("assistant", "你好"));
        choice.setFinishReason("stop");

        ChatCompletionResponse response = new ChatCompletionResponse();
        response.setId("chatcmpl-1");
        response.setModel("deepseek-chat");
        response.setChoices(Collections.singletonList(choice));
        return response;
    }
}
