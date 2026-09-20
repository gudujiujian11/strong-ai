package cn.strong.webapp.user.documentgovernance.mapper;

import cn.strong.mybatis.plus.extend.CustomBaseMapper;
import cn.strong.webapp.user.documentgovernance.entity.AiOptimizationStrategyEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiOptimizationStrategyMapper extends CustomBaseMapper<AiOptimizationStrategyEntity> {

    @Results(id = "strategy", value = @Result(column = "is_default", property = "defaultStrategy"))
    @Select("SELECT * FROM dg_ai_optimization_strategy WHERE team_id = #{teamId} ORDER BY updated_at DESC, id DESC")
    List<AiOptimizationStrategyEntity> selectByTeamId(@Param("teamId") String teamId);

    @ResultMap("strategy")
    @Select("SELECT * FROM dg_ai_optimization_strategy WHERE team_id = #{teamId} AND id = #{strategyId}")
    AiOptimizationStrategyEntity selectByTeamAndId(@Param("teamId") String teamId,
                                                   @Param("strategyId") Long strategyId);

    @ResultMap("strategy")
    @Select("SELECT * FROM dg_ai_optimization_strategy WHERE team_id = #{teamId} AND name = #{name}")
    AiOptimizationStrategyEntity selectByTeamAndName(@Param("teamId") String teamId, @Param("name") String name);
}
