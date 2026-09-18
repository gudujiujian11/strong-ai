package cn.strong.webapp.user.documentgovernance.mapper;

import cn.strong.mybatis.plus.extend.CustomBaseMapper;
import cn.strong.webapp.user.documentgovernance.entity.DocumentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DocumentMapper extends CustomBaseMapper<DocumentEntity> {

    @Select("SELECT * FROM dg_document WHERE id = #{documentId} AND team_id = #{teamId}")
    DocumentEntity selectByTeamAndId(@Param("documentId") Long documentId, @Param("teamId") String teamId);

    @Delete("DELETE d FROM dg_document d WHERE d.status = 'DRAFT' AND d.updated_at < #{cutoff} "
            + "AND NOT EXISTS (SELECT 1 FROM dg_document_version v WHERE v.document_id = d.id)")
    int deleteExpiredDrafts(@Param("cutoff") long cutoff);
}
