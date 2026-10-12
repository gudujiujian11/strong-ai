package cn.strong.webapp.user.mdoptimizer.mapper;

import cn.strong.mybatis.plus.extend.CustomBaseMapper;
import cn.strong.webapp.user.mdoptimizer.entity.ImageParseHistoryEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ImageParseHistoryMapper extends CustomBaseMapper<ImageParseHistoryEntity> {

    String COLUMNS = "id, original_name, content_type, size_bytes, provider, model, created_at";

    @Insert("INSERT INTO ai_image_parse_history (id, original_name, content_type, size_bytes, content, "
            + "result_json, provider, model, created_at) VALUES (#{id}, #{originalName}, #{contentType}, "
            + "#{sizeBytes}, #{content}, #{resultJson}, #{provider}, #{model}, #{createdAt})")
    int insert(ImageParseHistoryEntity history);

    @Select("SELECT COUNT(*) FROM ai_image_parse_history")
    long countAll();

    @Select("SELECT " + COLUMNS + " FROM ai_image_parse_history ORDER BY created_at DESC, id DESC "
            + "LIMIT #{pageSize} OFFSET #{offset}")
    List<ImageParseHistoryEntity> findPage(@Param("offset") long offset, @Param("pageSize") int pageSize);

    @Select("SELECT " + COLUMNS + ", result_json FROM ai_image_parse_history WHERE id = #{id}")
    ImageParseHistoryEntity findDetail(@Param("id") String id);

    @Select("SELECT id, content_type, content FROM ai_image_parse_history WHERE id = #{id}")
    ImageParseHistoryEntity findImage(@Param("id") String id);

    @Delete("DELETE FROM ai_image_parse_history WHERE id = #{id}")
    int deleteHistory(@Param("id") String id);
}
