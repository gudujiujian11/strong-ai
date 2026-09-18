package cn.strong.webapp.user.documentgovernance.mapper;

import cn.strong.mybatis.plus.extend.CustomBaseMapper;
import cn.strong.webapp.user.documentgovernance.entity.DocumentVersionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DocumentVersionMapper extends CustomBaseMapper<DocumentVersionEntity> {

    @Select("SELECT COALESCE(MAX(version_no), 0) FROM dg_document_version WHERE document_id = #{documentId}")
    Long maxVersionNo(@Param("documentId") Long documentId);

    @Select("SELECT * FROM dg_document_version WHERE document_id = #{documentId} AND version_no = #{versionNo}")
    DocumentVersionEntity selectByDocumentAndNo(@Param("documentId") Long documentId,
                                                @Param("versionNo") Long versionNo);

    @Select("SELECT * FROM dg_document_version WHERE team_id = #{teamId} AND document_id = #{documentId} ORDER BY version_no DESC")
    List<DocumentVersionEntity> selectByTeamAndDocument(@Param("teamId") String teamId,
                                                         @Param("documentId") Long documentId);

    @Select("SELECT * FROM dg_document_version WHERE team_id = #{teamId} AND id = #{versionId}")
    DocumentVersionEntity selectByTeamAndId(@Param("teamId") String teamId, @Param("versionId") Long versionId);
}
