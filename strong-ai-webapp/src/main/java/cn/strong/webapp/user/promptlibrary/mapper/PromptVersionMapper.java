package cn.strong.webapp.user.promptlibrary.mapper;

import cn.strong.mybatis.plus.extend.CustomBaseMapper;
import cn.strong.webapp.user.promptlibrary.entity.PromptVersionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PromptVersionMapper extends CustomBaseMapper<PromptVersionEntity> {

    @Insert("INSERT INTO ai_prompt_version (prompt_id, version_no, content, created_at) "
            + "VALUES (#{promptId}, #{versionNo}, #{content}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertVersion(PromptVersionEntity version);

    @Select("SELECT id, prompt_id, version_no, created_at FROM ai_prompt_version "
            + "WHERE prompt_id = #{promptId} ORDER BY version_no DESC")
    List<PromptVersionEntity> findByPromptId(@Param("promptId") Long promptId);

    @Select("SELECT id, prompt_id, version_no, content, created_at FROM ai_prompt_version "
            + "WHERE prompt_id = #{promptId} AND version_no = #{versionNo}")
    PromptVersionEntity findOne(@Param("promptId") Long promptId, @Param("versionNo") Long versionNo);

    @Select("SELECT content FROM ai_prompt_version WHERE prompt_id = #{promptId} ORDER BY version_no DESC LIMIT 1")
    String findLatestContent(@Param("promptId") Long promptId);

    @Delete("DELETE FROM ai_prompt_version WHERE prompt_id = #{promptId}")
    int deleteByPromptId(@Param("promptId") Long promptId);
}
