package com.tripplanner.guardrails;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@ApplicationScoped
public class GuardrailAuditLog {

    public record AuditEntry(Instant timestamp, String guardrail, String decision, String reason) {}

    private static final int MAX_ENTRIES = 100;

    private final Deque<AuditEntry> entries = new ConcurrentLinkedDeque<>();

    @Inject
    Logger logger;

    public void log(String guardrail, String decision, String reason) {
        AuditEntry entry = new AuditEntry(Instant.now(), guardrail, decision, reason);
        entries.addLast(entry);
        while (entries.size() > MAX_ENTRIES) {
            entries.pollFirst();
        }
        logger.infof("🛡️ [%s] %s — %s", guardrail, decision, reason);
    }

    public List<AuditEntry> getRecentEntries() {
        return List.copyOf(entries);
    }
}
