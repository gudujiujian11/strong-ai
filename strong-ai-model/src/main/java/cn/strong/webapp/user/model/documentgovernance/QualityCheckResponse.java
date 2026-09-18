package cn.strong.webapp.user.model.documentgovernance;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import lombok.Data;

@Data
public class QualityCheckResponse implements Serializable {
    private Long reportId;
    private Long documentId;
    private Long documentVersionId;
    private Integer score;
    private Long createdAt;
    private List<QualityIssue> issues = Collections.emptyList();
}
