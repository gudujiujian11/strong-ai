package cn.strong.webapp.user.documentgovernance.controller;

import cn.strong.common.base.Result;
import cn.strong.webapp.user.documentgovernance.service.DocumentService;
import cn.strong.webapp.user.documentgovernance.service.DocumentAiTaskService;
import cn.strong.webapp.user.documentgovernance.service.QualityReportService;
import cn.strong.webapp.user.model.documentgovernance.AiOptimizeRequest;
import cn.strong.webapp.user.model.documentgovernance.AiTaskResponse;
import cn.strong.webapp.user.model.documentgovernance.DiffResponse;
import cn.strong.webapp.user.model.documentgovernance.DocumentCreateRequest;
import cn.strong.webapp.user.model.documentgovernance.DocumentResponse;
import cn.strong.webapp.user.model.documentgovernance.DocumentSaveRequest;
import cn.strong.webapp.user.model.documentgovernance.DocumentStatusChangeRequest;
import cn.strong.webapp.user.model.documentgovernance.DocumentVersionResponse;
import cn.strong.webapp.user.model.documentgovernance.DocumentVersionSummaryResponse;
import cn.strong.webapp.user.model.documentgovernance.QualityCheckResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/document-governance/documents")
@Api(tags = {"v1.0", "document-governance"}, description = "技术文档治理接口")
public class DocumentGovernanceController {

    private final DocumentService documentService;
    private final QualityReportService qualityReportService;
    private final DocumentAiTaskService documentAiTaskService;

    public DocumentGovernanceController(DocumentService documentService, QualityReportService qualityReportService,
                                        DocumentAiTaskService documentAiTaskService) {
        this.documentService = documentService;
        this.qualityReportService = qualityReportService;
        this.documentAiTaskService = documentAiTaskService;
    }

    @PostMapping
    @ApiOperation("创建技术文档草稿")
    public Result<DocumentResponse> create(@RequestBody DocumentCreateRequest request) {
        return Result.ok(documentService.create(request));
    }

    @GetMapping
    @ApiOperation("查询当前团队文档")
    public Result<List<DocumentResponse>> list() {
        return Result.ok(documentService.list());
    }

    @GetMapping("/{documentId}")
    @ApiOperation("查询文档详情")
    public Result<DocumentResponse> get(@PathVariable("documentId") Long documentId) {
        return Result.ok(documentService.get(documentId));
    }

    @PutMapping("/{documentId}/draft")
    @ApiOperation("保存文档草稿")
    public Result<DocumentResponse> saveDraft(@PathVariable("documentId") Long documentId,
                                               @RequestBody DocumentSaveRequest request) {
        return Result.ok(documentService.saveDraft(documentId, request));
    }

    @PostMapping("/{documentId}/publish")
    @ApiOperation("发布文档版本")
    public Result<DocumentVersionResponse> publish(@PathVariable("documentId") Long documentId,
                                                    @RequestParam(value = "releaseNote", required = false) String releaseNote) {
        return Result.ok(documentService.publish(documentId, releaseNote));
    }

    @PostMapping("/{documentId}/rollback/{versionNo}")
    @ApiOperation("回滚并创建新版本")
    public Result<DocumentVersionResponse> rollback(@PathVariable("documentId") Long documentId,
                                                     @PathVariable("versionNo") Long versionNo,
                                                     @RequestParam(value = "releaseNote", required = false) String releaseNote) {
        return Result.ok(documentService.rollback(documentId, versionNo, releaseNote));
    }

    @GetMapping("/{documentId}/versions")
    @ApiOperation("查询文档版本历史（轻量摘要，不含全文）")
    public Result<List<DocumentVersionSummaryResponse>> versions(@PathVariable("documentId") Long documentId) {
        return Result.ok(documentService.listVersionSummaries(documentId));
    }

    @GetMapping("/{documentId}/versions/{versionNo}")
    @ApiOperation("查询单个版本详情（含全文）")
    public Result<DocumentVersionResponse> version(@PathVariable("documentId") Long documentId,
                                                   @PathVariable("versionNo") Long versionNo) {
        return Result.ok(documentService.getVersion(documentId, versionNo));
    }

    @GetMapping("/{documentId}/diff")
    @ApiOperation("比较任意两个版本，或版本与当前草稿（to 传 draft）")
    public Result<DiffResponse> diff(@PathVariable("documentId") Long documentId,
                                     @RequestParam("from") Long from,
                                     @RequestParam("to") String to) {
        return Result.ok(documentService.diff(documentId, from, to));
    }

    @PostMapping("/{documentId}/status")
    @ApiOperation("变更文档状态（归档或恢复）")
    public Result<DocumentResponse> changeStatus(@PathVariable("documentId") Long documentId,
                                                 @RequestBody DocumentStatusChangeRequest request) {
        return Result.ok(documentService.changeStatus(documentId, request == null ? null : request.getStatus()));
    }

    @PostMapping("/{documentId}/quality-check")
    @ApiOperation("运行确定性质量检查")
    public Result<QualityCheckResponse> qualityCheck(@PathVariable("documentId") Long documentId) {
        return Result.ok(qualityReportService.checkAndSave(documentId));
    }

    @PostMapping("/{documentId}/ai-tasks")
    @ApiOperation("提交文档 AI 优化任务")
    public Result<AiTaskResponse> submitAiTask(@PathVariable("documentId") Long documentId,
                                                @RequestBody AiOptimizeRequest request) {
        return Result.ok(documentAiTaskService.submit(documentId, request));
    }
}
