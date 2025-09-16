package org.nmcpye.datarun.jpa.analytics.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.analytics.service.AnalyticsMetadataGenerator;
import org.nmcpye.datarun.jpa.analytics.service.AnalyticsMetadataServiceNew;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @author Hamza Assada
 * @since 12/09/2025
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TemplateVersionListener {
    private final AnalyticsMetadataGenerator metadataGenerator;
    private final AnalyticsMetadataServiceNew metadataService; // Add this

    /**
     * Asynchronously handles the event when a new template version is published.
     * The @Async annotation ensures this runs in a background thread, so it
     * doesn't block the original user request.
     *
     * @param event The event containing the UID of the newly published template version.
     */
    @Async
    @EventListener
    public void handleNewTemplateVersionPublished(NewTemplateVersionPublishedEvent event) {
        try {
            log.info("Received NewTemplateVersionPublishedEvent for: {}", event.templateVersionUid());
            metadataGenerator.generateAndSaveAttributes(event.templateVersionUid());
            // Evict the cache after generation is successful
            metadataService.clearMetadataCache();
        } catch (Exception e) {
            // It's critical to catch exceptions in async methods to prevent silent failures.
            log.error("Failed to generate analytics metadata for template version: {}", event.templateVersionUid(), e);
        }
    }
}
