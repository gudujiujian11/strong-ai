package cn.strong.webapp.user.model.documentgovernance;

import java.io.Serializable;
import lombok.Data;

/**
 * 版本列表用轻量摘要，不含全文内容，避免列表响应膨胀。全文通过单版本详情接口按需获取。
 */
@Data
public class DocumentVersionSummaryResponse implements Serializable {
    private Long id;
    private Long documentId;
    private Long versionNo;
    private String releaseNote;
    private String createdBy;
    private Long createdAt;
}
