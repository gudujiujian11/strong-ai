package cn.strong.webapp.user.model.documentgovernance;

import java.io.Serializable;
import lombok.Data;

@Data
public class DocumentSaveRequest implements Serializable {
    private String content;
    private String tags = "";
}
