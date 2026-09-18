package cn.strong.webapp.user.documentgovernance.service;

import cn.strong.common.exception.ParamValiateException;
import cn.strong.webapp.user.documentgovernance.context.DocumentIdentity;
import cn.strong.webapp.user.documentgovernance.context.DocumentIdentityResolver;
import cn.strong.webapp.user.documentgovernance.entity.DocumentEntity;
import cn.strong.webapp.user.documentgovernance.entity.DocumentVersionEntity;
import cn.strong.webapp.user.documentgovernance.mapper.DocumentMapper;
import cn.strong.webapp.user.documentgovernance.mapper.DocumentVersionMapper;
import cn.strong.webapp.user.model.documentgovernance.DocumentCreateRequest;
import cn.strong.webapp.user.model.documentgovernance.DocumentResponse;
import cn.strong.webapp.user.model.documentgovernance.DocumentSaveRequest;
import cn.strong.webapp.user.model.documentgovernance.DocumentStatus;
import cn.strong.webapp.user.model.documentgovernance.DocumentVersionResponse;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentService {

    private final DocumentMapper documentMapper;
    private final DocumentVersionMapper versionMapper;
    private final DocumentIdentityResolver identityResolver;

    public DocumentService(DocumentMapper documentMapper, DocumentVersionMapper versionMapper,
                           DocumentIdentityResolver identityResolver) {
        this.documentMapper = documentMapper;
        this.versionMapper = versionMapper;
        this.identityResolver = identityResolver;
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
        document.setDraftContent(required(request.getContent(), "文档内容不能为空"));
        document.setTags(safe(request.getTags()));
        document.setUpdatedAt(System.currentTimeMillis());
        documentMapper.updateById(document);
        return toResponse(document);
    }

    @Transactional(rollbackFor = Exception.class)
    public DocumentVersionResponse publish(Long documentId, String releaseNote) {
        DocumentEntity document = requireWritable(documentId);
        DocumentVersionEntity version = createVersion(document, document.getDraftContent(), releaseNote);
        versionMapper.insert(version);
        document.setCurrentVersionId(version.getId());
        document.setStatus(DocumentStatus.PUBLISHED.name());
        document.setUpdatedAt(System.currentTimeMillis());
        documentMapper.updateById(document);
        return toVersionResponse(version);
    }

    @Transactional(rollbackFor = Exception.class)
    public DocumentVersionResponse rollback(Long documentId, Long versionNo, String releaseNote) {
        DocumentEntity document = requireWritable(documentId);
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
        document.setUpdatedAt(System.currentTimeMillis());
        documentMapper.updateById(document);
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
        version.setDiffContent("");
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
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
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
}
