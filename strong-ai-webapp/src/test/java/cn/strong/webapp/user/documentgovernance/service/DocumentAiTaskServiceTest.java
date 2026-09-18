package cn.strong.webapp.user.documentgovernance.service;

import cn.strong.webapp.user.documentgovernance.context.DocumentIdentity;
import cn.strong.webapp.user.documentgovernance.context.DocumentIdentityResolver;
import cn.strong.webapp.user.documentgovernance.entity.AiTaskEntity;
import cn.strong.webapp.user.documentgovernance.entity.DocumentEntity;
import cn.strong.webapp.user.documentgovernance.mapper.AiTaskMapper;
import cn.strong.webapp.user.mdoptimizer.service.MarkdownOptimizerService;
import cn.strong.webapp.user.model.documentgovernance.AiOptimizeRequest;
import cn.strong.webapp.user.model.documentgovernance.AiTaskResponse;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentAiTaskServiceTest {

    private DocumentService documentService;
    private AiTaskMapper taskMapper;
    private MarkdownOptimizerService optimizer;
    private DocumentAiTaskService service;
    private final AtomicReference<AiTaskEntity> storedTask = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        documentService = mock(DocumentService.class);
        taskMapper = mock(AiTaskMapper.class);
        optimizer = mock(MarkdownOptimizerService.class);
        TaskExecutor executor = command -> { };
        service = new DocumentAiTaskService(documentService, taskMapper, optimizer, executor,
                new DocumentIdentityResolver());
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
    void retryReusesIdempotencyKeyAndNeverCreatesDuplicateTask() {
        DocumentEntity document = document();
        when(documentService.requireWritableDraft(1L)).thenReturn(document);
        when(taskMapper.findByIdempotency("team-a", 1L, "request-7"))
                .thenAnswer(invocation -> storedTask.get());
        doAnswer(invocation -> {
            AiTaskEntity task = invocation.getArgument(0);
            task.setId(7L);
            storedTask.set(task);
            return 1;
        }).when(taskMapper).insert(any(AiTaskEntity.class));

        AiTaskResponse first = service.submit(1L, request("request-7"));
        AiTaskResponse retry = service.submit(1L, request("request-7"));

        assertEquals(first.getId(), retry.getId());
        verify(taskMapper).insert(any(AiTaskEntity.class));
    }

    @Test
    void failedAiTaskPreservesDraftAndCanRetry() {
        AiTaskEntity task = new AiTaskEntity();
        task.setId(7L);
        task.setTeamId("team-a");
        task.setDocumentId(1L);
        task.setStatus("PENDING");
        task.setProvider("openai");
        task.setModel("gpt-5.6-sol");
        task.setExpiresAt(System.currentTimeMillis() + 60000L);
        when(taskMapper.selectById(7L)).thenReturn(task);
        when(taskMapper.selectByTeamAndId("team-a", 7L)).thenReturn(task);
        when(documentService.findByTeam(1L, "team-a")).thenReturn(document());
        when(optimizer.optimize(any())).thenThrow(new IllegalStateException("provider body must stay private"));

        service.process(7L);

        assertEquals("FAILED", service.get(7L).getStatus());
        assertEquals("MODEL_UNAVAILABLE", service.get(7L).getErrorCode());
        verify(documentService, never()).applyAiDraft(any(Long.class), any(String.class), any(String.class));
    }

    private DocumentEntity document() {
        DocumentEntity document = new DocumentEntity();
        document.setId(1L);
        document.setTeamId("team-a");
        document.setDocumentType("README");
        document.setDraftContent("# original");
        return document;
    }

    private AiOptimizeRequest request(String key) {
        AiOptimizeRequest request = new AiOptimizeRequest();
        request.setIdempotencyKey(key);
        request.setProvider("openai");
        request.setModel("gpt-5.6-sol");
        return request;
    }
}
