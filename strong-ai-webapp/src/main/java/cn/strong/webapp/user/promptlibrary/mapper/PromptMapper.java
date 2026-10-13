package cn.strong.webapp.user.promptlibrary.mapper;

import cn.strong.mybatis.plus.extend.CustomBaseMapper;
import cn.strong.webapp.user.promptlibrary.entity.PromptEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PromptMapper extends CustomBaseMapper<PromptEntity> {

    String COLUMNS = "id, category, title, description, tags, current_version_no, rating, "
            + "favorite_count, created_at, updated_at";

    @Select("<script>SELECT " + COLUMNS + " FROM ai_prompt WHERE 1 = 1 "
            + "<if test=\"category != null and category != ''\">AND category = #{category} </if>"
            + "<if test=\"tag != null and tag != ''\">AND FIND_IN_SET(#{tag}, tags) &gt; 0 </if>"
            + "<if test=\"q != null and q != ''\">AND (title LIKE CONCAT('%', #{q}, '%') "
            + "OR description LIKE CONCAT('%', #{q}, '%') OR tags LIKE CONCAT('%', #{q}, '%')) </if>"
            + "ORDER BY updated_at DESC, id DESC</script>")
    List<PromptEntity> findList(@Param("category") String category, @Param("tag") String tag,
                                @Param("q") String q);

    @Select("SELECT " + COLUMNS + ", content FROM ai_prompt WHERE id = #{id}")
    PromptEntity findById(@Param("id") Long id);

    @Select("SELECT " + COLUMNS + ", content FROM ai_prompt WHERE id = #{id} FOR UPDATE")
    PromptEntity findByIdForUpdate(@Param("id") Long id);

    @Insert("INSERT INTO ai_prompt (category, title, description, tags, content, current_version_no, rating, "
            + "favorite_count, created_at, updated_at) VALUES (#{category}, #{title}, #{description}, #{tags}, "
            + "#{content}, #{currentVersionNo}, #{rating}, #{favoriteCount}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertPrompt(PromptEntity prompt);

    @Update("UPDATE ai_prompt SET category = #{category}, title = #{title}, description = #{description}, "
            + "tags = #{tags}, content = #{content}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateContent(PromptEntity prompt);

    @Update("UPDATE ai_prompt SET current_version_no = #{currentVersionNo}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateVersionNo(@Param("id") Long id, @Param("currentVersionNo") Long currentVersionNo,
                        @Param("updatedAt") Long updatedAt);

    @Update("UPDATE ai_prompt SET content = #{content}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateWorkingContent(@Param("id") Long id, @Param("content") String content,
                             @Param("updatedAt") Long updatedAt);

    @Update("UPDATE ai_prompt SET rating = #{score}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateRating(@Param("id") Long id, @Param("score") Integer score, @Param("updatedAt") Long updatedAt);

    @Update("UPDATE ai_prompt SET favorite_count = GREATEST(0, favorite_count + #{delta}), updated_at = #{updatedAt} "
            + "WHERE id = #{id}")
    int updateFavoriteCount(@Param("id") Long id, @Param("delta") int delta, @Param("updatedAt") Long updatedAt);

    @Delete("DELETE FROM ai_prompt WHERE id = #{id}")
    int deleteByPromptId(@Param("id") Long id);
}
