package cn.strong.webapp.user.documentgovernance.controller;

import cn.strong.common.base.Result;
import cn.strong.webapp.user.documentgovernance.service.AiOptimizationStrategyService;
import cn.strong.webapp.user.model.documentgovernance.AiOptimizationStrategyRequest;
import cn.strong.webapp.user.model.documentgovernance.AiOptimizationStrategyResponse;
import cn.strong.webapp.user.model.documentgovernance.AiOptimizationStrategyVersionResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/document-governance/ai-strategies")
@Api(tags = {"v1.0", "document-governance"}, description = "AI 优化策略管理接口")
public class AiOptimizationStrategyController {

    private final AiOptimizationStrategyService strategyService;

    public AiOptimizationStrategyController(AiOptimizationStrategyService strategyService) {
        this.strategyService = strategyService;
    }

    @GetMapping
    @ApiOperation("查询当前团队 AI 优化策略")
    public Result<List<AiOptimizationStrategyResponse>> list() {
        return Result.ok(strategyService.list());
    }

    @PostMapping
    @ApiOperation("创建 AI 优化策略及初始草稿版本")
    public Result<AiOptimizationStrategyResponse> create(@RequestBody AiOptimizationStrategyRequest request) {
        return Result.ok(strategyService.create(request));
    }

    @PutMapping("/{id}")
    @ApiOperation("修改策略名称和描述")
    public Result<AiOptimizationStrategyResponse> update(@PathVariable("id") Long id,
                                                         @RequestBody AiOptimizationStrategyRequest request) {
        return Result.ok(strategyService.update(id, request));
    }

    @PostMapping("/{id}/versions")
    @ApiOperation("创建新草稿版本")
    public Result<AiOptimizationStrategyVersionResponse> createVersion(@PathVariable("id") Long id,
                                                                       @RequestBody AiOptimizationStrategyRequest request) {
        return Result.ok(strategyService.createVersion(id, request));
    }

    @GetMapping("/{id}/versions")
    @ApiOperation("查询策略版本历史")
    public Result<List<AiOptimizationStrategyVersionResponse>> listVersions(@PathVariable("id") Long id) {
        return Result.ok(strategyService.listVersions(id));
    }

    @PostMapping("/{id}/versions/{versionNo}/publish")
    @ApiOperation("发布指定版本")
    public Result<AiOptimizationStrategyVersionResponse> publish(@PathVariable("id") Long id,
                                                                 @PathVariable("versionNo") Long versionNo) {
        return Result.ok(strategyService.publish(id, versionNo));
    }

    @PostMapping("/{id}/default")
    @ApiOperation("设置为团队默认策略")
    public Result<AiOptimizationStrategyResponse> setDefault(@PathVariable("id") Long id) {
        return Result.ok(strategyService.setDefault(id));
    }

    @PostMapping("/{id}/disable")
    @ApiOperation("停用策略")
    public Result<AiOptimizationStrategyResponse> disable(@PathVariable("id") Long id) {
        return Result.ok(strategyService.disable(id));
    }
}
