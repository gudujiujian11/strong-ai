package cn.strong.webapp.user.model.documentgovernance;

import java.io.Serializable;
import lombok.Data;

@Data
public class RuleSetRequest implements Serializable {
    private String name;
    private String configJson;
    private Boolean enabled;

    public RuleSetRequest() {
    }

    public RuleSetRequest(String name, String configJson, Boolean enabled) {
        this.name = name;
        this.configJson = configJson;
        this.enabled = enabled;
    }
}
