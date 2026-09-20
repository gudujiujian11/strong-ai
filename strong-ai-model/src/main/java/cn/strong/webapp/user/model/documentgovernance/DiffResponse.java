package cn.strong.webapp.user.model.documentgovernance;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 两个内容之间的行级差异结果。为便于前端渲染，仅返回结构化 hunk，不在后端拼接 HTML。
 */
@Data
public class DiffResponse implements Serializable {
    /** 起始版本号；与草稿比较时为当前发布版本号，可能为 null */
    private Long fromVersionNo;
    /** 目标版本号；与草稿比较时为 null */
    private Long toVersionNo;
    /** 目标是否为当前草稿 */
    private boolean againstDraft;
    private List<DiffHunk> hunks;
}
