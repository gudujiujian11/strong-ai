package cn.strong.webapp.user.documentgovernance.mapper;

import cn.strong.mybatis.plus.extend.CustomBaseMapper;
import cn.strong.webapp.user.documentgovernance.entity.AiTaskEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiTaskMapper extends CustomBaseMapper<AiTaskEntity> {

    @Select("SELECT * FROM dg_ai_task WHERE team_id = #{teamId} AND document_id = #{documentId} AND idempotency_key = #{idempotencyKey}")
    AiTaskEntity findByIdempotency(@Param("teamId") String teamId, @Param("documentId") Long documentId,
                                   @Param("idempotencyKey") String idempotencyKey);

    @Select("SELECT * FROM dg_ai_task WHERE team_id = #{teamId} AND id = #{taskId}")
    AiTaskEntity selectByTeamAndId(@Param("teamId") String teamId, @Param("taskId") Long taskId);

    @Delete("DELETE FROM dg_ai_task WHERE expires_at < #{now} AND status IN ('PENDING', 'FAILED', 'SUCCEEDED', 'CANCELLED')")
    int deleteExpiredTerminalTasks(@Param("now") long now);
}
