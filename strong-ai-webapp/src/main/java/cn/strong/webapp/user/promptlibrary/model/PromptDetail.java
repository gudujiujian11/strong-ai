package cn.strong.webapp.user.promptlibrary.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PromptDetail extends PromptSummary {
    private String content;
}
