package cn.strong.webapp.user.documentgovernance.service;

import cn.strong.common.exception.ParamValiateException;
import cn.strong.webapp.user.documentgovernance.context.DocumentIdentity;
import cn.strong.webapp.user.documentgovernance.context.DocumentIdentityResolver;
import cn.strong.webapp.user.documentgovernance.entity.DocumentEntity;
import cn.strong.webapp.user.documentgovernance.entity.DocumentVersionEntity;
import cn.strong.webapp.user.documentgovernance.mapper.DocumentMapper;
import cn.strong.webapp.user.documentgovernance.mapper.DocumentVersionMapper;
import cn.strong.webapp.user.model.documentgovernance.DiffResponse;
import cn.strong.webapp.user.model.documentgovernance.DocumentCreateRequest;
import cn.strong.webapp.user.model.documentgovernance.DocumentResponse;
import cn.strong.webapp.user.model.documentgovernance.DocumentSaveRequest;
import cn.strong.webapp.user.model.documentgovernance.DocumentStatus;
import cn.strong.webapp.user.model.documentgovernance.DocumentVersionResponse;
import cn.strong.webapp.user.model.documentgovernance.DocumentVersionSummaryResponse;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class DocumentService {

    private static final String DRAFT_MARKER = "draft";

    private final DocumentMapper documentMapper;
    private final DocumentVersionMapper versionMapper;
    private final DocumentIdentityResolver identityResolver;
    private final DocumentDiffService diffService;

    @Autowired(required = false)
    private DocumentOperationLogService operationLogService;

    public DocumentService(DocumentMapper documentMapper, DocumentVersionMapper versionMapper,
                           DocumentIdentityResolver identityResolver, DocumentDiffService diffService) {
        this.documentMapper = documentMapper;
        this.versionMapper = versionMapper;
        this.identityResolver = identityResolver;
        this.diffService = diffService;
    }

    @Transactional(rollbackFor = Exception.class)
    public DocumentResponse create(DocumentCreateRequest request) {
        if (request == null) {
            throw new ParamValiateException("创建文档请求不能为空");
        }
        DocumentIdentity identity = identityResolver.current();
        long now = System.currentTimeMillis();
        DocumentEntity document = new DocumentEntity();
        document.setTeamId(identity.getTeamId());
        document.setOwnerUserId(identity.getUserId());
        document.setTitle(required(request.getTitle(), "文档标题不能为空"));
        document.setDocumentType(required(request.getDocumentType(), "文档类型不能为空"));
        document.setTags(safe(request.getTags()));
        document.setStatus(DocumentStatus.DRAFT.name());
        document.setDraftContent(required(request.getContent(), "文档内容不能为空"));
        document.setHasUnpublishedChanges(0);
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        documentMapper.insert(document);
        return toResponse(document);
    }

    @Transactional(rollbackFor = Exception.class)
    public DocumentResponse saveDraft(Long documentId, DocumentSaveRequest request) {
        if (request == null) {
            throw new ParamValiateException("保存文档请求不能为空");
        }
        DocumentEntity document = requireWritable(documentId);
        requireNotArchived(document);
        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            document.setTitle(required(request.getTitle(), "文档标题不能为空"));
        }
        document.setDraftContent(required(request.getContent(), "文档内容不能为空"));
        document.setTags(safe(request.getTags()));
        if (DocumentStatus.PUBLISHED.name().equals(document.getStatus())) {
            document.setHasUnpublishedChanges(1);
        }
        document.setUpdatedAt(System.currentTimeMillis());
        documentMapper.updateById(document);
        operation(document, "DRAFT_SAVED", identityResolver.current().getUserId());
        return toResponse(document);
    }

    @Transactional(rollbackFor = Exception.class)
    public DocumentVersionResponse publish(Long documentId, String releaseNote) {
        DocumentEntity document = requireWritable(documentId);
        requireNotArchived(document);
        DocumentVersionEntity version = createVersion(document, document.getDraftContent(), releaseNote);
        versionMapper.insert(version);
        document.setCurrentVersionId(version.getId());
        document.setStatus(DocumentStatus.PUBLISHED.name());
        document.setHasUnpublishedChanges(0);
        document.setUpdatedAt(System.currentTimeMillis());
        documentMapper.updateById(document);
        operation(document, "VERSION_PUBLISHED", identityResolver.current().getUserId());
        return toVersionResponse(version);
    }

    @Transactional(rollbackFor = Exception.class)
    public DocumentVersionResponse rollback(Long documentId, Long versionNo, String releaseNote) {
        DocumentEntity document = requireWritable(documentId);
        requireNotArchived(document);
        if (versionNo == null || versionNo <= 0) {
            throw new ParamValiateException("回滚版本号无效");
        }
        DocumentVersionEntity source = versionMapper.selectByDocumentAndNo(document.getId(), versionNo);
        if (source == null || !document.getTeamId().equals(source.getTeamId())) {
            throw new ParamValiateException("回滚版本不存在");
        }
        document.setDraftContent(source.getContent());
        DocumentVersionEntity restored = createVersion(document, source.getContent(), releaseNote);
        versionMapper.insert(restored);
        document.setCurrentVersionId(restored.getId());
        document.setStatus(DocumentStatus.PUBLISHED.name());
        document.setHasUnpublishedChanges(0);
        document.setUpdatedAt(System.currentTimeMillis());
        documentMapper.updateById(document);
        operation(document, "VERSION_ROLLED_BACK", identityResolver.current().getUserId());
        return toVersionResponse(restored);
    }

    public DocumentResponse get(Long documentId) {
        return toResponse(requireReadable(documentId));
    }

    public List<DocumentResponse> list() {
        DocumentIdentity identity = identityResolver.current();
        List<DocumentEntity> documents = documentMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocumentEntity>()
                        .eq(DocumentEntity::getTeamId, identity.getTeamId())
                        .orderByDesc(DocumentEntity::getUpdatedAt));
        if (documents == null) {
            return Collections.emptyList();
        }
        return documents.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public DocumentEntity requireReadable(Long documentId) {
        DocumentEntity document = requireTeamDocument(documentId);
        DocumentIdentity identity = identityResolver.current();
        if (!identity.isAdmin() && !identity.getUserId().equals(document.getOwnerUserId())
                && !DocumentStatus.PUBLISHED.name().equals(document.getStatus())) {
            throw new ParamValiateException("无权访问其他成员的草稿");
        }
        return document;
    }

    public DocumentEntity requireWritableDraft(Long documentId) {
        return requireWritable(documentId);
    }

    public DocumentEntity findByTeam(Long documentId, String teamId) {
        if (documentId == null || documentId <= 0 || teamId == null || teamId.trim().isEmpty()) {
            return null;
        }
        return documentMapper.selectByTeamAndId(documentId, teamId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void applyAiDraft(Long documentId, String teamId, String content) {
        DocumentEntity document = findByTeam(documentId, teamId);
        if (document == null) {
            throw new ParamValiateException("文档不存在或无权访问");
        }
        document.setDraftContent(required(content, "AI 优化结果不能为空"));
        document.setUpdatedAt(System.currentTimeMillis());
        documentMapper.updateById(document);
    }

    public List<DocumentVersionResponse> listVersions(Long documentId) {
        DocumentEntity document = requireReadable(documentId);
        List<DocumentVersionEntity> versions = versionMapper.selectByTeamAndDocument(document.getTeamId(), document.getId());
        if (versions == null) {
            return Collections.emptyList();
        }
        return versions.stream().map(this::toVersionResponse).collect(Collectors.toList());
    }

    public List<DocumentVersionSummaryResponse> listVersionSummaries(Long documentId) {
        DocumentEntity document = requireReadable(documentId);
        List<DocumentVersionEntity> versions = versionMapper.selectByTeamAndDocument(document.getTeamId(), document.getId());
        if (versions == null) {
            return Collections.emptyList();
        }
        return versions.stream().map(this::toVersionSummary).collect(Collectors.toList());
    }

    public DocumentVersionResponse getVersion(Long documentId, Long versionNo) {
        DocumentEntity document = requireReadable(documentId);
        if (versionNo == null || versionNo <= 0) {
            throw new ParamValiateException("版本号无效");
        }
        DocumentVersionEntity version = versionMapper.selectByDocumentAndNo(document.getId(), versionNo);
        if (version == null || !document.getTeamId().equals(version.getTeamId())) {
            throw new ParamValiateException("版本不存在");
        }
        return toVersionResponse(version);
    }

    public DiffResponse diff(Long documentId, Long fromVersionNo, String toVersionNo) {
        DocumentEntity document = requireReadable(documentId);
        DocumentVersionEntity from = requireVersion(document, fromVersionNo, "起始");
        if (DRAFT_MARKER.equalsIgnoreCase(toVersionNo)) {
            return diffService.compare(from.getVersionNo(), from.getContent(), null,
                    document.getDraftContent(), true);
        }
        DocumentVersionEntity to = requireVersion(document, parseVersionNo(toVersionNo), "目标");
        return diffService.compare(from.getVersionNo(), from.getContent(), to.getVersionNo(), to.getContent(), false);
    }

    @Transactional(rollbackFor = Exception.class)
    public DocumentResponse changeStatus(Long documentId, DocumentStatus target) {
        if (target == null) {
            throw new ParamValiateException("目标状态不能为空");
        }
        DocumentEntity document = requireWritable(documentId);
        DocumentStatus current = DocumentStatus.valueOf(document.getStatus());
        if (current == target) {
            return toResponse(document);
        }
        String action;
        if (target == DocumentStatus.ARCHIVED
                && (current == DocumentStatus.PUBLISHED || current == DocumentStatus.DRAFT)) {
            action = "DOCUMENT_ARCHIVED";
        } else if (target == DocumentStatus.PUBLISHED && current == DocumentStatus.ARCHIVED) {
            action = "DOCUMENT_RESTORED";
        } else {
            throw new ParamValiateException("不支持从 " + current + " 流转到 " + target);
        }
        document.setStatus(target.name());
        document.setUpdatedAt(System.currentTimeMillis());
        documentMapper.updateById(document);
        operation(document, action, identityResolver.current().getUserId());
        return toResponse(document);
    }

    private DocumentVersionEntity requireVersion(DocumentEntity document, Long versionNo, String label) {
        if (versionNo == null || versionNo <= 0) {
            throw new ParamValiateException(label + "版本号无效");
        }
        DocumentVersionEntity version = versionMapper.selectByDocumentAndNo(document.getId(), versionNo);
        if (version == null || !document.getTeamId().equals(version.getTeamId())) {
            throw new ParamValiateException(label + "版本不存在");
        }
        return version;
    }

    private Long parseVersionNo(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ParamValiateException("目标版本号无效");
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            throw new ParamValiateException("目标版本号无效");
        }
    }

    private void requireNotArchived(DocumentEntity document) {
        if (DocumentStatus.ARCHIVED.name().equals(document.getStatus())) {
            throw new ParamValiateException("文档已归档，请先恢复后再编辑");
        }
    }

    private DocumentEntity requireWritable(Long documentId) {
        DocumentEntity document = requireTeamDocument(documentId);
        DocumentIdentity identity = identityResolver.current();
        if (!identity.isAdmin() && !identity.getUserId().equals(document.getOwnerUserId())) {
            throw new ParamValiateException("无权编辑其他成员的文档");
        }
        return document;
    }

    private DocumentEntity requireTeamDocument(Long documentId) {
        if (documentId == null || documentId <= 0) {
            throw new ParamValiateException("文档 ID 无效");
        }
        DocumentEntity document = documentMapper.selectById(documentId);
        DocumentIdentity identity = identityResolver.current();
        if (document == null || !identity.getTeamId().equals(document.getTeamId())) {
            throw new ParamValiateException("文档不存在或无权访问");
        }
        return document;
    }

    private DocumentVersionEntity createVersion(DocumentEntity document, String content, String releaseNote) {
        Long latest = versionMapper.maxVersionNo(document.getId());
        DocumentVersionEntity version = new DocumentVersionEntity();
        version.setDocumentId(document.getId());
        version.setTeamId(document.getTeamId());
        version.setVersionNo((latest == null ? 0L : latest) + 1L);
        version.setContent(content);
        DocumentVersionEntity previous = document.getCurrentVersionId() == null ? null
                : versionMapper.selectByTeamAndId(document.getTeamId(), document.getCurrentVersionId());
        version.setDiffContent(snapshotDiff(previous == null ? "" : previous.getContent(), content));
        version.setReleaseNote(safe(releaseNote));
        version.setCreatedBy(identityResolver.current().getUserId());
        version.setCreatedAt(System.currentTimeMillis());
        return version;
    }

    private DocumentResponse toResponse(DocumentEntity document) {
        DocumentResponse response = new DocumentResponse();
        response.setId(document.getId());
        response.setTeamId(document.getTeamId());
        response.setOwnerUserId(document.getOwnerUserId());
        response.setTitle(document.getTitle());
        response.setDocumentType(document.getDocumentType());
        response.setTags(document.getTags());
        response.setStatus(DocumentStatus.valueOf(document.getStatus()));
        response.setDraftContent(document.getDraftContent());
        response.setCurrentVersionId(document.getCurrentVersionId());
        response.setHasUnpublishedChanges(document.getHasUnpublishedChanges() != null
                && document.getHasUnpublishedChanges() == 1);
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
        return response;
    }

    private DocumentVersionSummaryResponse toVersionSummary(DocumentVersionEntity version) {
        DocumentVersionSummaryResponse response = new DocumentVersionSummaryResponse();
        response.setId(version.getId());
        response.setDocumentId(version.getDocumentId());
        response.setVersionNo(version.getVersionNo());
        response.setReleaseNote(version.getReleaseNote());
        response.setCreatedBy(version.getCreatedBy());
        response.setCreatedAt(version.getCreatedAt());
        return response;
    }

    private DocumentVersionResponse toVersionResponse(DocumentVersionEntity version) {
        DocumentVersionResponse response = new DocumentVersionResponse();
        response.setId(version.getId());
        response.setDocumentId(version.getDocumentId());
        response.setVersionNo(version.getVersionNo());
        response.setContent(version.getContent());
        response.setDiffContent(version.getDiffContent());
        response.setReleaseNote(version.getReleaseNote());
        response.setCreatedBy(version.getCreatedBy());
        response.setCreatedAt(version.getCreatedAt());
        return response;
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

    /**
     * 发版时保存的相邻版本差异快照，仅保留变更行的纯文本，前缀 - / + 表示删除/新增。
     * 对外的任意两版比较统一走 {@link #diff} 动态接口。
     */
    private String snapshotDiff(String before, String after) {
        StringBuilder diff = new StringBuilder();
        diffService.diffLines(before, after).forEach(hunk -> {
            if ("DELETE".equals(hunk.getType())) {
                diff.append("- ").append(hunk.getText()).append('\n');
            } else if ("INSERT".equals(hunk.getType())) {
                diff.append("+ ").append(hunk.getText()).append('\n');
            }
        });
        return diff.toString();
    }

    private void operation(DocumentEntity document, String action, String actor) {
        if (operationLogService != null) {
            operationLogService.record(document.getTeamId(), document.getId(), actor, action, "{}");
        }
    }
}
