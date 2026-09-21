package cn.strong.webapp.user.conversation.mysql;

import cn.strong.webapp.user.model.conversation.Conversation;
import cn.strong.webapp.user.model.conversation.ConversationMessage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MysqlConversationStoreTest {

    private ConversationMapper conversationMapper;
    private ConversationMessageMapper messageMapper;
    private MysqlConversationStore store;

    @BeforeEach
    void setUp() {
        conversationMapper = mock(ConversationMapper.class);
        messageMapper = mock(ConversationMessageMapper.class);
        store = new MysqlConversationStore(conversationMapper, messageMapper);
    }

    @Test
    void shouldLoadAllConversationsWithMessagesInDatabaseOrder() {
        ConversationEntity newer = conversationEntity("newer", 200L, true);
        ConversationEntity older = conversationEntity("older", 100L, false);
        ConversationMessageEntity first = messageEntity("message-1", "newer", 0, "user");
        ConversationMessageEntity second = messageEntity("message-2", "newer", 1, "assistant");
        when(conversationMapper.findAllOrderByUpdatedAtDesc()).thenReturn(Arrays.asList(newer, older));
        when(messageMapper.findByConversationIds(Arrays.asList("newer", "older")))
                .thenReturn(Arrays.asList(first, second));

        List<Conversation> result = store.findAll();

        assertEquals(Arrays.asList("newer", "older"), result.stream()
                .map(Conversation::getId).collect(Collectors.toList()));
        assertEquals(Arrays.asList("message-1", "message-2"), result.get(0).getMessages().stream()
                .map(ConversationMessage::getId).collect(Collectors.toList()));
        assertTrue(result.get(1).getMessages().isEmpty());
    }

    @Test
    void shouldMapEveryConversationAndMessageField() {
        ConversationEntity entity = conversationEntity("conversation-1", 200L, true);
        ConversationMessageEntity message = messageEntity("message-1", "conversation-1", 0, "assistant");
        when(conversationMapper.selectById("conversation-1")).thenReturn(entity);
        when(messageMapper.findByConversationId("conversation-1")).thenReturn(Collections.singletonList(message));

        Optional<Conversation> result = store.findById("conversation-1");

        assertTrue(result.isPresent());
        Conversation conversation = result.get();
        assertEquals("conversation-1", conversation.getId());
        assertEquals("Conversation conversation-1", conversation.getTitle());
        assertEquals("openai", conversation.getProvider());
        assertEquals("gpt-5.6-sol", conversation.getModel());
        assertEquals(Boolean.TRUE, conversation.getDeepThinking());
        assertEquals(50L, conversation.getCreatedAt());
        assertEquals(200L, conversation.getUpdatedAt());

        ConversationMessage savedMessage = conversation.getMessages().get(0);
        assertEquals("message-1", savedMessage.getId());
        assertEquals("assistant", savedMessage.getRole());
        assertEquals("content-message-1", savedMessage.getContent());
        assertEquals("reasoning-message-1", savedMessage.getReasoning());
        assertEquals("openai", savedMessage.getProvider());
        assertEquals("gpt-5.6-sol", savedMessage.getModel());
        assertEquals(11L, savedMessage.getPromptTokens());
        assertNull(savedMessage.getCompletionTokens());
        assertEquals(31L, savedMessage.getTotalTokens());
        assertEquals(60L, savedMessage.getCreatedAt());
    }

    @Test
    void shouldReturnEmptyWhenConversationDoesNotExist() {
        when(conversationMapper.selectById("missing")).thenReturn(null);

        Optional<Conversation> result = store.findById("missing");

        assertFalse(result.isPresent());
        verifyNoInteractions(messageMapper);
    }

    @Test
    void shouldReturnEmptyListWhenNoConversationsExist() {
        when(conversationMapper.findAllOrderByUpdatedAtDesc()).thenReturn(Collections.emptyList());

        assertTrue(store.findAll().isEmpty());
        verifyNoInteractions(messageMapper);
    }

    @Test
    void shouldReplaceMessageSnapshotWhenSaving() {
        Conversation input = conversationWithTwoMessages();
        ArgumentCaptor<ConversationEntity> conversationCaptor = ArgumentCaptor.forClass(ConversationEntity.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ConversationMessageEntity>> messageCaptor = ArgumentCaptor.forClass(List.class);

        Conversation saved = store.save(input);

        InOrder writes = inOrder(conversationMapper, messageMapper);
        writes.verify(conversationMapper).upsert(conversationCaptor.capture());
        writes.verify(messageMapper).deleteByConversationId(input.getId());
        writes.verify(messageMapper).insertBatch(messageCaptor.capture());
        assertEquals(input.getId(), conversationCaptor.getValue().getId());
        assertEquals(Boolean.TRUE, conversationCaptor.getValue().getDeepThinking());
        assertEquals(Arrays.asList(0, 1), messageCaptor.getValue().stream()
                .map(ConversationMessageEntity::getMessageOrder).collect(Collectors.toList()));
        assertEquals("conversation-1", messageCaptor.getValue().get(0).getConversationId());
        assertNotSame(input, saved);
        assertNotSame(input.getMessages(), saved.getMessages());
        assertNotSame(input.getMessages().get(0), saved.getMessages().get(0));
    }

    @Test
    void shouldNotBatchInsertWhenSavingConversationWithoutMessages() {
        Conversation input = conversationWithTwoMessages();
        input.setMessages(Collections.emptyList());

        Conversation saved = store.save(input);

        verify(conversationMapper).upsert(any(ConversationEntity.class));
        verify(messageMapper).deleteByConversationId(input.getId());
        verify(messageMapper, never()).insertBatch(anyList());
        assertTrue(saved.getMessages().isEmpty());
    }

    @Test
    void shouldReturnTrueWhenDeletingExistingConversation() {
        when(conversationMapper.deleteById("existing")).thenReturn(1);

        assertTrue(store.deleteById("existing"));

        InOrder writes = inOrder(conversationMapper, messageMapper);
        writes.verify(conversationMapper).deleteById("existing");
        writes.verify(messageMapper).deleteByConversationId("existing");
    }

    @Test
    void shouldReturnFalseWhenDeletingMissingConversation() {
        when(conversationMapper.deleteById("missing")).thenReturn(0);

        assertFalse(store.deleteById("missing"));

        verifyNoInteractions(messageMapper);
    }

    private ConversationEntity conversationEntity(String id, long updatedAt, boolean deepThinking) {
        ConversationEntity entity = new ConversationEntity();
        entity.setId(id);
        entity.setTitle("Conversation " + id);
        entity.setProvider("openai");
        entity.setModel("gpt-5.6-sol");
        entity.setDeepThinking(deepThinking);
        entity.setCreatedAt(50L);
        entity.setUpdatedAt(updatedAt);
        return entity;
    }

    private ConversationMessageEntity messageEntity(String id, String conversationId, int order, String role) {
        ConversationMessageEntity entity = new ConversationMessageEntity();
        entity.setId(id);
        entity.setConversationId(conversationId);
        entity.setMessageOrder(order);
        entity.setRole(role);
        entity.setContent("content-" + id);
        entity.setReasoning("reasoning-" + id);
        entity.setProvider("openai");
        entity.setModel("gpt-5.6-sol");
        entity.setPromptTokens(11L);
        entity.setCompletionTokens(null);
        entity.setTotalTokens(31L);
        entity.setCreatedAt(60L);
        return entity;
    }

    private Conversation conversationWithTwoMessages() {
        Conversation conversation = new Conversation();
        conversation.setId("conversation-1");
        conversation.setTitle("Conversation conversation-1");
        conversation.setProvider("openai");
        conversation.setModel("gpt-5.6-sol");
        conversation.setDeepThinking(Boolean.TRUE);
        conversation.setCreatedAt(50L);
        conversation.setUpdatedAt(200L);
        conversation.setMessages(Arrays.asList(
                message("message-1", "user", null),
                message("message-2", "assistant", 20L)));
        return conversation;
    }

    private ConversationMessage message(String id, String role, Long completionTokens) {
        ConversationMessage message = new ConversationMessage();
        message.setId(id);
        message.setRole(role);
        message.setContent("content-" + id);
        message.setReasoning("reasoning-" + id);
        message.setProvider("openai");
        message.setModel("gpt-5.6-sol");
        message.setPromptTokens(11L);
        message.setCompletionTokens(completionTokens);
        message.setTotalTokens(31L);
        message.setCreatedAt(60L);
        return message;
    }
}
