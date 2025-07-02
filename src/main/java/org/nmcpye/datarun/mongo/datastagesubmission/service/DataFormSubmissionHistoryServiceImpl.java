package org.nmcpye.datarun.mongo.datastagesubmission.service;

import org.nmcpye.datarun.mongo.common.DefaultMongoIdentifiableObjectService;
import org.nmcpye.datarun.mongo.datastagesubmission.mapper.DataFormSubmissionHistoryMapper;
import org.nmcpye.datarun.mongo.datastagesubmission.repository.DataFormSubmissionHistoryRepository;
import org.nmcpye.datarun.mongo.datastagesubmission.repository.DataFormSubmissionRepository;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.domain.DataFormSubmissionHistory;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DataFormSubmissionHistoryServiceImpl
    extends DefaultMongoIdentifiableObjectService<DataFormSubmissionHistory>
    implements DataFormSubmissionHistoryService {
    final DataFormSubmissionHistoryRepository repository;
    final DataFormSubmissionRepository submissionRepository;
    private final DataFormSubmissionHistoryMapper historyMapper;
    private final MongoTemplate mongoTemplate;
    private static final int MAX_HISTORY_VERSIONS = 5; // Keep only the last 10 versions

    public DataFormSubmissionHistoryServiceImpl(DataFormSubmissionHistoryRepository repository,
                                                CacheManager cacheManager,
                                                DataFormSubmissionRepository submissionRepository,
                                                DataFormSubmissionHistoryMapper historyMapper,
                                                MongoTemplate mongoTemplate) {
        super(repository, cacheManager);
        this.repository = repository;
        this.submissionRepository = submissionRepository;
        this.historyMapper = historyMapper;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<DataFormSubmissionHistory> getSubmissionVersions(String submissionUid) {
        Query query = new Query(Criteria.where("uid").is(submissionUid));
        List<DataFormSubmissionHistory> submissions = mongoTemplate.find(query, DataFormSubmissionHistory.class);

        return submissions;
    }

    @Override
    public void saveToHistory(DataFormSubmission submission) {
        DataFormSubmissionHistory history = historyMapper.fromSubmission(submission);

        repository.save(history);

        pruneOldVersions(submission.getUid());
    }

    @Override
    public void pruneOldVersions(String submissionUid) {
        Query pruneQuery = Query.query(Criteria.where("uid").is(submissionUid))
            .with(Sort.by(Sort.Direction.DESC, "timestamp"))
            .skip(MAX_HISTORY_VERSIONS);
        pruneQuery.fields().include("_id");

        List<String> idsToRemove = mongoTemplate
            .find(pruneQuery, DataFormSubmissionHistory.class)
            .stream()
            .map(DataFormSubmissionHistory::getId)
            .collect(Collectors.toList());

        if (!idsToRemove.isEmpty()) {
            mongoTemplate.remove(
                Query.query(Criteria.where("_id").in(idsToRemove)),
                DataFormSubmissionHistory.class
            );
        }
    }
}
