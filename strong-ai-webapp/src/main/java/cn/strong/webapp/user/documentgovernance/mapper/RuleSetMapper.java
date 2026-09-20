package cn.strong.webapp.user.documentgovernance.mapper;

import cn.strong.mybatis.plus.extend.CustomBaseMapper;
import cn.strong.webapp.user.documentgovernance.entity.RuleSetEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RuleSetMapper extends CustomBaseMapper<RuleSetEntity> {

    @Select("SELECT * FROM dg_rule_set WHERE team_id = #{teamId} ORDER BY updated_at DESC")
    List<RuleSetEntity> selectByTeamId(@Param("teamId") String teamId);

    @Select("SELECT * FROM dg_rule_set WHERE team_id = #{teamId} AND enabled = 1 ORDER BY updated_at ASC")
    List<RuleSetEntity> selectEnabledByTeamId(@Param("teamId") String teamId);

    @Select("SELECT * FROM dg_rule_set WHERE team_id = #{teamId} AND name = #{name}")
    RuleSetEntity selectByTeamAndName(@Param("teamId") String teamId, @Param("name") String name);
}
