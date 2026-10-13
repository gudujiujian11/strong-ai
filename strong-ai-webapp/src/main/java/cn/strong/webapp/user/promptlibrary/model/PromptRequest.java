package cn.strong.webapp.user.promptlibrary.model;

import lombok.Data;

@Data
public class PromptRequest {
    private String category;
    private String title;
    private String description;
    private String tags;
    private String content;
}
