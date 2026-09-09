package cn.strong.webapp.user.service;

import cn.strong.webapp.user.conversation.ConversationStore;
import cn.strong.webapp.user.conversation.InMemoryConversationStore;
import cn.strong.webapp.user.model.conversation.Conversation;
import cn.strong.webapp.user.model.request.AiChatRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class ConversationServiceTest {

    private final ConversationStore conversationStore = new InMemoryConversationStore();
    private final ConversationService conversationService = new ConversationService(conversationStore);

    @Test
    void shouldCreateConversationAndReturnDetachedSnapshots() {
        Conversation created = conversationService.create("openai", "gpt-5.6-sol");
        created.setTitle("已修改的客户端副本");
        Conversation loaded = conversationService.get(created.getId());

        assertEquals("新对话", loaded.getTitle());
        assertEquals(created.getId(), loaded.getId());
        assertNotSame(created, loaded);
        assertEquals(1, conversationService.list().size());
    }

    @Test
    void shouldBuildChatRequestFromStoredHistoryAndSaveAssistantReply() {
        Conversation conversation = conversationService.create("openai", "gpt-5.6-sol");

        AiChatRequest request = conversationService.prepareChatRequest(conversation.getId(), "请介绍一下你自己");
        conversationService.appendAssistantReply(conversation.getId(), "我是 Strong AI", "");

        assertEquals("openai", request.getProvider());
        assertEquals("gpt-5.6-sol", request.getModel());
        assertEquals(1, request.getMessages().size());
        assertEquals("user", request.getMessages().get(0).getRole());
        assertEquals("请介绍一下你自己", request.getMessages().get(0).getContent());

        Conversation saved = conversationService.get(conversation.getId());
        assertEquals("请介绍一下你自己", saved.getTitle());
        assertEquals(2, saved.getMessages().size());
        assertEquals("assistant", saved.getMessages().get(1).getRole());
        assertEquals("我是 Strong AI", saved.getMessages().get(1).getContent());
    }

    @Test
    void shouldRemoveSupersededAssistantReplyBeforeRegeneration() {
        Conversation conversation = conversationService.create("deepseek", "deepseek-chat");
        conversationService.prepareChatRequest(conversation.getId(), "第一个问题");
        conversationService.appendAssistantReply(conversation.getId(), "旧回答", "");

        String assistantMessageId = conversationService.get(conversation.getId()).getMessages().get(1).getId();
        AiChatRequest request = conversationService.prepareRegenerateRequest(conversation.getId(), assistantMessageId);

        assertEquals(1, request.getMessages().size());
        assertEquals("第一个问题", request.getMessages().get(0).getContent());
        List<?> messages = conversationService.get(conversation.getId()).getMessages();
        assertEquals(1, messages.size());
    }
}
