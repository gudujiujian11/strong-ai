package cn.strong.webapp.user.model.documentgovernance;

import java.io.Serializable;
import lombok.Data;

@Data
public class DocumentVersionResponse implements Serializable {
    private Long id;
    private Long documentId;
    private Long versionNo;
    private String content;
    private String diffContent;
    private String releaseNote;
    private String createdBy;
    private Long createdAt;
}
