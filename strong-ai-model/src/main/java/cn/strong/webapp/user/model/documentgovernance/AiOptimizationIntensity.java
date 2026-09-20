package cn.strong.webapp.user.model.documentgovernance;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AiOptimizationIntensity {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");

    private final String code;

    AiOptimizationIntensity(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }
}
