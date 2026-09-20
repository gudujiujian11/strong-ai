package cn.strong.webapp.user.documentgovernance.service;

import cn.strong.webapp.user.documentgovernance.context.DocumentIdentityResolver;
import cn.strong.webapp.user.documentgovernance.entity.DocumentEntity;
import cn.strong.webapp.user.documentgovernance.entity.QualityIssueEntity;
import cn.strong.webapp.user.documentgovernance.entity.QualityReportEntity;
import cn.strong.webapp.user.documentgovernance.mapper.QualityIssueMapper;
import cn.strong.webapp.user.documentgovernance.mapper.QualityReportMapper;
import cn.strong.webapp.user.model.documentgovernance.QualityCheckResponse;
import cn.strong.webapp.user.model.documentgovernance.QualityIssue;
import cn.strong.webapp.user.model.documentgovernance.QualitySeverity;
import cn.strong.webapp.user.model.documentgovernance.RuleConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class QualityReportService {

    private final DocumentService documentService;
    private final TemplateService templateService;
    private final TermService termService;
    private final RuleSetService ruleSetService;
    private final MarkdownQualityChecker checker;
    private final QualityReportMapper reportMapper;
    private final QualityIssueMapper issueMapper;
    private final DocumentIdentityResolver identityResolver;

    @Autowired(required = false)
    private DocumentOperationLogService operationLogService;

    @Autowired(required = false)
    private DocumentMetricsService metricsService;

    public QualityReportService(DocumentService documentService, TemplateService templateService,
                                TermService termService, RuleSetService ruleSetService,
                                MarkdownQualityChecker checker,
                                QualityReportMapper reportMapper, QualityIssueMapper issueMapper,
                                DocumentIdentityResolver identityResolver) {
        this.documentService = documentService;
        this.templateService = templateService;
        this.termService = termService;
        this.ruleSetService = ruleSetService;
        this.checker = checker;
        this.reportMapper = reportMapper;
        this.issueMapper = issueMapper;
        this.identityResolver = identityResolver;
    }

    @Transactional(rollbackFor = Exception.class)
    public QualityCheckResponse checkAndSave(Long documentId) {
        DocumentEntity document = documentService.requireReadable(documentId);
        RuleConfig ruleConfig = ruleSetService.effectiveConfig(document.getTeamId());
        List<QualityIssue> issues = checker.check(document.getDraftContent(),
                templateService.requiredHeadings(document.getTeamId(), document.getDocumentType()),
                termService.list(document.getTeamId()), ruleConfig);
        QualityReportEntity report = new QualityReportEntity();
        report.setTeamId(document.getTeamId());
        report.setDocumentId(document.getId());
        report.setDocumentVersionId(document.getCurrentVersionId());
        report.setScore(score(issues, ruleConfig));
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

    /** 查询文档最近一次质量报告（含问题明细）；从未检查返回 null。 */
    public QualityCheckResponse latestReport(Long documentId) {
        DocumentEntity document = documentService.requireReadable(documentId);
        QualityReportEntity report = reportMapper.selectLatestByDocument(document.getTeamId(), document.getId());
        if (report == null) {
            return null;
        }
        return response(report, loadIssues(document.getTeamId(), report.getId()));
    }

    /** 查询文档历史质量报告摘要（不含问题明细）。 */
    public List<QualityReportEntity> listReports(Long documentId) {
        DocumentEntity document = documentService.requireReadable(documentId);
        List<QualityReportEntity> reports = reportMapper.selectByDocument(document.getTeamId(), document.getId());
        return reports == null ? Collections.emptyList() : reports;
    }

    private List<QualityIssue> loadIssues(String teamId, Long reportId) {
        List<QualityIssueEntity> entities = issueMapper.selectByReportId(teamId, reportId);
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        List<QualityIssue> issues = new ArrayList<>(entities.size());
        for (QualityIssueEntity entity : entities) {
            QualityIssue issue = new QualityIssue();
            issue.setRuleCode(entity.getRuleCode());
            issue.setSeverity(parseSeverity(entity.getSeverity()));
            issue.setLineNumber(entity.getLineNumber() == null ? 0 : entity.getLineNumber());
            issue.setMessage(entity.getMessage());
            issue.setSuggestion(entity.getSuggestion());
            issues.add(issue);
        }
        return issues;
    }

    private QualitySeverity parseSeverity(String value) {
        if (value == null) {
            return QualitySeverity.LOW;
        }
        try {
            return QualitySeverity.valueOf(value);
        } catch (IllegalArgumentException e) {
            return QualitySeverity.LOW;
        }
    }

    private int score(List<QualityIssue> issues, RuleConfig ruleConfig) {
        int deductions = 0;
        for (QualityIssue issue : issues) {
            deductions += ruleConfig.weight(issue.getSeverity());
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
