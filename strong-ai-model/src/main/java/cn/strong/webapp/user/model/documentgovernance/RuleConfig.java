package cn.strong.webapp.user.model.documentgovernance;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * 确定性质量规则的生效配置（值对象）。
 *
 * <p>由团队规则集（dg_rule_set）反序列化得到；任意字段缺失时回退到内置默认值，
 * 保证规则集损坏或未配置时质量检查行为与历史一致。</p>
 */
public class RuleConfig implements Serializable {

    private final Map<String, Boolean> enabled;
    private final Map<String, QualitySeverity> severityOverrides;
    private final Map<QualitySeverity, Integer> scoreWeights;

    public RuleConfig(Map<String, Boolean> enabled,
                      Map<String, QualitySeverity> severityOverrides,
                      Map<QualitySeverity, Integer> scoreWeights) {
        this.enabled = enabled == null ? new HashMap<>() : new HashMap<>(enabled);
        this.severityOverrides = severityOverrides == null ? new HashMap<>() : new HashMap<>(severityOverrides);
        this.scoreWeights = (scoreWeights == null || scoreWeights.isEmpty())
                ? defaultWeights() : withDefaults(scoreWeights);
    }

    public static RuleConfig defaults() {
        return new RuleConfig(new HashMap<>(), new HashMap<>(), null);
    }

    /** 规则默认启用，只有显式配置为 false 才关闭。 */
    public boolean isEnabled(String ruleCode) {
        Boolean value = enabled.get(ruleCode);
        return value == null || value;
    }

    /** 有配置覆盖则用覆盖值，否则用规则内置的默认严重级别。 */
    public QualitySeverity severity(String ruleCode, QualitySeverity fallback) {
        QualitySeverity override = severityOverrides.get(ruleCode);
        return override == null ? fallback : override;
    }

    /** 按严重级别取扣分权重，缺省 HIGH=30 / MEDIUM=15 / LOW=5。 */
    public int weight(QualitySeverity severity) {
        Integer configured = scoreWeights.get(severity);
        return configured == null ? defaultWeight(severity) : configured;
    }

    private static Map<QualitySeverity, Integer> defaultWeights() {
        Map<QualitySeverity, Integer> weights = new EnumMap<>(QualitySeverity.class);
        weights.put(QualitySeverity.HIGH, 30);
        weights.put(QualitySeverity.MEDIUM, 15);
        weights.put(QualitySeverity.LOW, 5);
        return weights;
    }

    private static Map<QualitySeverity, Integer> withDefaults(Map<QualitySeverity, Integer> weights) {
        Map<QualitySeverity, Integer> merged = defaultWeights();
        merged.putAll(weights);
        return merged;
    }

    private static int defaultWeight(QualitySeverity severity) {
        switch (severity) {
            case HIGH:
                return 30;
            case MEDIUM:
                return 15;
            case LOW:
                return 5;
            default:
                return 0;
        }
    }
}
