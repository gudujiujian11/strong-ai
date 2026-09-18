package cn.strong.webapp.user.model.documentgovernance;

import java.io.Serializable;
import lombok.Data;

@Data
public class TermRequest implements Serializable {
    private String term;
    private String preferredTerm;
    private Boolean forbidden;
    private String description;

    public TermRequest() {
    }

    public TermRequest(String term, String preferredTerm, String description) {
        this.term = term;
        this.preferredTerm = preferredTerm;
        this.description = description;
    }
}
