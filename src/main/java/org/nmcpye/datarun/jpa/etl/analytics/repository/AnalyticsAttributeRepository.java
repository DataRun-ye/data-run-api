package org.nmcpye.datarun.jpa.etl.analytics.repository;

import org.nmcpye.datarun.jpa.common.BaseJpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.etl.analytics.domain.AnalyticsAttribute;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Hamza Assada
 * @since 11/09/2025
 */
@Repository
public interface AnalyticsAttributeRepository extends BaseJpaIdentifiableRepository<AnalyticsAttribute, Long> {
    /**
     * Finds all analytics attributes for a specific version of a data template.
     * This is the primary method for fetching metadata for the API.
     *
     * @param templateVersionUid The unique identifier of the template version.
     * @return a List of analytics attributes.
     */
    List<AnalyticsAttribute> findByTemplateVersionUid(String templateVersionUid);

    /**
     * Deletes all analytics attributes associated with a specific template version.
     * This is used to ensure clean regeneration of metadata.
     *
     * @param templateVersionUid The unique identifier of the template version.
     * @return the number of attributes deleted.
     */
    long deleteAllByTemplateVersionUid(String templateVersionUid);
}
