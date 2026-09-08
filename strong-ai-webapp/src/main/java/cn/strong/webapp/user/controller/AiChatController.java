package cn.strong.webapp.user.controller;

import cn.strong.common.base.Result;
import cn.strong.webapp.user.model.request.AiChatRequest;
import cn.strong.webapp.user.model.response.AiChatResponse;
import cn.strong.webapp.user.service.AiChatService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI 模型统一接入接口。 */
@RestController
@RequestMapping("/api/ai")
@Api(tags = {"v1.0", "ai"}, description = "基于 ai4j 的多模型聊天接口")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    @ApiOperation("按 ai.platforms 中配置的 provider 同步调用聊天模型")
    public Result<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        return Result.ok(aiChatService.chat(request));
    }
}
