package cn.strong.webapp.user.model.documentgovernance;

import java.io.Serializable;
import lombok.Data;

@Data
public class QualityIssue implements Serializable {
    private String ruleCode;
    private QualitySeverity severity;
    private int lineNumber;
    private String message;
    private String suggestion;
}
