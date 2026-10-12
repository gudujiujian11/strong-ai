package cn.strong.webapp.user.mdoptimizer.controller;

import cn.strong.common.base.Result;
import cn.strong.webapp.user.mdoptimizer.entity.ImageParseHistoryEntity;
import cn.strong.webapp.user.mdoptimizer.model.ImageParseHistory;
import cn.strong.webapp.user.mdoptimizer.model.ImageParseHistoryPage;
import cn.strong.webapp.user.mdoptimizer.model.MarkdownOptimizeRequest;
import cn.strong.webapp.user.mdoptimizer.model.MarkdownOptimizeResponse;
import cn.strong.webapp.user.mdoptimizer.service.MarkdownOptimizerService;
import cn.strong.webapp.user.mdoptimizer.service.ImageMarkdownParserService;
import cn.strong.webapp.user.mdoptimizer.service.ImageParseHistoryService;
import cn.strong.webapp.user.mdoptimizer.validator.MarkdownDocumentValidator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final ImageParseHistoryService imageParseHistoryService;

    public MarkdownOptimizerController(MarkdownOptimizerService markdownOptimizerService,
                                       ImageMarkdownParserService imageMarkdownParserService,
                                       ImageParseHistoryService imageParseHistoryService) {
        this.markdownOptimizerService = markdownOptimizerService;
        this.imageMarkdownParserService = imageMarkdownParserService;
        this.imageParseHistoryService = imageParseHistoryService;
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

    @GetMapping("/image-history")
    @ApiOperation("分页查看图片解析历史")
    public Result<ImageParseHistoryPage> imageHistory(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        return Result.ok(imageParseHistoryService.list(page, pageSize));
    }

    @GetMapping("/image-history/{id}")
    @ApiOperation("查看图片解析历史详情")
    public Result<ImageParseHistory> imageHistoryDetail(@PathVariable("id") String id) {
        return Result.ok(imageParseHistoryService.get(id));
    }

    @GetMapping("/image-history/{id}/image")
    @ApiOperation("查看历史解析原图")
    public ResponseEntity<byte[]> historyImage(@PathVariable("id") String id) {
        ImageParseHistoryEntity image = imageParseHistoryService.getImage(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.getContentType()))
                .cacheControl(CacheControl.noStore()).body(image.getContent());
    }

    @DeleteMapping("/image-history/{id}")
    @ApiOperation("删除图片解析历史及原图")
    public Result<Void> deleteImageHistory(@PathVariable("id") String id) {
        imageParseHistoryService.delete(id);
        return Result.ok();
    }
}
