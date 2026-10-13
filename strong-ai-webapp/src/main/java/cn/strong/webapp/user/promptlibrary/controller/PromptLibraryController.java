package cn.strong.webapp.user.promptlibrary.controller;

import cn.strong.common.base.Result;
import cn.strong.webapp.user.promptlibrary.model.PromptDetail;
import cn.strong.webapp.user.promptlibrary.model.PromptFavoriteRequest;
import cn.strong.webapp.user.promptlibrary.model.PromptRatingRequest;
import cn.strong.webapp.user.promptlibrary.model.PromptRequest;
import cn.strong.webapp.user.promptlibrary.model.PromptSummary;
import cn.strong.webapp.user.promptlibrary.model.PromptVersion;
import cn.strong.webapp.user.promptlibrary.service.PromptLibraryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prompts")
@Api(tags = {"v1.0", "prompt-library"}, description = "提示词库接口")
public class PromptLibraryController {

    private final PromptLibraryService promptLibraryService;

    public PromptLibraryController(PromptLibraryService promptLibraryService) {
        this.promptLibraryService = promptLibraryService;
    }

    @GetMapping
    @ApiOperation("查询提示词列表")
    public Result<List<PromptSummary>> list(@RequestParam(value = "category", required = false) String category,
                                            @RequestParam(value = "tag", required = false) String tag,
                                            @RequestParam(value = "q", required = false) String q) {
        return Result.ok(promptLibraryService.list(category, tag, q));
    }

    @GetMapping("/{id}")
    @ApiOperation("查询提示词详情")
    public Result<PromptDetail> get(@PathVariable("id") Long id) {
        return Result.ok(promptLibraryService.get(id));
    }

    @PostMapping
    @ApiOperation("创建提示词")
    public Result<PromptDetail> create(@RequestBody PromptRequest request) {
        return Result.ok(promptLibraryService.create(request));
    }

    @PutMapping("/{id}")
    @ApiOperation("编辑提示词")
    public Result<PromptDetail> update(@PathVariable("id") Long id, @RequestBody PromptRequest request) {
        return Result.ok(promptLibraryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除提示词")
    public Result<Void> delete(@PathVariable("id") Long id) {
        promptLibraryService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/versions")
    @ApiOperation("发布提示词新版本")
    public Result<PromptDetail> publish(@PathVariable("id") Long id) {
        return Result.ok(promptLibraryService.publish(id));
    }

    @GetMapping("/{id}/versions")
    @ApiOperation("查询提示词版本")
    public Result<List<PromptVersion>> versions(@PathVariable("id") Long id) {
        return Result.ok(promptLibraryService.versions(id));
    }

    @GetMapping("/{id}/versions/{versionNo}")
    @ApiOperation("查询提示词历史版本")
    public Result<PromptVersion> version(@PathVariable("id") Long id,
                                         @PathVariable("versionNo") Long versionNo) {
        return Result.ok(promptLibraryService.version(id, versionNo));
    }

    @PostMapping("/{id}/versions/{versionNo}/restore")
    @ApiOperation("恢复提示词历史版本")
    public Result<PromptDetail> restore(@PathVariable("id") Long id,
                                        @PathVariable("versionNo") Long versionNo) {
        return Result.ok(promptLibraryService.restore(id, versionNo));
    }

    @PutMapping("/{id}/rating")
    @ApiOperation("更新提示词评分")
    public Result<PromptDetail> rate(@PathVariable("id") Long id, @RequestBody PromptRatingRequest request) {
        return Result.ok(promptLibraryService.rate(id, request));
    }

    @PostMapping("/{id}/favorite")
    @ApiOperation("更新提示词收藏")
    public Result<PromptDetail> favorite(@PathVariable("id") Long id,
                                         @RequestBody PromptFavoriteRequest request) {
        return Result.ok(promptLibraryService.favorite(id, request));
    }
}
