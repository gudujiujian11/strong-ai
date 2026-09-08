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
@Api(tags = {"v1.0", "ai"}, description = "豆包、千问、DeepSeek 模型接口")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    @ApiOperation("同步调用豆包、千问或 DeepSeek 聊天模型")
    public Result<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        return Result.ok(aiChatService.chat(request));
    }
}
