package cn.strong.webapp.user.documentgovernance.service;

import cn.strong.common.exception.ParamValiateException;
import cn.strong.webapp.user.documentgovernance.context.DocumentIdentity;
import cn.strong.webapp.user.documentgovernance.context.DocumentIdentityResolver;
import cn.strong.webapp.user.documentgovernance.entity.AiOptimizationStrategyVersionEntity;
import cn.strong.webapp.user.documentgovernance.entity.AiTaskEntity;
import cn.strong.webapp.user.documentgovernance.entity.DocumentEntity;
import cn.strong.webapp.user.documentgovernance.mapper.AiOptimizationStrategyVersionMapper;
import cn.strong.webapp.user.documentgovernance.mapper.AiTaskMapper;
import cn.strong.webapp.user.model.documentgovernance.AiOptimizationStrategyConfig;
import cn.strong.webapp.user.mdoptimizer.model.MarkdownOptimizeRequest;
import cn.strong.webapp.user.mdoptimizer.model.MarkdownOptimizeResponse;
import cn.strong.webapp.user.mdoptimizer.service.MarkdownOptimizerService;
import cn.strong.webapp.user.model.documentgovernance.AiOptimizeRequest;
import cn.strong.webapp.user.model.documentgovernance.AiTaskResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class DocumentAiTaskService {

    private static final long TASK_TTL_MILLIS = 24 * 60 * 60 * 1000L;
    private static final String PENDING = "PENDING";
    private static final String RUNNING = "RUNNING";
    private static final String SUCCEEDED = "SUCCEEDED";
    private static final String FAILED = "FAILED";
    private static final String CANCELLED = "CANCELLED";

    private final DocumentService documentService;
    private final AiTaskMapper taskMapper;
    private final MarkdownOptimizerService optimizer;
    private final TaskExecutor taskExecutor;
    private final DocumentIdentityResolver identityResolver;
    private final AiOptimizationStrategyService strategyService;
    private final AiOptimizationStrategyVersionMapper strategyVersionMapper;
    private final AiOptimizationStrategyCompiler strategyCompiler;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private DocumentOperationLogService operationLogService;

    @Autowired(required = false)
    private DocumentMetricsService metricsService;

    public DocumentAiTaskService(DocumentService documentService, AiTaskMapper taskMapper,
                                 MarkdownOptimizerService optimizer,
                                 @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
                                 DocumentIdentityResolver identityResolver,
                                 AiOptimizationStrategyService strategyService,
                                 AiOptimizationStrategyVersionMapper strategyVersionMapper,
                                 AiOptimizationStrategyCompiler strategyCompiler) {
        this.documentService = documentService;
        this.taskMapper = taskMapper;
        this.optimizer = optimizer;
        this.taskExecutor = taskExecutor;
        this.identityResolver = identityResolver;
        this.strategyService = strategyService;
        this.strategyVersionMapper = strategyVersionMapper;
        this.strategyCompiler = strategyCompiler;
    }

    public AiTaskResponse submit(Long documentId, AiOptimizeRequest request) {
        if (request == null) {
            throw new ParamValiateException("AI 优化请求不能为空");
        }
        String idempotencyKey = required(request.getIdempotencyKey(), "幂等键不能为空");
        DocumentEntity document = documentService.requireWritableDraft(documentId);
        DocumentIdentity identity = identityResolver.current();
        long now = System.currentTimeMillis();
        AiTaskEntity existing = taskMapper.findByIdempotency(document.getTeamId(), document.getId(), idempotencyKey);
        if (existing != null && existing.getExpiresAt() != null && existing.getExpiresAt() > now) {
            if (FAILED.equals(existing.getStatus())) {
                reset(existing, request, now);
                taskMapper.updateById(existing);
                operation(existing, "AI_TASK_RETRIED", "{}");
                schedule(existing.getId());
            }
            return toResponse(existing);
        }
        if (existing != null) {
            reset(existing, request, now);
            taskMapper.updateById(existing);
            operation(existing, "AI_TASK_RETRIED", "{}");
            schedule(existing.getId());
            return toResponse(existing);
        }
        AiTaskEntity task = pendingTask(document, idempotencyKey, request, now);
        taskMapper.insert(task);
        operation(task, "AI_TASK_SUBMITTED", "{}");
        schedule(task.getId());
        return toResponse(task);
    }

    public AiTaskResponse get(Long taskId) {
        DocumentIdentity identity = identityResolver.current();
        AiTaskEntity task = taskMapper.selectByTeamAndId(identity.getTeamId(), requiredTaskId(taskId));
        if (task == null) {
            throw new ParamValiateException("AI 任务不存在或无权访问");
        }
        return toResponse(task);
    }

    public SseEmitter events(Long taskId) {
        AiTaskResponse task = get(taskId);
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(taskId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(taskId, emitter));
        emitter.onTimeout(() -> removeEmitter(taskId, emitter));
        send(emitter, task);
        if (terminal(task.getStatus())) {
            emitter.complete();
        }
        return emitter;
    }

    public void process(Long taskId) {
        AiTaskEntity task = taskMapper.selectById(taskId);
        if (task == null || terminal(task.getStatus())) {
            return;
        }
        task.setStatus(RUNNING);
        task.setUpdatedAt(System.currentTimeMillis());
        taskMapper.updateById(task);
        publishStatus(task);
        long startedAt = System.currentTimeMillis();
        String documentType = "UNKNOWN";
        try {
            DocumentEntity document = documentService.findByTeam(task.getDocumentId(), task.getTeamId());
            if (document == null) {
                throw new IllegalStateException("document not found");
            }
            documentType = document.getDocumentType();
            MarkdownOptimizeResponse optimized = optimizer.optimize(toOptimizeRequest(document, task));
            documentService.applyAiDraft(document.getId(), task.getTeamId(), optimized.getOptimizedContent());
            task.setStatus(SUCCEEDED);
            task.setResultSummary(safe(optimized.getChangeSummary()));
            task.setProvider(safe(optimized.getProvider(), task.getProvider()));
            task.setModel(safe(optimized.getModel(), task.getModel()));
            task.setPromptTokens(optimized.getPromptTokens());
            task.setCompletionTokens(optimized.getCompletionTokens());
            task.setTotalTokens(optimized.getTotalTokens());
            task.setErrorCode("");
        } catch (Exception ignored) {
            task.setStatus(FAILED);
            task.setErrorCode("MODEL_UNAVAILABLE");
        }
        task.setUpdatedAt(System.currentTimeMillis());
        taskMapper.updateById(task);
        if (FAILED.equals(task.getStatus())) {
            operation(task, "AI_TASK_FAILED", "{}");
        }
        if (metricsService != null) {
            metricsService.recordAiTask(documentType, task.getProvider(), task.getModel(), task.getPromptTokens(),
                    task.getCompletionTokens(), task.getTotalTokens(), System.currentTimeMillis() - startedAt, task.getStatus());
        }
        publishStatus(task);
    }

    public int cleanupExpiredTasks() {
        return taskMapper.deleteExpiredTerminalTasks(System.currentTimeMillis());
    }

    private AiTaskEntity pendingTask(DocumentEntity document, String idempotencyKey,
                                     AiOptimizeRequest request, long now) {
        AiTaskEntity task = new AiTaskEntity();
        task.setTeamId(document.getTeamId());
        task.setDocumentId(document.getId());
        task.setIdempotencyKey(idempotencyKey);
        task.setCreatedBy(identityResolver.current().getUserId());
        applyStrategy(task, document.getTeamId(), request);
        reset(task, request, now);
        task.setCreatedAt(now);
        return task;
    }

    /** 解析并固化策略版本；找到已发布版本时同时固化策略的模型参数。 */
    private void applyStrategy(AiTaskEntity task, String teamId, AiOptimizeRequest request) {
        AiOptimizationStrategyVersionEntity version = strategyService.resolveExecutable(teamId, request.getStrategyId());
        if (version == null) {
            return;
        }
        task.setStrategyId(version.getStrategyId());
        task.setStrategyVersionId(version.getId());
        task.setStrategyConfigHash(version.getConfigHash());
        AiOptimizationStrategyConfig config = parseConfig(version.getConfigJson());
        if (config != null && config.getModel() != null) {
            task.setProvider(safe(config.getModel().getProvider(), "openai"));
            task.setModel(safe(config.getModel().getName(), "gpt-5.6-sol"));
        }
    }

    private AiOptimizationStrategyConfig parseConfig(String configJson) {
        if (configJson == null || configJson.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(configJson, AiOptimizationStrategyConfig.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void reset(AiTaskEntity task, AiOptimizeRequest request, long now) {
        task.setStatus(PENDING);
        if (task.getStrategyVersionId() == null) {
            // 非策略任务：模型参数由请求决定。策略任务在创建时已固化 provider/model，重试沿用不重解析。
            task.setProvider(required(request.getProvider(), "AI provider 不能为空"));
            task.setModel(required(request.getModel(), "AI model 不能为空"));
        }
        task.setResultSummary("");
        task.setErrorCode("");
        task.setPromptTokens(null);
        task.setCompletionTokens(null);
        task.setTotalTokens(null);
        task.setUpdatedAt(now);
        task.setExpiresAt(now + TASK_TTL_MILLIS);
    }

    private MarkdownOptimizeRequest toOptimizeRequest(DocumentEntity document, AiTaskEntity task) {
        MarkdownOptimizeRequest request = new MarkdownOptimizeRequest();
        request.setContent(document.getDraftContent());
        request.setFileName(document.getTitle() + ".md");
        request.setDocumentType(document.getDocumentType());
        request.setProvider(task.getProvider());
        request.setModel(task.getModel());
        if (task.getStrategyVersionId() != null) {
            applyStrategyContext(request, document, task);
        }
        return request;
    }

    /** 读取任务固化的策略版本，编译系统提示词并完整映射模型参数。 */
    private void applyStrategyContext(MarkdownOptimizeRequest request, DocumentEntity document, AiTaskEntity task) {
        AiOptimizationStrategyVersionEntity version =
                strategyVersionMapper.selectByTeamAndId(task.getTeamId(), task.getStrategyVersionId());
        if (version == null) {
            throw new IllegalStateException("strategy version not found");
        }
        AiOptimizationStrategyConfig config = parseConfig(version.getConfigJson());
        if (config == null) {
            throw new IllegalStateException("strategy config invalid");
        }
        request.setSystemPromptOverride(
                strategyCompiler.compileSystemPrompt(config, document.getDocumentType(), null));
        if (config.getModel() != null) {
            request.setProvider(safe(config.getModel().getProvider(), request.getProvider()));
            request.setModel(safe(config.getModel().getName(), request.getModel()));
            request.setDeepThinking(Boolean.TRUE.equals(config.getModel().getDeepThinking()));
            if (config.getModel().getMaxTokens() != null && config.getModel().getMaxTokens() > 0) {
                request.setMaxTokens(config.getModel().getMaxTokens());
            }
        }
        if (config.getConstraints() != null && config.getConstraints().getMode() != null) {
            request.setMode(config.getConstraints().getMode());
        }
    }

    private void schedule(Long taskId) {
        taskExecutor.execute(() -> process(taskId));
    }

    private void publishStatus(AiTaskEntity task) {
        List<SseEmitter> taskEmitters = emitters.get(task.getId());
        if (taskEmitters == null) {
            return;
        }
        AiTaskResponse response = toResponse(task);
        for (SseEmitter emitter : taskEmitters) {
            send(emitter, response);
            if (terminal(task.getStatus())) {
                emitter.complete();
            }
        }
        if (terminal(task.getStatus())) {
            emitters.remove(task.getId());
        }
    }

    private void send(SseEmitter emitter, AiTaskResponse response) {
        try {
            emitter.send(SseEmitter.event().name("status").data(response));
        } catch (IOException ignored) {
            emitter.complete();
        }
    }

    private void removeEmitter(Long taskId, SseEmitter emitter) {
        List<SseEmitter> taskEmitters = emitters.get(taskId);
        if (taskEmitters != null) {
            taskEmitters.remove(emitter);
        }
    }

    private boolean terminal(String status) {
        return SUCCEEDED.equals(status) || FAILED.equals(status) || CANCELLED.equals(status);
    }

    private Long requiredTaskId(Long taskId) {
        if (taskId == null || taskId <= 0L) {
            throw new ParamValiateException("AI 任务 ID 无效");
        }
        return taskId;
    }

    private String required(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ParamValiateException(message);
        }
        return value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String value, String fallback) {
        String normalized = safe(value);
        return normalized.isEmpty() ? fallback : normalized;
    }

    private AiTaskResponse toResponse(AiTaskEntity task) {
        AiTaskResponse response = new AiTaskResponse();
        response.setId(task.getId());
        response.setDocumentId(task.getDocumentId());
        response.setStrategyId(task.getStrategyId());
        response.setStrategyVersionId(task.getStrategyVersionId());
        response.setStrategyConfigHash(task.getStrategyConfigHash());
        if (task.getStrategyVersionId() != null) {
            AiOptimizationStrategyVersionEntity version =
                    strategyVersionMapper.selectByTeamAndId(task.getTeamId(), task.getStrategyVersionId());
            if (version != null) {
                response.setStrategyVersionNo(version.getVersionNo());
            }
        }
        response.setStatus(task.getStatus());
        response.setProvider(task.getProvider());
        response.setModel(task.getModel());
        response.setResultSummary(task.getResultSummary());
        response.setErrorCode(task.getErrorCode());
        response.setPromptTokens(task.getPromptTokens());
        response.setCompletionTokens(task.getCompletionTokens());
        response.setTotalTokens(task.getTotalTokens());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        response.setExpiresAt(task.getExpiresAt());
        return response;
    }

    private void operation(AiTaskEntity task, String action, String metadata) {
        if (operationLogService != null) {
            operationLogService.record(task.getTeamId(), task.getDocumentId(), task.getCreatedBy(), action, metadata);
        }
    }
}
