package cn.strong.webapp.user.mdoptimizer.model;

import java.io.Serializable;
import lombok.Data;

@Data
public class ImageParseHistory implements Serializable {

    private String id;
    private String originalName;
    private String contentType;
    private Long sizeBytes;
    private String provider;
    private String model;
    private Long createdAt;
    private MarkdownOptimizeResponse result;
}
