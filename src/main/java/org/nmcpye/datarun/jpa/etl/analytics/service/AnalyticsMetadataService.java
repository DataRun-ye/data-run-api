package org.nmcpye.datarun.jpa.etl.analytics.service;

import org.nmcpye.datarun.jpa.etl.analytics.domain.AnalyticsEntity;

import java.util.List;

/**
 * @author Hamza Assada
 * @since 07/09/2025
 */
public interface AnalyticsMetadataService {
    AnalyticsEntity getEntityByUid(String uid);

    List<AnalyticsEntity> listAllEntities();
}
