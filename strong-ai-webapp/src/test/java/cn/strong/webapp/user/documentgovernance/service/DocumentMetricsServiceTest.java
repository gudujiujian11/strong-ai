package cn.strong.webapp.user.documentgovernance.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentMetricsServiceTest {

    @Test
    void recordsTokenAndLatencyWithoutDocumentContent() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        DocumentMetricsService metrics = new DocumentMetricsService(registry);

        metrics.recordAiTask("README", "openai", "gpt-5.6-sol", 123L, 456L, 789L, 1200L, "SUCCEEDED");

        assertEquals(1D, registry.find("document_governance_ai_task_total").counter().count());
        assertEquals(789D, registry.find("document_governance_ai_task_tokens").summary().totalAmount());
    }
}
