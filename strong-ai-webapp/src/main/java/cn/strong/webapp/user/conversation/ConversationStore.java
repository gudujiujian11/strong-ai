package cn.strong.webapp.user.conversation;

import cn.strong.webapp.user.model.conversation.Conversation;
import java.util.List;
import java.util.Optional;

/** Conversation persistence boundary. Implement this interface for Redis or database storage. */
public interface ConversationStore {

    List<Conversation> findAll();

    Optional<Conversation> findById(String id);

    Conversation save(Conversation conversation);

    boolean deleteById(String id);
}
