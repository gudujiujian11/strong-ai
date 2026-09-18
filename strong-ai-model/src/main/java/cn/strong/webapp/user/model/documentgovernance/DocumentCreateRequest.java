package cn.strong.webapp.user.model.documentgovernance;

import java.io.Serializable;
import lombok.Data;

@Data
public class DocumentCreateRequest implements Serializable {
    private String title;
    private String documentType;
    private String content;
    private String tags = "";
}
