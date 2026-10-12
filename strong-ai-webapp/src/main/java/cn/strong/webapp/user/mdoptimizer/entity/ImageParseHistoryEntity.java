package cn.strong.webapp.user.mdoptimizer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ai_image_parse_history")
public class ImageParseHistoryEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    private String originalName;
    private String contentType;
    private Long sizeBytes;
    private byte[] content;
    private String resultJson;
    private String provider;
    private String model;
    private Long createdAt;
}
