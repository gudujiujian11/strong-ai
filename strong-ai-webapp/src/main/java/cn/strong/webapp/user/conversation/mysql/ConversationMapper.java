package cn.strong.webapp.user.conversation.mysql;

import cn.strong.mybatis.plus.extend.CustomBaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ConversationMapper extends CustomBaseMapper<ConversationEntity> {

    String COLUMNS = "id, title, provider, model, deep_thinking, created_at, updated_at";

    @Select("SELECT " + COLUMNS + " FROM ai_conversation ORDER BY updated_at DESC")
    List<ConversationEntity> findAllOrderByUpdatedAtDesc();

    @Insert("INSERT INTO ai_conversation (id, title, provider, model, deep_thinking, created_at, updated_at) "
            + "VALUES (#{id}, #{title}, #{provider}, #{model}, #{deepThinking}, #{createdAt}, #{updatedAt}) "
            + "ON DUPLICATE KEY UPDATE title = VALUES(title), provider = VALUES(provider), "
            + "model = VALUES(model), deep_thinking = VALUES(deep_thinking), "
            + "created_at = VALUES(created_at), updated_at = VALUES(updated_at)")
    int upsert(ConversationEntity conversation);
}
