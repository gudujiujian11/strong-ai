package cn.strong.webapp.user.documentgovernance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("dg_ai_task")
public class AiTaskEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String teamId;
    private Long documentId;
    private String idempotencyKey;
    private Long strategyId;
    private Long strategyVersionId;
    private String strategyConfigHash;
    private String createdBy;
    private String status;
    private String provider;
    private String model;
    private String resultSummary;
    private String errorCode;
    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;
    private Long createdAt;
    private Long updatedAt;
    private Long expiresAt;
}
