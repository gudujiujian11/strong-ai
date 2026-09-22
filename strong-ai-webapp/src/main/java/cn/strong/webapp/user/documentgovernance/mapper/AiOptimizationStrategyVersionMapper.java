package cn.strong.webapp.user.documentgovernance.mapper;

import cn.strong.mybatis.plus.extend.CustomBaseMapper;
import cn.strong.webapp.user.documentgovernance.entity.AiOptimizationStrategyVersionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiOptimizationStrategyVersionMapper extends CustomBaseMapper<AiOptimizationStrategyVersionEntity> {

    @Select("SELECT * FROM dg_ai_optimization_strategy_version WHERE team_id = #{teamId} AND strategy_id = #{strategyId} ORDER BY version_no DESC")
    List<AiOptimizationStrategyVersionEntity> selectByStrategyId(@Param("teamId") String teamId,
                                                                 @Param("strategyId") Long strategyId);

    @Select("SELECT * FROM dg_ai_optimization_strategy_version WHERE team_id = #{teamId} AND strategy_id = #{strategyId} AND version_no = #{versionNo}")
    AiOptimizationStrategyVersionEntity selectByStrategyAndVersionNo(@Param("teamId") String teamId,
                                                                      @Param("strategyId") Long strategyId,
                                                                      @Param("versionNo") Long versionNo);

    @Select("SELECT * FROM dg_ai_optimization_strategy_version WHERE team_id = #{teamId} AND strategy_id = #{strategyId} AND status = 'PUBLISHED' ORDER BY version_no DESC")
    List<AiOptimizationStrategyVersionEntity> selectPublishedByStrategyId(@Param("teamId") String teamId,
                                                                          @Param("strategyId") Long strategyId);

    @Select("SELECT COALESCE(MAX(version_no), 0) FROM dg_ai_optimization_strategy_version WHERE team_id = #{teamId} AND strategy_id = #{strategyId}")
    Long selectMaxVersionNo(@Param("teamId") String teamId, @Param("strategyId") Long strategyId);

    @Select("SELECT * FROM dg_ai_optimization_strategy_version WHERE team_id = #{teamId} AND id = #{id}")
    AiOptimizationStrategyVersionEntity selectByTeamAndId(@Param("teamId") String teamId, @Param("id") Long id);
}
