package cn.strong.webapp.user.conversation;

import cn.strong.webapp.user.model.conversation.Conversation;
import cn.strong.webapp.user.model.conversation.ConversationMessage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Repository;

/** JVM-local store intended for development only; data is lost when the service restarts. */
@Repository
public class InMemoryConversationStore implements ConversationStore {

    private final ConcurrentMap<String, Conversation> conversations = new ConcurrentHashMap<>();

    @Override
    public List<Conversation> findAll() {
        List<Conversation> result = new ArrayList<>();
        for (Conversation conversation : conversations.values()) {
            result.add(copy(conversation));
        }
        result.sort(Comparator.comparingLong(Conversation::getUpdatedAt).reversed());
        return result;
    }

    @Override
    public Optional<Conversation> findById(String id) {
        return Optional.ofNullable(conversations.get(id)).map(this::copy);
    }

    @Override
    public Conversation save(Conversation conversation) {
        Conversation snapshot = copy(conversation);
        conversations.put(snapshot.getId(), snapshot);
        return copy(snapshot);
    }

    @Override
    public boolean deleteById(String id) {
        return conversations.remove(id) != null;
    }

    private Conversation copy(Conversation source) {
        Conversation target = new Conversation();
        target.setId(source.getId());
        target.setTitle(source.getTitle());
        target.setProvider(source.getProvider());
        target.setModel(source.getModel());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        List<ConversationMessage> messages = new ArrayList<>();
        for (ConversationMessage sourceMessage : source.getMessages()) {
            ConversationMessage targetMessage = new ConversationMessage();
            targetMessage.setId(sourceMessage.getId());
            targetMessage.setRole(sourceMessage.getRole());
            targetMessage.setContent(sourceMessage.getContent());
            targetMessage.setReasoning(sourceMessage.getReasoning());
            targetMessage.setProvider(sourceMessage.getProvider());
            targetMessage.setModel(sourceMessage.getModel());
            targetMessage.setCreatedAt(sourceMessage.getCreatedAt());
            messages.add(targetMessage);
        }
        target.setMessages(messages);
        return target;
    }
}
