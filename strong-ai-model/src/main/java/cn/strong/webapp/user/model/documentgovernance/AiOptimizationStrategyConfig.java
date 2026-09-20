package cn.strong.webapp.user.model.documentgovernance;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

/** AI 优化策略的结构化配置；严格校验由策略编译器负责。 */
@Data
public class AiOptimizationStrategyConfig implements Serializable {
    private Map<String, DirectionConfig> directions;
    private Protections protections;
    private Constraints constraints;
    private String customInstruction;
    private ModelConfig model;

    public static AiOptimizationStrategyConfig defaults() {
        AiOptimizationStrategyConfig config = new AiOptimizationStrategyConfig();
        Map<String, DirectionConfig> directions = new LinkedHashMap<>();
        directions.put(AiOptimizationDirection.FORMAT.getCode(), direction(true, AiOptimizationIntensity.MEDIUM));
        directions.put(AiOptimizationDirection.STRUCTURE.getCode(), direction(true, AiOptimizationIntensity.HIGH));
        directions.put(AiOptimizationDirection.LANGUAGE.getCode(), direction(true, AiOptimizationIntensity.MEDIUM));
        directions.put(AiOptimizationDirection.TECHNICAL_ACCURACY.getCode(), direction(true, AiOptimizationIntensity.HIGH));
        directions.put(AiOptimizationDirection.TERMINOLOGY.getCode(), direction(true, AiOptimizationIntensity.MEDIUM));
        directions.put(AiOptimizationDirection.COMPLETENESS.getCode(), direction(false, AiOptimizationIntensity.LOW));
        config.setDirections(directions);
        Protections protections = new Protections();
        protections.setCodeBlocks(true);
        protections.setLinks(true);
        protections.setCommands(true);
        protections.setVariables(true);
        protections.setApiFields(true);
        protections.setProperNouns(true);
        config.setProtections(protections);
        Constraints constraints = new Constraints();
        constraints.setAllowNewFacts(false);
        constraints.setAllowSectionReorder(false);
        constraints.setAllowExamples(false);
        constraints.setTone("专业、简洁");
        constraints.setMode("format");
        config.setConstraints(constraints);
        config.setCustomInstruction("");
        ModelConfig model = new ModelConfig();
        model.setProvider("openai");
        model.setName("gpt-5.6-sol");
        model.setDeepThinking(false);
        model.setMaxTokens(8192);
        config.setModel(model);
        return config;
    }

    private static DirectionConfig direction(boolean enabled, AiOptimizationIntensity intensity) {
        DirectionConfig direction = new DirectionConfig();
        direction.setEnabled(enabled);
        direction.setIntensity(intensity);
        return direction;
    }

    @Data
    public static class DirectionConfig implements Serializable {
        private Boolean enabled;
        private AiOptimizationIntensity intensity;
    }

    @Data
    public static class Protections implements Serializable {
        private Boolean codeBlocks;
        private Boolean links;
        private Boolean commands;
        private Boolean variables;
        private Boolean apiFields;
        private Boolean properNouns;
    }

    @Data
    public static class Constraints implements Serializable {
        private Boolean allowNewFacts;
        private Boolean allowSectionReorder;
        private Boolean allowExamples;
        private String tone;
        private String mode;
    }

    @Data
    public static class ModelConfig implements Serializable {
        private String provider;
        private String name;
        private Boolean deepThinking;
        private Integer maxTokens;
    }
}
