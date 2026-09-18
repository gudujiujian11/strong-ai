package cn.strong.webapp.user.model.documentgovernance;

import java.io.Serializable;
import lombok.Data;

@Data
public class AiTaskResponse implements Serializable {
    private Long id;
    private Long documentId;
    private String status;
    private String provider;
    private String model;
    private String resultSummary;
    private String errorCode;
    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;
    private Long createdAt;
    private Long updatedAt;
    private Long expiresAt;
}
