package cn.strong.webapp.user.documentgovernance.service;

import cn.strong.webapp.user.documentgovernance.context.DocumentIdentityResolver;
import cn.strong.webapp.user.documentgovernance.entity.DocumentEntity;
import cn.strong.webapp.user.documentgovernance.entity.QualityIssueEntity;
import cn.strong.webapp.user.documentgovernance.entity.QualityReportEntity;
import cn.strong.webapp.user.documentgovernance.mapper.QualityIssueMapper;
import cn.strong.webapp.user.documentgovernance.mapper.QualityReportMapper;
import cn.strong.webapp.user.model.documentgovernance.QualityCheckResponse;
import cn.strong.webapp.user.model.documentgovernance.QualityIssue;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class QualityReportService {

    private final DocumentService documentService;
    private final TemplateService templateService;
    private final TermService termService;
    private final MarkdownQualityChecker checker;
    private final QualityReportMapper reportMapper;
    private final QualityIssueMapper issueMapper;
    private final DocumentIdentityResolver identityResolver;

    @Autowired(required = false)
    private DocumentOperationLogService operationLogService;

    @Autowired(required = false)
    private DocumentMetricsService metricsService;

    public QualityReportService(DocumentService documentService, TemplateService templateService,
                                TermService termService, MarkdownQualityChecker checker,
                                QualityReportMapper reportMapper, QualityIssueMapper issueMapper,
                                DocumentIdentityResolver identityResolver) {
        this.documentService = documentService;
        this.templateService = templateService;
        this.termService = termService;
        this.checker = checker;
        this.reportMapper = reportMapper;
        this.issueMapper = issueMapper;
        this.identityResolver = identityResolver;
    }

    @Transactional(rollbackFor = Exception.class)
    public QualityCheckResponse checkAndSave(Long documentId) {
        DocumentEntity document = documentService.requireReadable(documentId);
        List<QualityIssue> issues = checker.check(document.getDraftContent(),
                templateService.requiredHeadings(document.getTeamId(), document.getDocumentType()),
                termService.list(document.getTeamId()));
        QualityReportEntity report = new QualityReportEntity();
        report.setTeamId(document.getTeamId());
        report.setDocumentId(document.getId());
        report.setDocumentVersionId(document.getCurrentVersionId());
        report.setScore(score(issues));
        report.setCheckedBy(identityResolver.current().getUserId());
        report.setCreatedAt(System.currentTimeMillis());
        reportMapper.insert(report);
        for (QualityIssue issue : issues) {
            issueMapper.insert(toEntity(document.getTeamId(), report.getId(), issue));
            if (metricsService != null) {
                metricsService.recordQualityCheck(document.getDocumentType(), issue.getRuleCode());
            }
        }
        if (operationLogService != null) {
            operationLogService.record(document.getTeamId(), document.getId(), identityResolver.current().getUserId(),
                    "QUALITY_CHECKED", "{\"ruleCount\":" + issues.size() + "}");
        }
        return response(report, issues);
    }

    private int score(List<QualityIssue> issues) {
        int deductions = 0;
        for (QualityIssue issue : issues) {
            switch (issue.getSeverity()) {
                case HIGH:
                    deductions += 30;
                    break;
                case MEDIUM:
                    deductions += 15;
                    break;
                case LOW:
                    deductions += 5;
                    break;
                default:
                    break;
            }
        }
        return Math.max(0, 100 - deductions);
    }

    private QualityIssueEntity toEntity(String teamId, Long reportId, QualityIssue issue) {
        QualityIssueEntity entity = new QualityIssueEntity();
        entity.setTeamId(teamId);
        entity.setReportId(reportId);
        entity.setRuleCode(issue.getRuleCode());
        entity.setSeverity(issue.getSeverity().name());
        entity.setLineNumber(issue.getLineNumber());
        entity.setMessage(issue.getMessage());
        entity.setSuggestion(issue.getSuggestion());
        return entity;
    }

    private QualityCheckResponse response(QualityReportEntity report, List<QualityIssue> issues) {
        QualityCheckResponse response = new QualityCheckResponse();
        response.setReportId(report.getId());
        response.setDocumentId(report.getDocumentId());
        response.setDocumentVersionId(report.getDocumentVersionId());
        response.setScore(report.getScore());
        response.setCreatedAt(report.getCreatedAt());
        response.setIssues(issues);
        return response;
    }
}
