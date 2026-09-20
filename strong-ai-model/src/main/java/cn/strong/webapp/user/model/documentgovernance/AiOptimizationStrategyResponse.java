package cn.strong.webapp.user.model.documentgovernance;

import java.io.Serializable;
import lombok.Data;

@Data
public class AiOptimizationStrategyResponse implements Serializable {
    private Long id;
    private String name;
    private String description;
    private String status;
    private Long currentVersionId;
    private Long currentVersionNo;
    private Boolean defaultStrategy;
    private String createdBy;
    private Long createdAt;
    private Long updatedAt;
}
