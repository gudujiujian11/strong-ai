package cn.strong.webapp.user.model.documentgovernance;

import java.io.Serializable;
import lombok.Data;

@Data
public class AiOptimizeRequest implements Serializable {
    private String idempotencyKey;
    private String provider = "openai";
    private String model = "gpt-5.6-sol";
    private String mode = "format";
    private Boolean deepThinking = Boolean.FALSE;
    private Integer maxTokens = 8192;
}
