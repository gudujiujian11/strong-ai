package cn.strong.webapp.user.model.documentgovernance;

import java.io.Serializable;
import lombok.Data;

@Data
public class DocumentStatusChangeRequest implements Serializable {
    /** 目标状态：PUBLISHED（恢复）或 ARCHIVED（归档） */
    private DocumentStatus status;
}
