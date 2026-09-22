package cn.strong.webapp.user.documentgovernance.service;

import cn.strong.common.exception.ParamValiateException;
import cn.strong.webapp.user.documentgovernance.context.DocumentIdentity;
import cn.strong.webapp.user.documentgovernance.context.DocumentIdentityResolver;
import cn.strong.webapp.user.documentgovernance.entity.AiOptimizationStrategyEntity;
import cn.strong.webapp.user.documentgovernance.entity.AiOptimizationStrategyVersionEntity;
import cn.strong.webapp.user.documentgovernance.mapper.AiOptimizationStrategyMapper;
import cn.strong.webapp.user.documentgovernance.mapper.AiOptimizationStrategyVersionMapper;
import cn.strong.webapp.user.model.documentgovernance.AiOptimizationStrategyCompiledContext;
import cn.strong.webapp.user.model.documentgovernance.AiOptimizationStrategyConfig;
import cn.strong.webapp.user.model.documentgovernance.AiOptimizationStrategyRequest;
import cn.strong.webapp.user.model.documentgovernance.AiOptimizationStrategyResponse;
import cn.strong.webapp.user.model.documentgovernance.AiOptimizationStrategyVersionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 优化策略集读写与版本发布。
 *
 * <p>读侧（成员）：只读团队启用策略；写侧（管理员）：创建、改草稿、发布、设默认、停用。
 * 策略配置一律经 {@link AiOptimizationStrategyCompiler} 严格校验并规范化，已发布版本不可修改。</p>
 */
@Service
public class AiOptimizationStrategyService {

    private static final String ENABLED = "ENABLED";
    private static final String DISABLED = "DISABLED";
    private static final String DRAFT = "DRAFT";
    private static final String PUBLISHED = "PUBLISHED";

    private final AiOptimizationStrategyMapper strategyMapper;
    private final AiOptimizationStrategyVersionMapper versionMapper;
    private final AiOptimizationStrategyCompiler compiler;
    private final DocumentIdentityResolver identityResolver;
    private final DocumentOperationLogService operationLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiOptimizationStrategyService(AiOptimizationStrategyMapper strategyMapper,
                                         AiOptimizationStrategyVersionMapper versionMapper,
                                         AiOptimizationStrategyCompiler compiler,
                                         DocumentIdentityResolver identityResolver,
                                         DocumentOperationLogService operationLogService) {
        this.strategyMapper = strategyMapper;
        this.versionMapper = versionMapper;
        this.compiler = compiler;
        this.identityResolver = identityResolver;
        this.operationLogService = operationLogService;
    }

    /** 成员可读：当前团队全部策略。 */
    public List<AiOptimizationStrategyResponse> list() {
        String teamId = identityResolver.current().getTeamId();
        List<AiOptimizationStrategyEntity> strategies = strategyMapper.selectByTeamId(teamId);
        if (strategies == null || strategies.isEmpty()) {
            return Collections.emptyList();
        }
        List<AiOptimizationStrategyResponse> responses = new ArrayList<>(strategies.size());
        for (AiOptimizationStrategyEntity strategy : strategies) {
            responses.add(toResponse(teamId, strategy));
        }
        return responses;
    }

    @Transactional(rollbackFor = Exception.class)
    public AiOptimizationStrategyResponse create(AiOptimizationStrategyRequest request) {
        DocumentIdentity identity = requireAdmin();
        String name = required(request == null ? null : request.getName(), "策略名称不能为空");
        AiOptimizationStrategyCompiledContext compiled = compile(request);
        if (strategyMapper.selectByTeamAndName(identity.getTeamId(), name) != null) {
            throw new ParamValiateException("同名策略已存在");
        }
        long now = System.currentTimeMillis();
        AiOptimizationStrategyEntity strategy = new AiOptimizationStrategyEntity();
        strategy.setTeamId(identity.getTeamId());
        strategy.setName(name);
        strategy.setDescription(safe(request.getDescription()));
        strategy.setStatus(ENABLED);
        strategy.setDefaultStrategy(false);
        strategy.setCreatedBy(identity.getUserId());
        strategy.setCreatedAt(now);
        strategy.setUpdatedAt(now);
        strategyMapper.insert(strategy);
        insertVersion(identity, strategy.getId(), 1L, compiled, now);
        record(identity, "AI_STRATEGY_CREATED", strategy.getId(), null);
        return toResponse(identity.getTeamId(), strategy);
    }

    @Transactional(rollbackFor = Exception.class)
    public AiOptimizationStrategyResponse update(Long id, AiOptimizationStrategyRequest request) {
        DocumentIdentity identity = requireAdmin();
        AiOptimizationStrategyEntity strategy = requireStrategy(identity.getTeamId(), id);
        String name = required(request == null ? null : request.getName(), "策略名称不能为空");
        AiOptimizationStrategyEntity duplicate = strategyMapper.selectByTeamAndName(identity.getTeamId(), name);
        if (duplicate != null && !duplicate.getId().equals(strategy.getId())) {
            throw new ParamValiateException("同名策略已存在");
        }
        strategy.setName(name);
        strategy.setDescription(safe(request.getDescription()));
        strategy.setUpdatedAt(System.currentTimeMillis());
        strategyMapper.updateById(strategy);
        record(identity, "AI_STRATEGY_UPDATED", strategy.getId(), null);
        return toResponse(identity.getTeamId(), strategy);
    }

