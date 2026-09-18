package cn.strong.webapp.user.documentgovernance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("dg_document")
public class DocumentEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String teamId;
    private String ownerUserId;
    private String title;
    private String documentType;
    private String tags;
    private String status;
    private String draftContent;
    private Long currentVersionId;
    private Long createdAt;
    private Long updatedAt;
}
