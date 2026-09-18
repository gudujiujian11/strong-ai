package cn.strong.webapp.user.documentgovernance.service;

import cn.strong.webapp.user.documentgovernance.context.DocumentIdentity;
import cn.strong.webapp.user.documentgovernance.context.DocumentIdentityResolver;
import cn.strong.webapp.user.documentgovernance.entity.DocumentEntity;
import cn.strong.webapp.user.documentgovernance.entity.QualityIssueEntity;
import cn.strong.webapp.user.documentgovernance.entity.QualityReportEntity;
import cn.strong.webapp.user.documentgovernance.mapper.QualityIssueMapper;
import cn.strong.webapp.user.documentgovernance.mapper.QualityReportMapper;
import cn.strong.webapp.user.model.documentgovernance.QualityCheckResponse;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QualityReportServiceTest {

    private DocumentService documentService;
    private TemplateService templateService;
    private TermService termService;
    private QualityReportMapper reportMapper;
    private QualityIssueMapper issueMapper;
    private QualityReportService service;

    @BeforeEach
    void setUp() {
        documentService = mock(DocumentService.class);
        templateService = mock(TemplateService.class);
        termService = mock(TermService.class);
        reportMapper = mock(QualityReportMapper.class);
        issueMapper = mock(QualityIssueMapper.class);
        service = new QualityReportService(documentService, templateService, termService,
                new MarkdownQualityChecker(), reportMapper, issueMapper, new DocumentIdentityResolver());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(DocumentIdentityResolver.ATTRIBUTE_NAME,
                new DocumentIdentity("u-1", "team-a", "member"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void savesReportAndEveryDeterministicIssueForCurrentTeamDocument() {
        DocumentEntity document = new DocumentEntity();
        document.setId(1L);
        document.setTeamId("team-a");
        document.setDocumentType("README");
        document.setDraftContent("# README\n[危险](javascript:alert(1))");
        when(documentService.requireReadable(1L)).thenReturn(document);
        when(templateService.requiredHeadings("team-a", "README")).thenReturn(Collections.singletonList("安装"));
        when(termService.list("team-a")).thenReturn(Collections.emptyList());
        doAnswer(invocation -> {
            invocation.<QualityReportEntity>getArgument(0).setId(10L);
            return 1;
        }).when(reportMapper).insert(any(QualityReportEntity.class));

        QualityCheckResponse response = service.checkAndSave(1L);

        ArgumentCaptor<QualityIssueEntity> issues = ArgumentCaptor.forClass(QualityIssueEntity.class);
        org.mockito.Mockito.verify(issueMapper, org.mockito.Mockito.times(2)).insert(issues.capture());
        assertEquals(Long.valueOf(10L), response.getReportId());
        assertEquals(2, response.getIssues().size());
        assertEquals("team-a", issues.getAllValues().get(0).getTeamId());
    }
}
