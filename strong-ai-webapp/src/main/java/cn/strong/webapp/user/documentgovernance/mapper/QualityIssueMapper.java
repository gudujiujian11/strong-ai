package cn.strong.webapp.user.documentgovernance.mapper;

import cn.strong.mybatis.plus.extend.CustomBaseMapper;
import cn.strong.webapp.user.documentgovernance.entity.QualityIssueEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QualityIssueMapper extends CustomBaseMapper<QualityIssueEntity> {

    @Select("SELECT * FROM dg_quality_issue WHERE team_id = #{teamId} AND report_id = #{reportId} ORDER BY id ASC")
    List<QualityIssueEntity> selectByReportId(@Param("teamId") String teamId, @Param("reportId") Long reportId);
}
