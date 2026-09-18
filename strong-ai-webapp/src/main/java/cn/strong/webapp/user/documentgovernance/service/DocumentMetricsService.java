package cn.strong.webapp.user.documentgovernance.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class DocumentMetricsService {

    private final MeterRegistry registry;

    public DocumentMetricsService(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordAiTask(String documentType, String provider, String model, Long promptTokens,
                             Long completionTokens, Long totalTokens, long latencyMillis, String outcome) {
        String type = safe(documentType, "UNKNOWN");
        String providerValue = safe(provider, "UNKNOWN");
        String modelValue = safe(model, "UNKNOWN");
        String status = safe(outcome, "UNKNOWN");
        Counter.builder("document_governance_ai_task_total")
                .tags("documentType", type, "provider", providerValue, "model", modelValue, "outcome", status)
                .register(registry).increment();
        DistributionSummary.builder("document_governance_ai_task_tokens")
                .tags("documentType", type, "provider", providerValue, "model", modelValue)
                .register(registry).record(totalTokens == null ? 0D : totalTokens.doubleValue());
        Timer.builder("document_governance_ai_task_latency")
                .tags("documentType", type, "provider", providerValue, "model", modelValue, "outcome", status)
                .register(registry).record(Math.max(0L, latencyMillis), TimeUnit.MILLISECONDS);
    }

    public void recordQualityCheck(String documentType, String ruleCode) {
        Counter.builder("document_governance_quality_issue_total")
                .tags("documentType", safe(documentType, "UNKNOWN"), "ruleCode", safe(ruleCode, "UNKNOWN"))
                .register(registry).increment();
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
