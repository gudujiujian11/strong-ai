package cn.strong.webapp.user.conversation.mysql;

import cn.strong.mybatis.plus.extend.CustomBaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ConversationAttachmentMapper extends CustomBaseMapper<ConversationAttachmentEntity> {

    String COLUMNS = "id, conversation_id, message_id, original_name, content_type, size_bytes, "
            + "created_at, expires_at";

    @Select("SELECT " + COLUMNS + " FROM ai_conversation_attachment WHERE conversation_id = #{conversationId} "
            + "AND id = #{attachmentId}")
    ConversationAttachmentEntity findByConversationAndId(@Param("conversationId") String conversationId,
                                                          @Param("attachmentId") String attachmentId);

    @Select("SELECT id, conversation_id, message_id, original_name, content_type, size_bytes, content, "
            + "created_at, expires_at FROM ai_conversation_attachment WHERE conversation_id = #{conversationId} "
            + "AND id = #{attachmentId}")
    ConversationAttachmentEntity findWithContent(@Param("conversationId") String conversationId,
                                                  @Param("attachmentId") String attachmentId);

    @Select({
            "<script>",
            "SELECT " + COLUMNS + " FROM ai_conversation_attachment WHERE conversation_id IN",
            "<foreach collection='conversationIds' item='conversationId' open='(' separator=',' close=')'>",
            "#{conversationId}",
            "</foreach>",
            "ORDER BY created_at",
            "</script>"
    })
    List<ConversationAttachmentEntity> findByConversationIds(@Param("conversationIds") List<String> conversationIds);

    @Select({
            "<script>",
            "SELECT " + COLUMNS + " FROM ai_conversation_attachment WHERE conversation_id = #{conversationId} "
                    + "AND message_id IS NULL AND (expires_at IS NULL OR expires_at > #{now}) AND id IN",
            "<foreach collection='attachmentIds' item='attachmentId' open='(' separator=',' close=')'>",
            "#{attachmentId}",
            "</foreach>",
            "</script>"
    })
    List<ConversationAttachmentEntity> findUnbound(@Param("conversationId") String conversationId,
                                                   @Param("attachmentIds") List<String> attachmentIds,
                                                   @Param("now") long now);

    @Insert("INSERT INTO ai_conversation_attachment (id, conversation_id, message_id, original_name, "
            + "content_type, size_bytes, content, created_at, expires_at) VALUES (#{id}, #{conversationId}, "
            + "#{messageId}, #{originalName}, #{contentType}, #{sizeBytes}, #{content}, #{createdAt}, #{expiresAt})")
    int insert(ConversationAttachmentEntity attachment);

    @Update({
            "<script>",
            "UPDATE ai_conversation_attachment SET message_id = #{messageId}, expires_at = NULL "
                    + "WHERE conversation_id = #{conversationId} AND message_id IS NULL "
                    + "AND (expires_at IS NULL OR expires_at > #{now}) AND id IN",
            "<foreach collection='attachmentIds' item='attachmentId' open='(' separator=',' close=')'>",
            "#{attachmentId}",
            "</foreach>",
            "</script>"
    })
    int bind(@Param("conversationId") String conversationId, @Param("messageId") String messageId,
             @Param("attachmentIds") List<String> attachmentIds, @Param("now") long now);

    @Delete("DELETE FROM ai_conversation_attachment WHERE conversation_id = #{conversationId} "
            + "AND id = #{attachmentId} AND message_id IS NULL")
    int deleteUnbound(@Param("conversationId") String conversationId, @Param("attachmentId") String attachmentId);

    @Delete("DELETE FROM ai_conversation_attachment WHERE conversation_id = #{conversationId}")
    int deleteByConversationId(@Param("conversationId") String conversationId);

    @Delete("DELETE FROM ai_conversation_attachment WHERE message_id IS NULL AND expires_at IS NOT NULL "
            + "AND expires_at <= #{now}")
    int deleteExpired(@Param("now") long now);
}
