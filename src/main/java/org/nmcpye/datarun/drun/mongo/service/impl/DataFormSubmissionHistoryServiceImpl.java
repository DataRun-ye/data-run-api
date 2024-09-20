package org.nmcpye.datarun.drun.mongo.service.impl;

import org.nmcpye.datarun.drun.mongo.domain.DataFormSubmissionHistory;
import org.nmcpye.datarun.drun.mongo.repository.DataFormSubmissionHistoryRepository;
import org.nmcpye.datarun.drun.mongo.repository.DataFormSubmissionRepositoryCustom;
import org.nmcpye.datarun.drun.mongo.service.DataFormSubmissionHistoryService;
import org.nmcpye.datarun.web.rest.mongo.JsonFlattener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

public class DataFormSubmissionHistoryServiceImpl
    extends IdentifiableMongoServiceImpl<DataFormSubmissionHistory>
    implements DataFormSubmissionHistoryService {
    final DataFormSubmissionHistoryRepository repository;
    final DataFormSubmissionRepositoryCustom submissionRepository;
    final private MongoTemplate mongoTemplate;

    public DataFormSubmissionHistoryServiceImpl(DataFormSubmissionHistoryRepository repository, DataFormSubmissionRepositoryCustom submissionRepository, MongoTemplate mongoTemplate) {
        super(repository);
        this.repository = repository;
        this.submissionRepository = submissionRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<DataFormSubmissionHistory> getSubmissionVersions(String dataSubmissionId) {
        Query query = new Query(Criteria.where("dataSubmissionId").is(dataSubmissionId));
        List<DataFormSubmissionHistory> submissions = mongoTemplate.find(query, DataFormSubmissionHistory.class);

        return submissions;
    }

    @Override
    public Page<DataFormSubmissionHistory> findAllByForm(List<String> forms, Pageable pageable, boolean flatten) {
        Query query = new Query(Criteria.where("form").in(forms));
        List<DataFormSubmissionHistory> submissions = mongoTemplate.find(query, DataFormSubmissionHistory.class);

        if (flatten) {
            List<DataFormSubmissionHistory> flattenSubmissions = flattenSubmissions(submissions, true);
            return getDataFormSubmissions(pageable, flattenSubmissions);
        }

        return getDataFormSubmissions(pageable, submissions);
    }

    private List<DataFormSubmissionHistory> flattenSubmissions(List<DataFormSubmissionHistory> submissions, boolean includingArrays) {
        return submissions
            .stream()
            .peek(submission -> {
                if (includingArrays) {
                    submission.setFormData(JsonFlattener.flattenIncludingArrays(submission.getFormData()));
                } else {
                    submission.setFormData(JsonFlattener.flatten(submission.getFormData()));
                }

            })
            .toList();
    }


    private static Page<DataFormSubmissionHistory> getDataFormSubmissions(Pageable pageable, List<DataFormSubmissionHistory> submissions) {
        if (!pageable.isPaged()) {
            return new PageImpl<>(submissions);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), submissions.size());
        if (start > end) {
            return Page.empty(pageable);
        }

        List<DataFormSubmissionHistory> sublist = submissions.subList(start, end);
        return new PageImpl<>(sublist, pageable, submissions.size());
    }
}
