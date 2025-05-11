package org.nmcpye.datarun.datasubmission;

import org.nmcpye.datarun.common.mongo.impl.DefaultMongoAuditableObjectService;
import org.nmcpye.datarun.mongo.domain.DataFormSubmissionBu;
import org.nmcpye.datarun.mongo.repository.DataFormSubmissionBuRepository;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
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
        CacheManager cacheManager, MongoTemplate mongoTemplate) {
        super(repository, cacheManager, mongoTemplate);
    }
}
