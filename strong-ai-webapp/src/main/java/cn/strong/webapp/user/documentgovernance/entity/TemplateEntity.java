package cn.strong.webapp.user.documentgovernance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("dg_template")
public class TemplateEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String teamId;
    private String name;
    private String documentType;
    private String requiredHeadings;
    private String createdBy;
    private Long createdAt;
    private Long updatedAt;
}
