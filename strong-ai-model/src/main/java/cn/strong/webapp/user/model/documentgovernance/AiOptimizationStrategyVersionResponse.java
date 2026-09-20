package cn.strong.webapp.user.model.documentgovernance;

import java.io.Serializable;
import lombok.Data;

@Data
public class AiOptimizationStrategyVersionResponse implements Serializable {
    private Long id;
    private Long strategyId;
    private Long versionNo;
    private String status;
    private AiOptimizationStrategyConfig config;
    private String configHash;
    private String createdBy;
    private Long createdAt;
    private Long publishedAt;
}
