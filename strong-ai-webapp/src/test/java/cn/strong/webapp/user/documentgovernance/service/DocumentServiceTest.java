package cn.strong.webapp.user.documentgovernance.service;

import cn.strong.common.exception.ParamValiateException;
import cn.strong.webapp.user.documentgovernance.context.DocumentIdentity;
import cn.strong.webapp.user.documentgovernance.context.DocumentIdentityResolver;
import cn.strong.webapp.user.documentgovernance.entity.DocumentEntity;
import cn.strong.webapp.user.documentgovernance.entity.DocumentVersionEntity;
import cn.strong.webapp.user.documentgovernance.mapper.DocumentMapper;
import cn.strong.webapp.user.documentgovernance.mapper.DocumentVersionMapper;
import cn.strong.webapp.user.model.documentgovernance.DocumentCreateRequest;
import cn.strong.webapp.user.model.documentgovernance.DocumentSaveRequest;
import cn.strong.webapp.user.model.documentgovernance.DocumentVersionResponse;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentServiceTest {

    private DocumentMapper documentMapper;
    private DocumentVersionMapper versionMapper;
    private DocumentService service;

    @BeforeEach
    void setUp() {
        documentMapper = mock(DocumentMapper.class);
        versionMapper = mock(DocumentVersionMapper.class);
        service = new DocumentService(documentMapper, versionMapper, new DocumentIdentityResolver(),
                new DocumentDiffService());
        useIdentity("u-1", "team-a", "member");
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void publishCreatesAnImmutableIncrementingVersion() {
        DocumentEntity document = document("# V1", "u-1");
        when(documentMapper.selectById(1L)).thenReturn(document);
        when(versionMapper.maxVersionNo(1L)).thenReturn(0L, 1L);
        AtomicLong id = new AtomicLong(10L);
        doAnswer(invocation -> { invocation.<DocumentVersionEntity>getArgument(0).setId(id.getAndIncrement()); return 1; })
                .when(versionMapper).insert(any(DocumentVersionEntity.class));

        DocumentVersionResponse first = service.publish(1L, "首次发布");
        document.setDraftContent("# V2");
        DocumentVersionResponse second = service.publish(1L, "第二次发布");

        ArgumentCaptor<DocumentVersionEntity> captor = ArgumentCaptor.forClass(DocumentVersionEntity.class);
        org.mockito.Mockito.verify(versionMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertEquals(Long.valueOf(1L), first.getVersionNo());
        assertEquals(Long.valueOf(2L), second.getVersionNo());
        assertEquals("# V1", captor.getAllValues().get(0).getContent());
        assertEquals("# V2", captor.getAllValues().get(1).getContent());
    }

    @Test
    void memberCannotOverwriteAnotherMembersDraft() {
        when(documentMapper.selectById(1L)).thenReturn(document("# protected", "u-2"));
        DocumentSaveRequest request = new DocumentSaveRequest();
        request.setContent("# altered");

        assertThrows(ParamValiateException.class, () -> service.saveDraft(1L, request));
    }

    @Test
    void rollbackCopiesHistoricalContentIntoANewVersion() {
        DocumentEntity document = document("# current", "u-1");
        DocumentVersionEntity history = new DocumentVersionEntity();
        history.setId(7L);
        history.setDocumentId(1L);
        history.setTeamId("team-a");
        history.setVersionNo(1L);
        history.setContent("# stable");
        when(documentMapper.selectById(1L)).thenReturn(document);
        when(versionMapper.selectByDocumentAndNo(1L, 1L)).thenReturn(history);
        when(versionMapper.maxVersionNo(1L)).thenReturn(2L);
        doAnswer(invocation -> { invocation.<DocumentVersionEntity>getArgument(0).setId(12L); return 1; })
                .when(versionMapper).insert(any(DocumentVersionEntity.class));

        DocumentVersionResponse restored = service.rollback(1L, 1L, "回滚到稳定版本");

        assertEquals(Long.valueOf(3L), restored.getVersionNo());
        assertEquals("# stable", restored.getContent());
        assertEquals("# stable", document.getDraftContent());
    }

    @Test
    void savingDraftOnPublishedDocumentMarksUnpublishedChanges() {
        DocumentEntity document = document("# published", "u-1");
        document.setStatus("PUBLISHED");
        when(documentMapper.selectById(1L)).thenReturn(document);
        DocumentSaveRequest request = new DocumentSaveRequest();
        request.setContent("# edited");

        service.saveDraft(1L, request);

        assertEquals(Integer.valueOf(1), document.getHasUnpublishedChanges());
    }

    @Test
    void publishClearsUnpublishedChanges() {
        DocumentEntity document = document("# draft", "u-1");
        document.setStatus("PUBLISHED");
        document.setHasUnpublishedChanges(1);
        when(documentMapper.selectById(1L)).thenReturn(document);
        when(versionMapper.maxVersionNo(1L)).thenReturn(1L);
        doAnswer(invocation -> { invocation.<DocumentVersionEntity>getArgument(0).setId(20L); return 1; })
                .when(versionMapper).insert(any(DocumentVersionEntity.class));

        service.publish(1L, "发布");

        assertEquals(Integer.valueOf(0), document.getHasUnpublishedChanges());
    }

    @Test
    void archivedDocumentCannotBeEdited() {
        DocumentEntity document = document("# archived", "u-1");
        document.setStatus("ARCHIVED");
        when(documentMapper.selectById(1L)).thenReturn(document);
        DocumentSaveRequest request = new DocumentSaveRequest();
        request.setContent("# edit");

        assertThrows(ParamValiateException.class, () -> service.saveDraft(1L, request));
    }

    @Test
    void statusTransitionRejectsIllegalTarget() {
        DocumentEntity document = document("# draft", "u-1");
        document.setStatus("DRAFT");
        when(documentMapper.selectById(1L)).thenReturn(document);

        assertThrows(ParamValiateException.class,
                () -> service.changeStatus(1L, cn.strong.webapp.user.model.documentgovernance.DocumentStatus.PUBLISHED));
    }

    @Test
    void archiveThenRestoreFollowsStatusMachine() {
        DocumentEntity document = document("# doc", "u-1");
        document.setStatus("PUBLISHED");
        when(documentMapper.selectById(1L)).thenReturn(document);

        service.changeStatus(1L, cn.strong.webapp.user.model.documentgovernance.DocumentStatus.ARCHIVED);
        assertEquals("ARCHIVED", document.getStatus());

        service.changeStatus(1L, cn.strong.webapp.user.model.documentgovernance.DocumentStatus.PUBLISHED);
        assertEquals("PUBLISHED", document.getStatus());
    }

    @Test
    void diffComparesAnyTwoVersions() {
        DocumentEntity document = document("# current", "u-1");
        document.setStatus("PUBLISHED");
        when(documentMapper.selectById(1L)).thenReturn(document);
        DocumentVersionEntity v1 = version(1L, "line1\nline2");
        DocumentVersionEntity v2 = version(2L, "line1\nline2\nline3");
        when(versionMapper.selectByDocumentAndNo(1L, 1L)).thenReturn(v1);
        when(versionMapper.selectByDocumentAndNo(1L, 2L)).thenReturn(v2);

        var response = service.diff(1L, 1L, "2");

        assertEquals(Long.valueOf(1L), response.getFromVersionNo());
        assertEquals(Long.valueOf(2L), response.getToVersionNo());
        boolean hasInsert = response.getHunks().stream().anyMatch(h -> "INSERT".equals(h.getType()));
        org.junit.jupiter.api.Assertions.assertTrue(hasInsert);
    }

    @Test
    void diffAgainstDraftUsesDraftContent() {
        DocumentEntity document = document("# current\nnew draft line", "u-1");
        document.setStatus("PUBLISHED");
        when(documentMapper.selectById(1L)).thenReturn(document);
        when(versionMapper.selectByDocumentAndNo(1L, 3L)).thenReturn(version(3L, "# current"));

        var response = service.diff(1L, 3L, "draft");

        org.junit.jupiter.api.Assertions.assertTrue(response.isAgainstDraft());
        assertEquals(null, response.getToVersionNo());
    }

    private DocumentVersionEntity version(long versionNo, String content) {
        DocumentVersionEntity version = new DocumentVersionEntity();
        version.setId(versionNo);
        version.setDocumentId(1L);
        version.setTeamId("team-a");
        version.setVersionNo(versionNo);
        version.setContent(content);
        return version;
    }

    private DocumentEntity document(String content, String ownerUserId) {
        DocumentEntity document = new DocumentEntity();
        document.setId(1L);
        document.setTeamId("team-a");
        document.setOwnerUserId(ownerUserId);
        document.setTitle("部署说明");
        document.setDocumentType("DEPLOYMENT");
        document.setStatus("DRAFT");
        document.setDraftContent(content);
        return document;
    }

    private void useIdentity(String userId, String teamId, String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(DocumentIdentityResolver.ATTRIBUTE_NAME, new DocumentIdentity(userId, teamId, role));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
