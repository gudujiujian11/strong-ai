package cn.strong.webapp.user.documentgovernance.service;

import cn.strong.common.exception.ParamValiateException;
import cn.strong.webapp.user.documentgovernance.context.DocumentIdentity;
import cn.strong.webapp.user.documentgovernance.context.DocumentIdentityResolver;
import cn.strong.webapp.user.documentgovernance.entity.TemplateEntity;
import cn.strong.webapp.user.documentgovernance.mapper.TemplateMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateService {

    private static final long CACHE_TTL_MILLIS = 5 * 60 * 1000L;

    private final TemplateMapper templateMapper;
    private final DocumentIdentityResolver identityResolver;
    private final Map<String, CachedHeadings> headingsCache = new ConcurrentHashMap<>();

    public TemplateService(TemplateMapper templateMapper, DocumentIdentityResolver identityResolver) {
        this.templateMapper = templateMapper;
        this.identityResolver = identityResolver;
    }

    public List<TemplateEntity> list(String teamId) {
        requireCurrentTeam(teamId);
        List<TemplateEntity> templates = templateMapper.selectByTeamId(teamId);
        return Collections.unmodifiableList(templates == null ? new ArrayList<>() : new ArrayList<>(templates));
    }

    public List<String> requiredHeadings(String teamId, String documentType) {
        requireCurrentTeam(teamId);
        String key = teamId + "\u0000" + documentType;
        CachedHeadings cached = headingsCache.get(key);
        long now = System.currentTimeMillis();
        if (cached != null && cached.expiresAt > now) {
            return cached.headings;
        }
        LinkedHashSet<String> headings = new LinkedHashSet<>();
        List<TemplateEntity> templates = templateMapper.selectByTeamAndDocumentType(teamId, documentType);
        if (templates != null) {
            for (TemplateEntity template : templates) {
                for (String heading : template.getRequiredHeadings().split("\\r?\\n")) {
                    String normalized = heading.trim();
                    if (!normalized.isEmpty()) {
                        headings.add(normalized);
                    }
                }
            }
        }
        List<String> result = Collections.unmodifiableList(new ArrayList<>(headings));
        headingsCache.put(key, new CachedHeadings(result, now + CACHE_TTL_MILLIS));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public TemplateEntity upsert(TemplateEntity request) {
        DocumentIdentity identity = requireAdmin();
        if (request == null) {
            throw new ParamValiateException("模板请求不能为空");
        }
        String name = required(request.getName(), "模板名称不能为空");
        String documentType = required(request.getDocumentType(), "文档类型不能为空");
        long now = System.currentTimeMillis();
        TemplateEntity existing = templateMapper.selectByTeamAndName(identity.getTeamId(), name);
        TemplateEntity template = existing == null ? new TemplateEntity() : existing;
        template.setTeamId(identity.getTeamId());
        template.setName(name);
        template.setDocumentType(documentType);
        template.setRequiredHeadings(request.getRequiredHeadings() == null ? "" : request.getRequiredHeadings().trim());
        template.setUpdatedAt(now);
        if (existing == null) {
            template.setCreatedBy(identity.getUserId());
            template.setCreatedAt(now);
            templateMapper.insert(template);
        } else {
            templateMapper.updateById(template);
        }
        invalidateTeam(identity.getTeamId());
        return template;
    }

    private void requireCurrentTeam(String teamId) {
        if (!identityResolver.current().getTeamId().equals(teamId)) {
            throw new ParamValiateException("无权访问其他团队模板");
        }
    }

    private DocumentIdentity requireAdmin() {
        DocumentIdentity identity = identityResolver.current();
        if (!identity.isAdmin()) {
            throw new ParamValiateException("仅管理员可以维护团队模板");
        }
        return identity;
    }

    private void invalidateTeam(String teamId) {
        headingsCache.keySet().removeIf(key -> key.startsWith(teamId + "\u0000"));
    }

    private String required(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ParamValiateException(message);
        }
        return value.trim();
    }

    private static final class CachedHeadings {
        private final List<String> headings;
        private final long expiresAt;

        private CachedHeadings(List<String> headings, long expiresAt) {
            this.headings = headings;
            this.expiresAt = expiresAt;
        }
    }
}
