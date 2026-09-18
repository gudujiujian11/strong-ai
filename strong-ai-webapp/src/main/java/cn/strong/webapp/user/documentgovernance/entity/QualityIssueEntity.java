package cn.strong.webapp.user.documentgovernance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("dg_quality_issue")
public class QualityIssueEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String teamId;
    private Long reportId;
    private String ruleCode;
    private String severity;
    private Integer lineNumber;
    private String message;
    private String suggestion;
}
