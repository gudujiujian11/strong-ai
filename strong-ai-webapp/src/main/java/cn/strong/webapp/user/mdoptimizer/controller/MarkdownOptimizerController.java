package cn.strong.webapp.user.mdoptimizer.controller;

import cn.strong.common.base.Result;
import cn.strong.webapp.user.mdoptimizer.model.MarkdownOptimizeRequest;
import cn.strong.webapp.user.mdoptimizer.model.MarkdownOptimizeResponse;
import cn.strong.webapp.user.mdoptimizer.service.MarkdownOptimizerService;
import cn.strong.webapp.user.mdoptimizer.service.ImageMarkdownParserService;
import cn.strong.webapp.user.mdoptimizer.validator.MarkdownDocumentValidator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/md-optimizer")
@Api(tags = {"v1.0", "md-optimizer"}, description = "Markdown 文档优化接口")
public class MarkdownOptimizerController {

    private final MarkdownOptimizerService markdownOptimizerService;
    private final ImageMarkdownParserService imageMarkdownParserService;

    public MarkdownOptimizerController(MarkdownOptimizerService markdownOptimizerService,
                                       ImageMarkdownParserService imageMarkdownParserService) {
        this.markdownOptimizerService = markdownOptimizerService;
        this.imageMarkdownParserService = imageMarkdownParserService;
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

    @PostMapping(value = "/parse-image", consumes = "multipart/form-data")
    @ApiOperation("解析图片并生成 Markdown")
    public Result<MarkdownOptimizeResponse> parseImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "provider", required = false, defaultValue = "openai") String provider,
            @RequestParam(value = "model", required = false, defaultValue = "gpt-5.6-sol") String model,
            @RequestParam(value = "maxTokens", required = false, defaultValue = "8192") Integer maxTokens) {
        return Result.ok(imageMarkdownParserService.parse(image, provider, model, maxTokens));
    }
}
