package cn.strong.webapp.user.model.documentgovernance;

import lombok.Getter;

@Getter
public class AiOptimizationStrategyCompiledContext {
    private final AiOptimizationStrategyConfig config;
    private final String configHash;
    private final String systemPrompt;
    private final String normalizedJson;

    public AiOptimizationStrategyCompiledContext(AiOptimizationStrategyConfig config,
                                                 String configHash, String systemPrompt) {
        this(config, configHash, systemPrompt, null);
    }

    public AiOptimizationStrategyCompiledContext(AiOptimizationStrategyConfig config,
                                                 String configHash, String systemPrompt, String normalizedJson) {
        this.config = config;
        this.configHash = configHash;
        this.systemPrompt = systemPrompt;
        this.normalizedJson = normalizedJson;
    }
}
