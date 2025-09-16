package org.nmcpye.datarun.jpa.analytics.event;

/**
 * @author Hamza Assada
 * @since 12/09/2025
 */

/**
 * This event is published when a DataTemplateVersion becomes active.
 */
public record NewTemplateVersionPublishedEvent(String templateVersionUid) {
}
