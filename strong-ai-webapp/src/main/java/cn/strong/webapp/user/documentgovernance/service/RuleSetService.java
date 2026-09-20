package cn.strong.webapp.user.documentgovernance.service;

import cn.strong.common.exception.ParamValiateException;
import cn.strong.webapp.user.documentgovernance.context.DocumentIdentity;
import cn.strong.webapp.user.documentgovernance.context.DocumentIdentityResolver;
import cn.strong.webapp.user.documentgovernance.entity.RuleSetEntity;
import cn.strong.webapp.user.documentgovernance.mapper.RuleSetMapper;
import cn.strong.webapp.user.model.documentgovernance.QualitySeverity;
import cn.strong.webapp.user.model.documentgovernance.RuleConfig;
import cn.strong.webapp.user.model.documentgovernance.RuleSetRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 团队规则集读写与生效配置解析。
 *
 * <p>读侧宽松：配置缺失或损坏一律回退默认，绝不阻断质量检查。
 * 写侧严格：管理员提交的 configJson 必须是合法 JSON 对象。</p>
 */
@Service
public class RuleSetService {

    private static final Logger LOG = LoggerFactory.getLogger(RuleSetService.class);
    private static final long CACHE_TTL_MILLIS = 5 * 60 * 1000L;

    private final RuleSetMapper ruleSetMapper;
    private final DocumentIdentityResolver identityResolver;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, CachedConfig> configCache = new ConcurrentHashMap<>();

    public RuleSetService(RuleSetMapper ruleSetMapper, DocumentIdentityResolver identityResolver) {
        this.ruleSetMapper = ruleSetMapper;
        this.identityResolver = identityResolver;
    }

    /** 供质量检查使用：合并团队所有启用规则集，缺失回退默认。 */
    public RuleConfig effectiveConfig(String teamId) {
        long now = System.currentTimeMillis();
        CachedConfig cached = configCache.get(teamId);
        if (cached != null && cached.expiresAt > now) {
            return cached.config;
        }
        RuleConfig config = resolve(teamId);
        configCache.put(teamId, new CachedConfig(config, now + CACHE_TTL_MILLIS));
        return config;
    }

    public List<RuleSetEntity> list(String teamId) {
        requireCurrentTeam(teamId);
        List<RuleSetEntity> ruleSets = ruleSetMapper.selectByTeamId(teamId);
        return Collections.unmodifiableList(ruleSets == null ? new ArrayList<>() : new ArrayList<>(ruleSets));
    }

    @Transactional(rollbackFor = Exception.class)
    public RuleSetEntity upsert(RuleSetRequest request) {
        DocumentIdentity identity = requireAdmin();
        if (request == null) {
            throw new ParamValiateException("规则集请求不能为空");
        }
        String name = required(request.getName(), "规则集名称不能为空");
        String configJson = validateConfigJson(request.getConfigJson());
        long now = System.currentTimeMillis();
        RuleSetEntity existing = ruleSetMapper.selectByTeamAndName(identity.getTeamId(), name);
        RuleSetEntity entity = existing == null ? new RuleSetEntity() : existing;
        entity.setTeamId(identity.getTeamId());
        entity.setName(name);
        entity.setConfigJson(configJson);
        entity.setEnabled(request.getEnabled() == null ? Boolean.TRUE : request.getEnabled());
        entity.setUpdatedAt(now);
        if (existing == null) {
            entity.setCreatedBy(identity.getUserId());
            entity.setCreatedAt(now);
            ruleSetMapper.insert(entity);
        } else {
            ruleSetMapper.updateById(entity);
        }
        configCache.remove(identity.getTeamId());
        return entity;
    }

    private RuleConfig resolve(String teamId) {
        List<RuleSetEntity> enabledSets = ruleSetMapper.selectEnabledByTeamId(teamId);
        if (enabledSets == null || enabledSets.isEmpty()) {
            return RuleConfig.defaults();
        }
        Map<String, Boolean> enabled = new HashMap<>();
        Map<String, QualitySeverity> severities = new HashMap<>();
        Map<QualitySeverity, Integer> weights = new HashMap<>();
        // selectEnabledByTeamId 按 updated_at 升序，越新的规则集越后应用、优先级越高。
        for (RuleSetEntity ruleSet : enabledSets) {
            applyConfig(ruleSet.getConfigJson(), enabled, severities, weights);
        }
        return new RuleConfig(enabled, severities, weights);
    }

    private void applyConfig(String configJson, Map<String, Boolean> enabled,
                             Map<String, QualitySeverity> severities, Map<QualitySeverity, Integer> weights) {
        if (configJson == null || configJson.trim().isEmpty()) {
            return;
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(configJson);
        } catch (Exception e) {
            LOG.warn("规则集配置解析失败，忽略该配置: {}", e.getMessage());
            return;
        }
        if (root == null || !root.isObject()) {
            return;
        }
        JsonNode rules = root.path("rules");
        if (rules.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = rules.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String ruleCode = field.getKey();
                JsonNode rule = field.getValue();
                if (rule.has("enabled") && rule.get("enabled").isBoolean()) {
                    enabled.put(ruleCode, rule.get("enabled").asBoolean());
                }
                QualitySeverity severity = parseSeverity(rule.path("severity").asText(null));
                if (severity != null) {
                    severities.put(ruleCode, severity);
                }
            }
        }
        JsonNode scoreWeights = root.path("scoreWeights");
        if (scoreWeights.isObject()) {
            for (QualitySeverity level : QualitySeverity.values()) {
                JsonNode weight = scoreWeights.path(level.name());
                if (weight.isInt() || weight.isLong()) {
                    weights.put(level, weight.asInt());
                }
            }
        }
    }

    private QualitySeverity parseSeverity(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return QualitySeverity.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String validateConfigJson(String configJson) {
        if (configJson == null || configJson.trim().isEmpty()) {
            throw new ParamValiateException("规则集配置不能为空");
        }
        try {
            JsonNode root = objectMapper.readTree(configJson);
            if (root == null || !root.isObject()) {
                throw new ParamValiateException("规则集配置必须是 JSON 对象");
            }
        } catch (ParamValiateException e) {
            throw e;
        } catch (Exception e) {
            throw new ParamValiateException("规则集配置不是合法 JSON");
        }
        return configJson.trim();
    }

    private void requireCurrentTeam(String teamId) {
        if (!identityResolver.current().getTeamId().equals(teamId)) {
            throw new ParamValiateException("无权访问其他团队规则集");
        }
    }

    private DocumentIdentity requireAdmin() {
        DocumentIdentity identity = identityResolver.current();
        if (!identity.isAdmin()) {
            throw new ParamValiateException("仅管理员可以维护团队规则集");
        }
        return identity;
    }

    private String required(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ParamValiateException(message);
        }
        return value.trim();
    }

    private static final class CachedConfig {
        private final RuleConfig config;
        private final long expiresAt;

        private CachedConfig(RuleConfig config, long expiresAt) {
            this.config = config;
            this.expiresAt = expiresAt;
        }
    }
}