    @Transactional(rollbackFor = Exception.class)
    public AiOptimizationStrategyVersionResponse createVersion(Long id, AiOptimizationStrategyRequest request) {
        DocumentIdentity identity = requireAdmin();
        AiOptimizationStrategyEntity strategy = requireStrategy(identity.getTeamId(), id);
        AiOptimizationStrategyCompiledContext compiled = compile(request);
        long now = System.currentTimeMillis();
        long nextVersionNo = versionMapper.selectMaxVersionNo(identity.getTeamId(), strategy.getId()) + 1L;
        AiOptimizationStrategyVersionEntity version = insertVersion(identity, strategy.getId(), nextVersionNo, compiled, now);
        strategy.setUpdatedAt(now);
        strategyMapper.updateById(strategy);
        record(identity, "AI_STRATEGY_VERSION_CREATED", strategy.getId(), nextVersionNo);
        return toVersionResponse(version);
    }

    public List<AiOptimizationStrategyVersionResponse> listVersions(Long id) {
        DocumentIdentity identity = requireAdmin();
        requireStrategy(identity.getTeamId(), id);
        List<AiOptimizationStrategyVersionEntity> versions = versionMapper.selectByStrategyId(identity.getTeamId(), id);
        if (versions == null || versions.isEmpty()) {
            return Collections.emptyList();
        }
        List<AiOptimizationStrategyVersionResponse> responses = new ArrayList<>(versions.size());
        for (AiOptimizationStrategyVersionEntity version : versions) {
            responses.add(toVersionResponse(version));
        }
        return responses;
    }

    @Transactional(rollbackFor = Exception.class)
    public AiOptimizationStrategyVersionResponse publish(Long id, Long versionNo) {
        DocumentIdentity identity = requireAdmin();
        AiOptimizationStrategyEntity strategy = requireStrategy(identity.getTeamId(), id);
        AiOptimizationStrategyVersionEntity version =
                versionMapper.selectByStrategyAndVersionNo(identity.getTeamId(), id, versionNo);
        if (version == null) {
            throw new ParamValiateException("策略版本不存在");
        }
        long now = System.currentTimeMillis();
        if (!PUBLISHED.equals(version.getStatus())) {
            version.setStatus(PUBLISHED);
            version.setPublishedAt(now);
            versionMapper.updateById(version);
        }
        strategy.setCurrentVersionId(version.getId());
        strategy.setUpdatedAt(now);
        strategyMapper.updateById(strategy);
        record(identity, "AI_STRATEGY_VERSION_PUBLISHED", strategy.getId(), versionNo);
        return toVersionResponse(version);
    }

    @Transactional(rollbackFor = Exception.class)
    public AiOptimizationStrategyResponse setDefault(Long id) {
        DocumentIdentity identity = requireAdmin();
        AiOptimizationStrategyEntity strategy = requireStrategy(identity.getTeamId(), id);
        if (!ENABLED.equals(strategy.getStatus()) || strategy.getCurrentVersionId() == null) {
            throw new ParamValiateException("只能将已发布且启用的策略设为默认");
        }
        long now = System.currentTimeMillis();
        strategyMapper.clearDefault(identity.getTeamId(), now);
        strategy.setDefaultStrategy(true);
        strategy.setUpdatedAt(now);
        strategyMapper.updateById(strategy);
        record(identity, "AI_STRATEGY_DEFAULT_CHANGED", strategy.getId(), null);
        return toResponse(identity.getTeamId(), strategy);
    }

    @Transactional(rollbackFor = Exception.class)
    public AiOptimizationStrategyResponse disable(Long id) {
        DocumentIdentity identity = requireAdmin();
        AiOptimizationStrategyEntity strategy = requireStrategy(identity.getTeamId(), id);
        strategy.setStatus(DISABLED);
        strategy.setDefaultStrategy(false);
        strategy.setUpdatedAt(System.currentTimeMillis());
        strategyMapper.updateById(strategy);
        record(identity, "AI_STRATEGY_DISABLED", strategy.getId(), null);
        return toResponse(identity.getTeamId(), strategy);
    }

    /**
     * 解析任务实际执行的策略版本；供任务链路固化使用。
     *
     * @return 已发布版本实体；无可用策略（无默认且未指定）时返回 null，由调用方回退系统兼容策略
     */
    public AiOptimizationStrategyVersionEntity resolveExecutable(String teamId, Long strategyId) {
        if (strategyId != null) {
            AiOptimizationStrategyEntity strategy = strategyMapper.selectByTeamAndId(teamId, strategyId);
            if (strategy == null) {
                throw new ParamValiateException("策略不存在或无权访问");
            }
            if (!ENABLED.equals(strategy.getStatus())) {
                throw new ParamValiateException("策略已停用，无法执行优化");
            }
            return requirePublishedVersion(teamId, strategy);
        }
        for (AiOptimizationStrategyEntity strategy : safeList(strategyMapper.selectByTeamId(teamId))) {
            if (Boolean.TRUE.equals(strategy.getDefaultStrategy()) && ENABLED.equals(strategy.getStatus())) {
                return requirePublishedVersion(teamId, strategy);
            }
        }
        return null;
    }

