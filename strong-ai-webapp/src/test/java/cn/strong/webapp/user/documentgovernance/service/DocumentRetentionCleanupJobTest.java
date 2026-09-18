package cn.strong.webapp.user.documentgovernance.service;

import cn.strong.webapp.user.documentgovernance.mapper.DocumentMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentRetentionCleanupJobTest {

    @Test
    void cleanupDeletesExpiredDraftsAndTerminalTasksButNeverPublishedDocumentsOrVersions() {
        DocumentMapper documentMapper = mock(DocumentMapper.class);
        DocumentAiTaskService taskService = mock(DocumentAiTaskService.class);
        when(documentMapper.deleteExpiredDrafts(anyLong())).thenReturn(2);
        when(taskService.cleanupExpiredTasks()).thenReturn(3);
        DocumentRetentionCleanupJob job = new DocumentRetentionCleanupJob(documentMapper, taskService);

        int deleted = job.cleanupExpiredData();

        assertEquals(5, deleted);
        verify(documentMapper).deleteExpiredDrafts(anyLong());
        verify(taskService).cleanupExpiredTasks();
    }
}
