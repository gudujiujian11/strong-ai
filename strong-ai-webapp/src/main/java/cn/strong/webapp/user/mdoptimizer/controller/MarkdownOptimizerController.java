package cn.strong.webapp.user.mdoptimizer.controller;

import cn.strong.common.base.Result;
import cn.strong.webapp.user.mdoptimizer.model.MarkdownOptimizeRequest;
import cn.strong.webapp.user.mdoptimizer.model.MarkdownOptimizeResponse;
import cn.strong.webapp.user.mdoptimizer.service.MarkdownOptimizerService;
import cn.strong.webapp.user.mdoptimizer.validator.MarkdownDocumentValidator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/md-optimizer")
@Api(tags = {"v1.0", "md-optimizer"}, description = "Markdown 文档优化接口")
public class MarkdownOptimizerController {

    private final MarkdownOptimizerService markdownOptimizerService;

    public MarkdownOptimizerController(MarkdownOptimizerService markdownOptimizerService) {
        this.markdownOptimizerService = markdownOptimizerService;
    }

    @PostMapping("/validate")
    @ApiOperation("校验 Markdown 内容和文件名")
    public Result<MarkdownDocumentValidator.ValidationResult> validate(
            @RequestBody MarkdownOptimizeRequest request) {
        return Result.ok(markdownOptimizerService.validate(request));
    }

    @PostMapping("/optimize")
    @ApiOperation("同步优化 Markdown 内容")
    public Result<MarkdownOptimizeResponse> optimize(@RequestBody MarkdownOptimizeRequest request) {
        return Result.ok(markdownOptimizerService.optimize(request));
    }
}
