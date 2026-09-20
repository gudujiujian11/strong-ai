package cn.strong.webapp.user.model.documentgovernance;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AiOptimizationDirection {
    FORMAT("format"),
    STRUCTURE("structure"),
    LANGUAGE("language"),
    TECHNICAL_ACCURACY("technicalAccuracy"),
    TERMINOLOGY("terminology"),
    COMPLETENESS("completeness");

    private final String code;

    AiOptimizationDirection(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }
}
