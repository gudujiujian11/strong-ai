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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiChatServiceTest {

    @Test
    void shouldRouteGpt56ToConfiguredOpenAiService() throws Exception {
        AiServiceRegistry registry = mock(AiServiceRegistry.class);
        IChatService chatService = mock(IChatService.class);
        when(registry.contains("openai")).thenReturn(true);
        when(registry.getChatService("openai")).thenReturn(chatService);
        when(chatService.chatCompletion(any(ChatCompletion.class))).thenReturn(chatResponse("gpt-5.6-sol"));

        AiChatRequest request = chatRequest("openai", "gpt-5.6-sol");
        request.setDeepThinking(Boolean.TRUE);
        request.setMaxTokens(2048);
        AiChatResponse response = new AiChatService(registry).chat(request);

        assertEquals("openai", response.getProvider());
        assertEquals("gpt-5.6-sol", response.getModel());
        ArgumentCaptor<ChatCompletion> captor = ArgumentCaptor.forClass(ChatCompletion.class);
        verify(chatService).chatCompletion(captor.capture());
        assertEquals("gpt-5.6-sol", captor.getValue().getModel());
        assertEquals(2048, captor.getValue().getMaxCompletionTokens());
        assertNull(captor.getValue().getMaxTokens());
        assertEquals("medium", captor.getValue().getParameters().get("reasoning_effort"));
    }

    @Test
    void shouldDisableDeepThinkingForGpt56() throws Exception {
        AiServiceRegistry registry = mock(AiServiceRegistry.class);
        IChatService chatService = mock(IChatService.class);
        when(registry.contains("openai")).thenReturn(true);
        when(registry.getChatService("openai")).thenReturn(chatService);
        when(chatService.chatCompletion(any(ChatCompletion.class))).thenReturn(chatResponse("gpt-5.6-luna"));

        AiChatRequest request = chatRequest("openai", "gpt-5.6-luna");
        request.setDeepThinking(Boolean.FALSE);
        new AiChatService(registry).chat(request);

        ArgumentCaptor<ChatCompletion> captor = ArgumentCaptor.forClass(ChatCompletion.class);
        verify(chatService).chatCompletion(captor.capture());
        assertEquals("none", captor.getValue().getParameters().get("reasoning_effort"));
    }

    @Test
    void shouldRouteDashscopeAliasToConfiguredQwenService() throws Exception {
        AiServiceRegistry registry = mock(AiServiceRegistry.class);
        IChatService chatService = mock(IChatService.class);
        when(registry.contains("qwen")).thenReturn(true);
        when(registry.getChatService("qwen")).thenReturn(chatService);
        when(chatService.chatCompletion(any(ChatCompletion.class))).thenReturn(chatResponse());

        AiChatRequest request = chatRequest("dashscope", "deepseek-chat");
        request.setMaxTokens(1024);
        AiChatResponse response = new AiChatService(registry).chat(request);

        assertEquals("qwen", response.getProvider());
        assertEquals("deepseek-chat", response.getModel());
        assertEquals("你好", response.getContent());
        ArgumentCaptor<ChatCompletion> captor = ArgumentCaptor.forClass(ChatCompletion.class);
        verify(chatService).chatCompletion(captor.capture());
        assertEquals("deepseek-chat", captor.getValue().getModel());
        assertEquals(1024, captor.getValue().getMaxTokens());
        assertNull(captor.getValue().getMaxCompletionTokens());
        assertEquals(Boolean.FALSE, captor.getValue().getStream());
    }

    @Test
    void shouldRejectProviderMissingFromStarterRegistry() {
        AiServiceRegistry registry = mock(AiServiceRegistry.class);
        when(registry.contains("unknown")).thenReturn(false);

        assertThrows(ParamValiateException.class,
                () -> new AiChatService(registry).chat(chatRequest("unknown", "deepseek-chat")));
    }

    @Test
    void shouldCreateAStreamRequestForConfiguredProvider() throws Exception {
        AiServiceRegistry registry = mock(AiServiceRegistry.class);
        IChatService chatService = mock(IChatService.class);
        when(registry.contains("deepseek")).thenReturn(true);
        when(registry.getChatService("deepseek")).thenReturn(chatService);
        doNothing().when(chatService).chatCompletionStream(any(ChatCompletion.class), any(SseListener.class));

        new AiChatService(registry).chatStream(chatRequest("deepseek", "deepseek-v4.1-flash"), new SseListener() {
            @Override
            protected void send() {
            }
        });

        ArgumentCaptor<ChatCompletion> captor = ArgumentCaptor.forClass(ChatCompletion.class);
        verify(chatService).chatCompletionStream(captor.capture(), any(SseListener.class));
        assertEquals("deepseek-v4.1-flash", captor.getValue().getModel());
        assertEquals(Boolean.TRUE, captor.getValue().getStream());
    }

    @Test
    void shouldKeepConfiguredDeepSeekModelName() throws Exception {
        AiServiceRegistry registry = mock(AiServiceRegistry.class);
        IChatService chatService = mock(IChatService.class);
        when(registry.contains("deepseek")).thenReturn(true);
        when(registry.getChatService("deepseek")).thenReturn(chatService);
        when(chatService.chatCompletion(any(ChatCompletion.class))).thenReturn(chatResponse("deepseek-v4.1-flash"));

        new AiChatService(registry).chat(chatRequest("deepseek", "deepseek-v4.1-flash"));

        ArgumentCaptor<ChatCompletion> captor = ArgumentCaptor.forClass(ChatCompletion.class);
        verify(chatService).chatCompletion(captor.capture());
        assertEquals("deepseek-v4.1-flash", captor.getValue().getModel());
    }

    @Test
    void shouldEnableDeepThinkingForDeepSeekFlash() throws Exception {
        AiServiceRegistry registry = mock(AiServiceRegistry.class);
        IChatService chatService = mock(IChatService.class);
        when(registry.contains("deepseek")).thenReturn(true);
        when(registry.getChatService("deepseek")).thenReturn(chatService);
        when(chatService.chatCompletion(any(ChatCompletion.class)))
                .thenReturn(chatResponse("deepseek-v4.1-flash"));

        AiChatRequest request = chatRequest("deepseek", "deepseek-v4.1-flash");
        request.setDeepThinking(Boolean.TRUE);
        new AiChatService(registry).chat(request);

        ArgumentCaptor<ChatCompletion> captor = ArgumentCaptor.forClass(ChatCompletion.class);
        verify(chatService).chatCompletion(captor.capture());
        assertEquals("enabled", ((java.util.Map<?, ?>) captor.getValue()
                .getExtraBody().get("thinking")).get("type"));
    }

    @Test
    void shouldDisableDeepThinkingForDeepSeekFlash() throws Exception {
        AiServiceRegistry registry = mock(AiServiceRegistry.class);
        IChatService chatService = mock(IChatService.class);
        when(registry.contains("deepseek")).thenReturn(true);
        when(registry.getChatService("deepseek")).thenReturn(chatService);
        when(chatService.chatCompletion(any(ChatCompletion.class)))
                .thenReturn(chatResponse("deepseek-v4.1-flash"));

        AiChatRequest request = chatRequest("deepseek", "deepseek-v4.1-flash");
        request.setDeepThinking(Boolean.FALSE);
        new AiChatService(registry).chat(request);

        ArgumentCaptor<ChatCompletion> captor = ArgumentCaptor.forClass(ChatCompletion.class);
        verify(chatService).chatCompletion(captor.capture());
        assertEquals("disabled", ((java.util.Map<?, ?>) captor.getValue()
                .getExtraBody().get("thinking")).get("type"));
    }

    @Test
    void shouldCreateMultimodalImageRequest() throws Exception {
        AiServiceRegistry registry = mock(AiServiceRegistry.class);
        IChatService chatService = mock(IChatService.class);
        when(registry.contains("openai")).thenReturn(true);
        when(registry.getChatService("openai")).thenReturn(chatService);
        when(chatService.chatCompletion(any(ChatCompletion.class))).thenReturn(chatResponse("gpt-5.6-sol"));

        AiChatResponse response = new AiChatService(registry).chatWithImage(
                "openai", "gpt-5.6-sol", "system", "解析图片并输出 Markdown",
                "data:image/png;base64,ZmFrZQ==", 1024);

        assertEquals("openai", response.getProvider());
        ArgumentCaptor<ChatCompletion> captor = ArgumentCaptor.forClass(ChatCompletion.class);
        verify(chatService).chatCompletion(captor.capture());
        ChatCompletion completion = captor.getValue();
        assertEquals(1024, completion.getMaxCompletionTokens());
        assertEquals(2, completion.getMessages().size());
        assertTrue(completion.getMessages().get(1).getContent().getMultiModals().toString()
                .contains("data:image/png;base64,ZmFrZQ=="));
    }

    private AiChatRequest chatRequest(String provider, String model) {
        AiChatRequest request = new AiChatRequest();
        request.setProvider(provider);
        request.setModel(model);
        AiChatMessageRequest message = new AiChatMessageRequest();
        message.setRole("user");
        message.setContent("你好");
        request.setMessages(Collections.singletonList(message));
        return request;
    }

    private ChatCompletionResponse chatResponse() {
        return chatResponse("deepseek-chat");
    }

    private ChatCompletionResponse chatResponse(String model) {
        Choice choice = new Choice();
        choice.setMessage(new ChatMessage("assistant", "你好"));
        choice.setFinishReason("stop");

        ChatCompletionResponse response = new ChatCompletionResponse();
        response.setId("chatcmpl-1");
        response.setModel(model);
        response.setChoices(Collections.singletonList(choice));
        return response;
    }
}
