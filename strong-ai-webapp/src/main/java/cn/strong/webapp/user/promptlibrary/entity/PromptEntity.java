package cn.strong.webapp.user.promptlibrary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ai_prompt")
public class PromptEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String category;
    private String title;
    private String description;
    private String tags;
    private String content;
    private Long currentVersionNo;
    private Integer rating;
    private Integer favoriteCount;
    private Long createdAt;
    private Long updatedAt;
}
