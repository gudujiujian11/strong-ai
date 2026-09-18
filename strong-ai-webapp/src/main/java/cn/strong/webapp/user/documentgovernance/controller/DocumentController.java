package cn.strong.webapp.user.documentgovernance.controller;

import cn.strong.common.base.Result;
import cn.strong.webapp.user.documentgovernance.context.DocumentIdentity;
import cn.strong.webapp.user.documentgovernance.context.DocumentIdentityResolver;
import cn.strong.webapp.user.documentgovernance.entity.TermEntity;
import cn.strong.webapp.user.documentgovernance.service.DocumentAiTaskService;
import cn.strong.webapp.user.documentgovernance.service.TermService;
import cn.strong.webapp.user.model.documentgovernance.AiTaskResponse;
import cn.strong.webapp.user.model.documentgovernance.TermRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/document-governance")
@Api(tags = {"v1.0", "document-governance"}, description = "技术文档 AI 任务接口")
public class DocumentController {

    private final DocumentAiTaskService taskService;
    private final TermService termService;
    private final DocumentIdentityResolver identityResolver;

    public DocumentController(DocumentAiTaskService taskService, TermService termService,
                              DocumentIdentityResolver identityResolver) {
        this.taskService = taskService;
        this.termService = termService;
        this.identityResolver = identityResolver;
    }

    @GetMapping("/ai-tasks/{taskId}")
    @ApiOperation("查询文档 AI 任务")
    public Result<AiTaskResponse> getAiTask(@PathVariable("taskId") Long taskId) {
        return Result.ok(taskService.get(taskId));
    }

    @GetMapping(value = "/ai-tasks/{taskId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation("订阅文档 AI 任务状态")
    public SseEmitter aiTaskEvents(@PathVariable("taskId") Long taskId) {
        return taskService.events(taskId);
    }

    @GetMapping("/identity")
    @ApiOperation("查询当前文档治理身份")
    public Result<Map<String, String>> identity() {
        DocumentIdentity identity = identityResolver.current();
        Map<String, String> response = new HashMap<>();
        response.put("teamId", identity.getTeamId());
        response.put("role", identity.getRole());
        return Result.ok(response);
    }

    @GetMapping("/terms")
    @ApiOperation("查询当前团队术语")
    public Result<List<TermEntity>> terms() {
        return Result.ok(termService.list(identityResolver.current().getTeamId()));
    }

    @PutMapping("/terms")
    @ApiOperation("维护当前团队术语")
    public Result<TermEntity> upsertTerm(@RequestBody TermRequest request) {
        return Result.ok(termService.upsert(request));
    }
}
