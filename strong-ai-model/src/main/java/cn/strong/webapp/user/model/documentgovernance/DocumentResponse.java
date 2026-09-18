package cn.strong.webapp.user.model.documentgovernance;

import java.io.Serializable;
import lombok.Data;

@Data
public class DocumentResponse implements Serializable {
    private Long id;
    private String teamId;
    private String ownerUserId;
    private String title;
    private String documentType;
    private String tags;
    private DocumentStatus status;
    private String draftContent;
    private Long currentVersionId;
    private Long createdAt;
    private Long updatedAt;
}
