package cn.strong.webapp.user.promptlibrary.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class PromptVersion {
    private Long id;
    private Long promptId;
    private Long versionNo;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String content;
    private Long createdAt;
}
