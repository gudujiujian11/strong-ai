package cn.strong.webapp.user.documentgovernance.mapper;

import cn.strong.mybatis.plus.extend.CustomBaseMapper;
import cn.strong.webapp.user.documentgovernance.entity.TermEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TermMapper extends CustomBaseMapper<TermEntity> {

    @Select("SELECT * FROM dg_term WHERE team_id = #{teamId} ORDER BY updated_at DESC")
    List<TermEntity> selectByTeamId(@Param("teamId") String teamId);

    @Select("SELECT * FROM dg_term WHERE team_id = #{teamId} AND term = #{term}")
    TermEntity selectByTeamAndTerm(@Param("teamId") String teamId, @Param("term") String term);
}
