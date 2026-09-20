package cn.strong.webapp.user.model.documentgovernance;

import java.io.Serializable;
import lombok.Data;

/**
 * 一行差异。type 为 EQUAL / INSERT / DELETE；oldLine、newLine 为 1 起始行号，不适用时为 null。
 */
@Data
public class DiffHunk implements Serializable {
    private String type;
    private Integer oldLine;
    private Integer newLine;
    private String text;
}
