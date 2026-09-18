package cn.strong.webapp.user.documentgovernance.service;

import cn.strong.webapp.user.documentgovernance.mapper.DocumentMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DocumentRetentionCleanupJob {

    private static final long DRAFT_RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000;

    private final DocumentMapper documentMapper;
    private final DocumentAiTaskService taskService;

    public DocumentRetentionCleanupJob(DocumentMapper documentMapper, DocumentAiTaskService taskService) {
        this.documentMapper = documentMapper;
        this.taskService = taskService;
    }

    @Scheduled(cron = "0 30 3 * * ?")
    public int cleanupExpiredData() {
        int documents = documentMapper.deleteExpiredDrafts(System.currentTimeMillis() - DRAFT_RETENTION_MILLIS);
        return documents + taskService.cleanupExpiredTasks();
    }
}
