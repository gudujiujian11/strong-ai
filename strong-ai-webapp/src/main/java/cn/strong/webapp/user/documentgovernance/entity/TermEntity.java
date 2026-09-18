package cn.strong.webapp.user.documentgovernance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("dg_term")
public class TermEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String teamId;
    private String term;
    private String preferredTerm;
    private Boolean forbidden;
    private String description;
    private String createdBy;
    private Long createdAt;
    private Long updatedAt;
}
