package cn.strong.webapp.user.model.documentgovernance;

import java.io.Serializable;
import lombok.Data;

@Data
public class AiOptimizationStrategyRequest implements Serializable {
    private String name;
    private String description;
    private AiOptimizationStrategyConfig config;
}
