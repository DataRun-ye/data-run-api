package org.nmcpye.datarun.mongo.datastagesubmission.service;

import org.nmcpye.datarun.mongo.common.DefaultMongoIdentifiableObjectService;
import org.nmcpye.datarun.mongo.datastagesubmission.repository.DataFormSubmissionBuRepository;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.domain.DataFormSubmissionBu;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Service Implementation for managing {@link DataFormSubmission}.
 */
@Service
@Primary
public class DataFormSubmissionBuServiceImpl
    extends DefaultMongoIdentifiableObjectService<DataFormSubmissionBu>
    implements DataFormSubmissionBuService {

    public DataFormSubmissionBuServiceImpl(
        DataFormSubmissionBuRepository repository,
        CacheManager cacheManager) {
        super(repository, cacheManager);
    }
}
