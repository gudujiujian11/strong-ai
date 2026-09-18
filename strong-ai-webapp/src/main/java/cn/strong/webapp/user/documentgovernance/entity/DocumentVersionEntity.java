package cn.strong.webapp.user.documentgovernance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("dg_document_version")
public class DocumentVersionEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long documentId;
    private String teamId;
    private Long versionNo;
    private String content;
    private String diffContent;
    private String releaseNote;
    private String createdBy;
    private Long createdAt;
}
