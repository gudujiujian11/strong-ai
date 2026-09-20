package cn.strong.webapp.user.documentgovernance.mapper;

import cn.strong.mybatis.plus.extend.CustomBaseMapper;
import cn.strong.webapp.user.documentgovernance.entity.AiTaskEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiTaskMapper extends CustomBaseMapper<AiTaskEntity> {

    String TASK_COLUMNS = "id, team_id, document_id, idempotency_key, strategy_id, strategy_version_id, "
            + "strategy_config_hash, created_by, status, provider, model, result_summary, error_code, "
            + "prompt_tokens, completion_tokens, total_tokens, created_at, updated_at, expires_at";

    @Select("SELECT " + TASK_COLUMNS + " FROM dg_ai_task WHERE team_id = #{teamId} AND document_id = #{documentId} AND idempotency_key = #{idempotencyKey}")
    AiTaskEntity findByIdempotency(@Param("teamId") String teamId, @Param("documentId") Long documentId,
                                   @Param("idempotencyKey") String idempotencyKey);

    @Select("SELECT " + TASK_COLUMNS + " FROM dg_ai_task WHERE team_id = #{teamId} AND id = #{taskId}")
    AiTaskEntity selectByTeamAndId(@Param("teamId") String teamId, @Param("taskId") Long taskId);

    @Delete("DELETE FROM dg_ai_task WHERE expires_at < #{now} AND status IN ('PENDING', 'FAILED', 'SUCCEEDED', 'CANCELLED')")
    int deleteExpiredTerminalTasks(@Param("now") long now);
}
