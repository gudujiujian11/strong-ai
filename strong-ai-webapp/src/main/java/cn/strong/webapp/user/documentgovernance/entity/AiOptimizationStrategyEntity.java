package cn.strong.webapp.user.documentgovernance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("dg_ai_optimization_strategy")
public class AiOptimizationStrategyEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String teamId;
    private String name;
    private String description;
    private String status;
    private Long currentVersionId;
    @TableField("is_default")
    private Boolean defaultStrategy;
    private String createdBy;
    private Long createdAt;
    private Long updatedAt;
}
