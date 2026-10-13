package cn.strong.webapp.user.promptlibrary.model;

import lombok.Data;

@Data
public class PromptSummary {
    private Long id;
    private String category;
    private String title;
    private String description;
    private String tags;
    private Long currentVersionNo;
    private Integer rating;
    private Integer favoriteCount;
    private Long createdAt;
    private Long updatedAt;
}
