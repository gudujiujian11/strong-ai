package cn.strong.webapp.user.promptlibrary.service;

import cn.strong.common.exception.ParamValiateException;
import cn.strong.webapp.user.promptlibrary.entity.PromptEntity;
import cn.strong.webapp.user.promptlibrary.entity.PromptVersionEntity;
import cn.strong.webapp.user.promptlibrary.mapper.PromptMapper;
import cn.strong.webapp.user.promptlibrary.mapper.PromptVersionMapper;
import cn.strong.webapp.user.promptlibrary.model.PromptDetail;
import cn.strong.webapp.user.promptlibrary.model.PromptFavoriteRequest;
import cn.strong.webapp.user.promptlibrary.model.PromptRatingRequest;
import cn.strong.webapp.user.promptlibrary.model.PromptRequest;
import cn.strong.webapp.user.promptlibrary.model.PromptSummary;
import cn.strong.webapp.user.promptlibrary.model.PromptVersion;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromptLibraryService {

    private static final Set<String> CATEGORIES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "JAVA_REVIEW", "SQL_OPTIMIZE", "DOC_WRITING", "TROUBLESHOOT")));

    private final PromptMapper promptMapper;
    private final PromptVersionMapper versionMapper;

    public PromptLibraryService(PromptMapper promptMapper, PromptVersionMapper versionMapper) {
        this.promptMapper = promptMapper;
        this.versionMapper = versionMapper;
    }

    public List<PromptSummary> list(String category, String tag, String q) {
        if (category != null && !category.trim().isEmpty()) validateCategory(category);
        String normalizedTag = normalizeTagValue(tag);
        String normalizedQuery = normalizeOptional(q);
        if (normalizedQuery.length() > 500) throw new ParamValiateException("搜索内容不能超过 500 个字符");
        List<PromptSummary> result = new ArrayList<>();
        for (PromptEntity entity : promptMapper.findList(normalizeOptional(category), normalizedTag, normalizedQuery)) {
            result.add(toSummary(entity));
        }
        return result;
    }

    public PromptDetail get(Long id) {
        return toDetail(requirePrompt(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public PromptDetail create(PromptRequest request) {
        ValidatedPrompt validated = validateRequest(request);
        long now = System.currentTimeMillis();
        PromptEntity entity = new PromptEntity();
        entity.setCategory(validated.category);
        entity.setTitle(validated.title);
        entity.setDescription(validated.description);
        entity.setTags(validated.tags);
        entity.setContent(validated.content);
        entity.setCurrentVersionNo(0L);
        entity.setRating(0);
        entity.setFavoriteCount(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        if (promptMapper.insertPrompt(entity) != 1) {
            throw new IllegalStateException("创建提示词失败");
        }
        if (!isBlank(entity.getContent())) publish(entity, now);
        return toDetail(requirePrompt(entity.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public PromptDetail update(Long id, PromptRequest request) {
        PromptEntity existing = requireLockedPrompt(id);
        ValidatedPrompt validated = validateRequest(request);
        existing.setCategory(validated.category);
        existing.setTitle(validated.title);
        existing.setDescription(validated.description);
        existing.setTags(validated.tags);
        existing.setContent(validated.content);
        existing.setUpdatedAt(System.currentTimeMillis());
        if (promptMapper.updateContent(existing) != 1) {
            throw new IllegalStateException("更新提示词失败");
        }
        return toDetail(requirePrompt(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireLockedPrompt(id);
        versionMapper.deleteByPromptId(id);
        if (promptMapper.deleteByPromptId(id) != 1) {
            throw new ParamValiateException("提示词不存在或已被删除");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public PromptDetail publish(Long id) {
        PromptEntity prompt = requireLockedPrompt(id);
        if (isBlank(prompt.getContent())) {
            throw new ParamValiateException("没有可发布的提示词正文");
        }
        String latestContent = versionMapper.findLatestContent(id);
        if (prompt.getCurrentVersionNo() != null && prompt.getCurrentVersionNo() > 0
                && prompt.getContent().equals(latestContent)) {
            throw new ParamValiateException("内容无变更");
        }
        publish(prompt, System.currentTimeMillis());
        return toDetail(requirePrompt(id));
    }

    public List<PromptVersion> versions(Long id) {
        requirePrompt(id);
        List<PromptVersion> result = new ArrayList<>();
        for (PromptVersionEntity entity : versionMapper.findByPromptId(id)) result.add(toVersion(entity, false));
        return result;
    }

    public PromptVersion version(Long id, Long versionNo) {
        requirePrompt(id);
        PromptVersionEntity entity = versionMapper.findOne(id, requireVersionNo(versionNo));
        if (entity == null) throw new ParamValiateException("提示词版本不存在或已被删除");
        return toVersion(entity, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public PromptDetail restore(Long id, Long versionNo) {
        requireLockedPrompt(id);
        PromptVersionEntity version = versionMapper.findOne(id, requireVersionNo(versionNo));
        if (version == null) throw new ParamValiateException("提示词版本不存在或已被删除");
        if (promptMapper.updateWorkingContent(id, version.getContent(), System.currentTimeMillis()) != 1) {
            throw new IllegalStateException("恢复提示词版本失败");
        }
        return toDetail(requirePrompt(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public PromptDetail rate(Long id, PromptRatingRequest request) {
        requireLockedPrompt(id);
        if (request == null || request.getScore() == null || request.getScore() < 1 || request.getScore() > 5) {
            throw new ParamValiateException("评分必须是 1 到 5 的整数");
        }
        if (promptMapper.updateRating(id, request.getScore(), System.currentTimeMillis()) != 1) {
            throw new IllegalStateException("更新评分失败");
        }
        return toDetail(requirePrompt(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public PromptDetail favorite(Long id, PromptFavoriteRequest request) {
        requireLockedPrompt(id);
        if (request == null || request.getFavorited() == null) {
            throw new ParamValiateException("收藏状态不能为空");
        }
        int delta = request.getFavorited() ? 1 : -1;
        if (promptMapper.updateFavoriteCount(id, delta, System.currentTimeMillis()) != 1) {
            throw new IllegalStateException("更新收藏数失败");
        }
        return toDetail(requirePrompt(id));
    }

    private void publish(PromptEntity prompt, long now) {
        long nextVersion = prompt.getCurrentVersionNo() == null ? 1L : prompt.getCurrentVersionNo() + 1L;
        PromptVersionEntity version = new PromptVersionEntity();
        version.setPromptId(prompt.getId());
        version.setVersionNo(nextVersion);
        version.setContent(prompt.getContent());
        version.setCreatedAt(now);
        if (versionMapper.insertVersion(version) != 1
                || promptMapper.updateVersionNo(prompt.getId(), nextVersion, now) != 1) {
            throw new IllegalStateException("发布提示词版本失败");
        }
    }

    private PromptEntity requirePrompt(Long id) {
        return requirePrompt(id, false);
    }

    private PromptEntity requireLockedPrompt(Long id) {
        return requirePrompt(id, true);
    }

    private PromptEntity requirePrompt(Long id, boolean lock) {
        if (id == null || id < 1) throw new ParamValiateException("提示词 ID 无效");
        PromptEntity entity = lock ? promptMapper.findByIdForUpdate(id) : promptMapper.findById(id);
        if (entity == null) throw new ParamValiateException("提示词不存在或已被删除");
        return entity;
    }

    private ValidatedPrompt validateRequest(PromptRequest request) {
        if (request == null) throw new ParamValiateException("提示词请求不能为空");
        String category = required(request.getCategory(), "分类不能为空");
        validateCategory(category);
        String title = required(request.getTitle(), "标题不能为空");
        if (title.length() > 100) throw new ParamValiateException("标题不能超过 100 个字符");
        String description = normalizeOptional(request.getDescription());
        if (description.length() > 500) throw new ParamValiateException("说明不能超过 500 个字符");
        String tags = normalizeTags(request.getTags());
        if (tags.length() > 500) throw new ParamValiateException("标签不能超过 500 个字符");
        String content = request.getContent() == null ? "" : request.getContent();
        if (content.length() > 65535) throw new ParamValiateException("正文不能超过 65535 个字符");
        return new ValidatedPrompt(category, title, description, tags, content);
    }

    private void validateCategory(String category) {
        if (!CATEGORIES.contains(category.trim())) throw new ParamValiateException("不支持的提示词分类");
    }

    private String normalizeTags(String value) {
        String tags = normalizeOptional(value).replace('，', ',');
        String[] parts = tags.split(",");
        List<String> normalized = new ArrayList<>();
        for (String part : parts) {
            String tag = part.trim();
            if (!tag.isEmpty() && !normalized.contains(tag)) normalized.add(tag);
        }
        return String.join(",", normalized);
    }

    private String normalizeTagValue(String value) {
        String tag = normalizeOptional(value);
        if (tag.length() > 500) throw new ParamValiateException("标签不能超过 500 个字符");
        if (tag.indexOf(',') >= 0 || tag.indexOf('，') >= 0) throw new ParamValiateException("标签筛选只支持单个标签");
        return tag;
    }

    private String required(String value, String message) {
        if (isBlank(value)) throw new ParamValiateException(message);
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Long requireVersionNo(Long versionNo) {
        if (versionNo == null || versionNo < 1) throw new ParamValiateException("版本号无效");
        return versionNo;
    }

    private PromptSummary toSummary(PromptEntity entity) {
        PromptSummary result = new PromptSummary();
        populateSummary(result, entity);
        return result;
    }

    private void populateSummary(PromptSummary result, PromptEntity entity) {
        result.setId(entity.getId());
        result.setCategory(entity.getCategory());
        result.setTitle(entity.getTitle());
        result.setDescription(entity.getDescription());
        result.setTags(entity.getTags());
        result.setCurrentVersionNo(entity.getCurrentVersionNo());
        result.setRating(entity.getRating());
        result.setFavoriteCount(entity.getFavoriteCount());
        result.setCreatedAt(entity.getCreatedAt());
        result.setUpdatedAt(entity.getUpdatedAt());
    }

    private PromptDetail toDetail(PromptEntity entity) {
        PromptDetail result = new PromptDetail();
        populateSummary(result, entity);
        result.setContent(entity.getContent());
        return result;
    }

    private PromptVersion toVersion(PromptVersionEntity entity, boolean includeContent) {
        PromptVersion result = new PromptVersion();
        result.setId(entity.getId());
        result.setPromptId(entity.getPromptId());
        result.setVersionNo(entity.getVersionNo());
        result.setCreatedAt(entity.getCreatedAt());
        if (includeContent) result.setContent(entity.getContent());
        return result;
    }

    private static final class ValidatedPrompt {
        private final String category;
        private final String title;
        private final String description;
        private final String tags;
        private final String content;

        private ValidatedPrompt(String category, String title, String description, String tags, String content) {
            this.category = category;
            this.title = title;
            this.description = description;
            this.tags = tags;
            this.content = content;
        }
    }
}
