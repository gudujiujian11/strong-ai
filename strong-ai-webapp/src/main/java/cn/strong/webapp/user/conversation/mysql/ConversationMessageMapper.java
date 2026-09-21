package cn.strong.webapp.user.conversation.mysql;

import cn.strong.mybatis.plus.extend.CustomBaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ConversationMessageMapper extends CustomBaseMapper<ConversationMessageEntity> {

    String COLUMNS = "id, conversation_id, message_order, role, content, reasoning, provider, model, "
            + "prompt_tokens, completion_tokens, total_tokens, created_at";

    @Select("SELECT " + COLUMNS + " FROM ai_conversation_message "
            + "WHERE conversation_id = #{conversationId} ORDER BY message_order")
    List<ConversationMessageEntity> findByConversationId(@Param("conversationId") String conversationId);

    @Select({
            "<script>",
            "SELECT " + COLUMNS + " FROM ai_conversation_message WHERE conversation_id IN",
            "<foreach collection='conversationIds' item='conversationId' open='(' separator=',' close=')'>",
            "#{conversationId}",
            "</foreach>",
            "ORDER BY conversation_id, message_order",
            "</script>"
    })
    List<ConversationMessageEntity> findByConversationIds(@Param("conversationIds") List<String> conversationIds);

    @Delete("DELETE FROM ai_conversation_message WHERE conversation_id = #{conversationId}")
    int deleteByConversationId(@Param("conversationId") String conversationId);

    @Insert({
            "<script>",
            "INSERT INTO ai_conversation_message (id, conversation_id, message_order, role, content, reasoning, "
                    + "provider, model, prompt_tokens, completion_tokens, total_tokens, created_at) VALUES",
            "<foreach collection='messages' item='message' separator=','>",
            "(#{message.id}, #{message.conversationId}, #{message.messageOrder}, #{message.role}, "
                    + "#{message.content}, #{message.reasoning}, #{message.provider}, #{message.model}, "
                    + "#{message.promptTokens}, #{message.completionTokens}, #{message.totalTokens}, "
                    + "#{message.createdAt})",
            "</foreach>",
            "</script>"
    })
    int insertBatch(@Param("messages") List<ConversationMessageEntity> messages);
}
