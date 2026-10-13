package cn.strong.webapp.user.promptlibrary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ai_prompt_version")
public class PromptVersionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long promptId;
    private Long versionNo;
    private String content;
    private Long createdAt;
}
