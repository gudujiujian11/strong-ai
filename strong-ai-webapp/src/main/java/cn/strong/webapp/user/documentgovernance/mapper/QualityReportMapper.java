package cn.strong.webapp.user.documentgovernance.mapper;

import cn.strong.mybatis.plus.extend.CustomBaseMapper;
import cn.strong.webapp.user.documentgovernance.entity.QualityReportEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QualityReportMapper extends CustomBaseMapper<QualityReportEntity> {

    @Select("SELECT * FROM dg_quality_report WHERE team_id = #{teamId} AND document_id = #{documentId} "
            + "ORDER BY created_at DESC LIMIT 1")
    QualityReportEntity selectLatestByDocument(@Param("teamId") String teamId,
                                               @Param("documentId") Long documentId);

    @Select("SELECT * FROM dg_quality_report WHERE team_id = #{teamId} AND document_id = #{documentId} "
            + "ORDER BY created_at DESC")
    List<QualityReportEntity> selectByDocument(@Param("teamId") String teamId,
                                               @Param("documentId") Long documentId);
}
