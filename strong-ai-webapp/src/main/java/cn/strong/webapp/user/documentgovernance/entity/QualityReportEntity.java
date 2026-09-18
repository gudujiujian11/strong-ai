package cn.strong.webapp.user.documentgovernance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("dg_quality_report")
public class QualityReportEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String teamId;
    private Long documentId;
    private Long documentVersionId;
    private Integer score;
    private String checkedBy;
    private Long createdAt;
}
