package cn.strong.webapp.user.mdoptimizer.model;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class ImageParseHistoryPage implements Serializable {

    private List<ImageParseHistory> items;
    private Long total;
    private Integer page;
    private Integer pageSize;
}
