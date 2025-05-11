package org.nmcpye.datarun.datasubmission;

import org.nmcpye.datarun.common.mongo.impl.DefaultMongoAuditableObjectService;
import org.nmcpye.datarun.mongo.domain.DataFormSubmissionHistory;
import org.nmcpye.datarun.mongo.repository.DataFormSubmissionHistoryRepository;
import org.nmcpye.datarun.mongo.repository.DataFormSubmissionRepository;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataFormSubmissionHistoryServiceImpl
    extends DefaultMongoAuditableObjectService<DataFormSubmissionHistory>
    implements DataFormSubmissionHistoryService {
    final DataFormSubmissionHistoryRepository repository;
    final DataFormSubmissionRepository submissionRepository;

    public DataFormSubmissionHistoryServiceImpl(DataFormSubmissionHistoryRepository repository, CacheManager cacheManager,
                                                DataFormSubmissionRepository submissionRepository,
                                                MongoTemplate mongoTemplate) {
        super(repository, cacheManager, mongoTemplate);
        this.repository = repository;
        this.submissionRepository = submissionRepository;
    }

    @Override
    public List<DataFormSubmissionHistory> getSubmissionVersions(String dataSubmissionId) {
        Query query = new Query(Criteria.where("dataSubmissionId").is(dataSubmissionId));
        List<DataFormSubmissionHistory> submissions = mongoTemplate.find(query, DataFormSubmissionHistory.class);

        return submissions;
    }

//    @Override
//    public Page<DataFormSubmissionHistory> findAllByForm(List<String> forms, Pageable pageable, boolean includeDeleted) {
//        Query query = new Query(Criteria.where("form").in(forms));
//        query.with(pageable);
//        List<DataFormSubmissionHistory> submissions = getFormSubmissions(query, includeDeleted);
//        long total = submissions.size();
//
//        return new PageImpl<>(submissions, pageable, total);
//    }

}
