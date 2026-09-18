package cn.strong.webapp.user.documentgovernance.service;

import cn.strong.common.exception.ParamValiateException;
import cn.strong.webapp.user.documentgovernance.context.DocumentIdentity;
import cn.strong.webapp.user.documentgovernance.context.DocumentIdentityResolver;
import cn.strong.webapp.user.documentgovernance.entity.TermEntity;
import cn.strong.webapp.user.documentgovernance.mapper.TermMapper;
import cn.strong.webapp.user.model.documentgovernance.TermRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TermService {

    private static final long CACHE_TTL_MILLIS = 5 * 60 * 1000L;

    private final TermMapper termMapper;
    private final DocumentIdentityResolver identityResolver;
    private final Map<String, CachedTerms> termsCache = new ConcurrentHashMap<>();

    public TermService(TermMapper termMapper, DocumentIdentityResolver identityResolver) {
        this.termMapper = termMapper;
        this.identityResolver = identityResolver;
    }

    public List<TermEntity> list(String teamId) {
        requireCurrentTeam(teamId);
        CachedTerms cached = termsCache.get(teamId);
        long now = System.currentTimeMillis();
        if (cached != null && cached.expiresAt > now) {
            return cached.terms;
        }
        List<TermEntity> terms = termMapper.selectByTeamId(teamId);
        List<TermEntity> result = Collections.unmodifiableList(
                terms == null ? new ArrayList<>() : new ArrayList<>(terms));
        termsCache.put(teamId, new CachedTerms(result, now + CACHE_TTL_MILLIS));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public TermEntity upsert(TermRequest request) {
        DocumentIdentity identity = requireAdmin();
        if (request == null) {
            throw new ParamValiateException("术语请求不能为空");
        }
        String term = required(request.getTerm(), "术语不能为空");
        long now = System.currentTimeMillis();
        TermEntity existing = termMapper.selectByTeamAndTerm(identity.getTeamId(), term);
        TermEntity entity = existing == null ? new TermEntity() : existing;
        entity.setTeamId(identity.getTeamId());
        entity.setTerm(term);
        entity.setPreferredTerm(safe(request.getPreferredTerm()));
        entity.setForbidden(Boolean.TRUE.equals(request.getForbidden()));
        entity.setDescription(safe(request.getDescription()));
        entity.setUpdatedAt(now);
        if (existing == null) {
            entity.setCreatedBy(identity.getUserId());
            entity.setCreatedAt(now);
            termMapper.insert(entity);
        } else {
            termMapper.updateById(entity);
        }
        termsCache.remove(identity.getTeamId());
        return entity;
    }

    private void requireCurrentTeam(String teamId) {
        if (!identityResolver.current().getTeamId().equals(teamId)) {
            throw new ParamValiateException("无权访问其他团队术语");
        }
    }

    private DocumentIdentity requireAdmin() {
        DocumentIdentity identity = identityResolver.current();
        if (!identity.isAdmin()) {
            throw new ParamValiateException("仅管理员可以维护团队术语");
        }
        return identity;
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

    private static final class CachedTerms {
        private final List<TermEntity> terms;
        private final long expiresAt;

        private CachedTerms(List<TermEntity> terms, long expiresAt) {
            this.terms = terms;
            this.expiresAt = expiresAt;
        }
    }
}
