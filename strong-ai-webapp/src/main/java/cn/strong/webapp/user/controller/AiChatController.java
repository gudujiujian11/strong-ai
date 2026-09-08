package cn.strong.webapp.user.controller;

import cn.strong.common.base.Result;
import cn.strong.webapp.user.model.request.AiChatRequest;
import cn.strong.webapp.user.model.response.AiChatResponse;
import cn.strong.webapp.user.service.AiChatService;
import io.github.lnyocly.ai4j.listener.SseListener;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** AI 模型统一接入接口。 */
@RestController
@RequestMapping("/api/ai")
@Api(tags = {"v1.0", "ai"}, description = "基于 ai4j 的多模型聊天接口")
public class AiChatController {

    private final AiChatService aiChatService;
    private final TaskExecutor taskExecutor;

    public AiChatController(AiChatService aiChatService,
                            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.aiChatService = aiChatService;
        this.taskExecutor = taskExecutor;
    }

    @PostMapping("/chat")
    @ApiOperation("按 ai.platforms 中配置的 provider 同步调用聊天模型")
    public Result<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        return Result.ok(aiChatService.chat(request));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation("按 ai.platforms 中配置的 provider 流式调用聊天模型")
    public SseEmitter chatStream(@RequestBody AiChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        AtomicBoolean completed = new AtomicBoolean(false);
        SseListener listener = new SseListener() {
            @Override
            protected void send() {
                String data = getCurrData();
                try {
                    if ("[DONE]".equalsIgnoreCase(data)) {
                        if (completed.compareAndSet(false, true)) {
                            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                            emitter.complete();
                        }
                        return;
                    }
                    if (data != null && !data.isEmpty() && !completed.get()) {
                        emitter.send(SseEmitter.event().name("message")
                                .data(data, MediaType.APPLICATION_JSON));
                    }
                } catch (IOException ex) {
                    completed.set(true);
                    cancelStream();
                    emitter.complete();
                }
            }
        };

        emitter.onCompletion(() -> {
            completed.set(true);
            listener.cancelStream();
        });
        emitter.onTimeout(() -> {
            completed.set(true);
            listener.cancelStream();
        });

        taskExecutor.execute(() -> {
            try {
                aiChatService.chatStream(request, listener);
                if (completed.compareAndSet(false, true)) {
                    emitter.complete();
                }
            } catch (Exception ex) {
                if (completed.compareAndSet(false, true)) {
                    try {
                        emitter.send(SseEmitter.event().name("error").data(message(ex)));
                    } catch (IOException ignored) {
                        // The client has already disconnected.
                    }
                    emitter.complete();
                }
            }
        });
        return emitter;
    }

    private String message(Exception ex) {
        Throwable cause = ex.getCause() == null ? ex : ex.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