    private AiOptimizationStrategyVersionEntity requirePublishedVersion(String teamId,
                                                                        AiOptimizationStrategyEntity strategy) {
        Long versionId = strategy.getCurrentVersionId();
        AiOptimizationStrategyVersionEntity version =
                versionId == null ? null : versionMapper.selectByTeamAndId(teamId, versionId);
        if (version == null || !PUBLISHED.equals(version.getStatus())) {
            throw new ParamValiateException("策略没有已发布版本，无法执行优化");
        }
        return version;
    }

    private AiOptimizationStrategyVersionEntity insertVersion(DocumentIdentity identity, Long strategyId,
                                                              Long versionNo,
                                                              AiOptimizationStrategyCompiledContext compiled, long now) {
        AiOptimizationStrategyVersionEntity version = new AiOptimizationStrategyVersionEntity();
        version.setTeamId(identity.getTeamId());
        version.setStrategyId(strategyId);
        version.setVersionNo(versionNo);
        version.setStatus(DRAFT);
        version.setConfigJson(compiled.getNormalizedJson());
        version.setConfigHash(compiled.getConfigHash());
        version.setCreatedBy(identity.getUserId());
        version.setCreatedAt(now);
        versionMapper.insert(version);
        return version;
    }

    private AiOptimizationStrategyCompiledContext compile(AiOptimizationStrategyRequest request) {
        AiOptimizationStrategyConfig config = request == null ? null : request.getConfig();
        String configJson;
        try {
            configJson = config == null ? "{}" : objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new ParamValiateException("AI 优化策略配置无效：请检查字段、类型和取值");
        }
        return compiler.normalizeAndValidate(configJson);
    }

    private AiOptimizationStrategyResponse toResponse(String teamId, AiOptimizationStrategyEntity strategy) {
        AiOptimizationStrategyResponse response = new AiOptimizationStrategyResponse();
        response.setId(strategy.getId());
        response.setName(strategy.getName());
        response.setDescription(strategy.getDescription());
        response.setStatus(strategy.getStatus());
        response.setCurrentVersionId(strategy.getCurrentVersionId());
        response.setDefaultStrategy(strategy.getDefaultStrategy());
        response.setCreatedBy(strategy.getCreatedBy());
        response.setCreatedAt(strategy.getCreatedAt());
        response.setUpdatedAt(strategy.getUpdatedAt());
        if (strategy.getCurrentVersionId() != null) {
            AiOptimizationStrategyVersionEntity current =
                    versionMapper.selectByTeamAndId(teamId, strategy.getCurrentVersionId());
            if (current != null) {
                response.setCurrentVersionNo(current.getVersionNo());
            }
        }
        return response;
    }

    private AiOptimizationStrategyVersionResponse toVersionResponse(AiOptimizationStrategyVersionEntity version) {
        AiOptimizationStrategyVersionResponse response = new AiOptimizationStrategyVersionResponse();
        response.setId(version.getId());
        response.setStrategyId(version.getStrategyId());
        response.setVersionNo(version.getVersionNo());
        response.setStatus(version.getStatus());
        response.setConfigHash(version.getConfigHash());
        response.setCreatedBy(version.getCreatedBy());
        response.setCreatedAt(version.getCreatedAt());
        response.setPublishedAt(version.getPublishedAt());
        response.setConfig(parseConfig(version.getConfigJson()));
        return response;
    }

    private AiOptimizationStrategyConfig parseConfig(String configJson) {
        if (configJson == null || configJson.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(configJson, AiOptimizationStrategyConfig.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private AiOptimizationStrategyEntity requireStrategy(String teamId, Long id) {
        if (id == null || id <= 0L) {
            throw new ParamValiateException("策略 ID 无效");
        }
        AiOptimizationStrategyEntity strategy = strategyMapper.selectByTeamAndId(teamId, id);
        if (strategy == null) {
            throw new ParamValiateException("策略不存在或无权访问");
        }
        return strategy;
    }

    private DocumentIdentity requireAdmin() {
        DocumentIdentity identity = identityResolver.current();
        if (!identity.isAdmin()) {
            throw new ParamValiateException("仅管理员可以维护 AI 优化策略");
        }
        return identity;
    }

    private void record(DocumentIdentity identity, String action, Long strategyId, Long versionNo) {
        StringBuilder metadata = new StringBuilder("{\"strategyId\":").append(strategyId);
        if (versionNo != null) {
            metadata.append(",\"versionNo\":").append(versionNo);
        }
        metadata.append("}");
        operationLogService.record(identity.getTeamId(), null, identity.getUserId(), action, metadata.toString());
    }

    private List<AiOptimizationStrategyEntity> safeList(List<AiOptimizationStrategyEntity> list) {
        return list == null ? Collections.emptyList() : list;
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
}
