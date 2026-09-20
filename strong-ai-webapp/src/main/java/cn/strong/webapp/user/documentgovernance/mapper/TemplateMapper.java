package cn.strong.webapp.user.documentgovernance.mapper;

import cn.strong.mybatis.plus.extend.CustomBaseMapper;
import cn.strong.webapp.user.documentgovernance.entity.TemplateEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TemplateMapper extends CustomBaseMapper<TemplateEntity> {

    @Select("SELECT * FROM dg_template WHERE team_id = #{teamId} AND document_type = #{documentType} ORDER BY updated_at DESC")
    List<TemplateEntity> selectByTeamAndDocumentType(@Param("teamId") String teamId,
                                                      @Param("documentType") String documentType);

    @Select("SELECT * FROM dg_template WHERE team_id = #{teamId} AND name = #{name}")
    TemplateEntity selectByTeamAndName(@Param("teamId") String teamId, @Param("name") String name);

    @Select("SELECT * FROM dg_template WHERE team_id = #{teamId} ORDER BY updated_at DESC")
    List<TemplateEntity> selectByTeamId(@Param("teamId") String teamId);
}
