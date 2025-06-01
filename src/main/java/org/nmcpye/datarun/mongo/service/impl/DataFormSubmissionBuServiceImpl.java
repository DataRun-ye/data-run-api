package org.nmcpye.datarun.mongo.service.impl;

import org.nmcpye.datarun.common.mongo.DefaultMongoAuditableObjectService;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.domain.DataFormSubmissionBu;
import org.nmcpye.datarun.mongo.repository.DataFormSubmissionBuRepository;
import org.nmcpye.datarun.mongo.service.DataFormSubmissionBuService;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Service Implementation for managing {@link DataFormSubmission}.
 */
@Service
@Primary
public class DataFormSubmissionBuServiceImpl
    extends DefaultMongoAuditableObjectService<DataFormSubmissionBu>
    implements DataFormSubmissionBuService {

    public DataFormSubmissionBuServiceImpl(
        DataFormSubmissionBuRepository repository,
        CacheManager cacheManager) {
        super(repository, cacheManager);
    }
}
