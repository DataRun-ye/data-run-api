package org.nmcpye.datarun.drun.mongo.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.drun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.drun.mongo.domain.DataFormSubmissionHistory;
import org.nmcpye.datarun.drun.mongo.repository.DataFormSubmissionHistoryRepository;
import org.nmcpye.datarun.drun.mongo.repository.DataFormSubmissionRepositoryCustom;
import org.nmcpye.datarun.drun.mongo.service.DataFormSubmissionService;
import org.nmcpye.datarun.drun.postgres.repository.ActivityRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.JsonFlattener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Service Implementation for managing {@link DataFormSubmission}.
 */
@Service
@Primary
public class DataFormSubmissionServiceImpl
    extends IdentifiableMongoServiceImpl<DataFormSubmission>
    implements DataFormSubmissionService {

    private final Logger log = LoggerFactory.getLogger(DataFormSubmissionServiceImpl.class);

    private final DataFormSubmissionRepositoryCustom repository;
    final DataFormSubmissionHistoryRepository historyRepository;
    private final ActivityRelationalRepositoryCustom activityRepository;
    private final OrgUnitRelationalRepositoryCustom orgUnitRelationalRepositoryCustom;
    private final TeamRelationalRepositoryCustom teamRepository;
    final private MongoTemplate mongoTemplate;

    public DataFormSubmissionServiceImpl(
        DataFormSubmissionRepositoryCustom repository, DataFormSubmissionHistoryRepository historyRepository,
        ActivityRelationalRepositoryCustom activityRepository,
        OrgUnitRelationalRepositoryCustom orgUnitRelationalRepositoryCustom,
        TeamRelationalRepositoryCustom teamRepository, MongoTemplate mongoTemplate) {
        super(repository);
        this.repository = repository;
        this.historyRepository = historyRepository;
        this.activityRepository = activityRepository;
        this.orgUnitRelationalRepositoryCustom = orgUnitRelationalRepositoryCustom;
        this.teamRepository = teamRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public DataFormSubmission saveVersioning(DataFormSubmission submission) {
        DataFormSubmission existingSubmission = repository.findById(submission.getId()).orElse(null);
        if (existingSubmission != null) {
            // Create a new version of the current data
            DataFormSubmissionHistory history = new DataFormSubmissionHistory(
                existingSubmission,
                Instant.now()
            );

            // Save the old version in the history collection
            historyRepository.save(history);

            // Increment version number and update the current document
            submission.setVersion(existingSubmission.getVersion() + 1);
        }

        return repository.save(submission);
    }

    @Override
    public DataFormSubmission saveWithRelations(DataFormSubmission dataFormSubmission) {
        activityRepository.findByUid(dataFormSubmission.getActivity())
            .ifPresentOrElse((a) -> dataFormSubmission.setActivity(a.getUid()),
                () -> {
                    throw new PropertyNotFoundException("Activity not found: " + dataFormSubmission.getOrgUnit());
                });
        teamRepository.findByUid(dataFormSubmission.getTeam())
            .ifPresentOrElse((a) -> dataFormSubmission.setTeam(a.getUid()),
                () -> {
                    throw new PropertyNotFoundException("Team not found: " + dataFormSubmission.getOrgUnit());
                });
        orgUnitRelationalRepositoryCustom.findByUid(dataFormSubmission.getOrgUnit())
            .ifPresentOrElse((a) -> dataFormSubmission.setOrgUnit(a.getUid()),
                () -> {
                    throw new PropertyNotFoundException("OrgUnit not found: " + dataFormSubmission.getOrgUnit());
                });

        return repository.save(dataFormSubmission);
//        return saveVersioning(dataFormSubmission);
    }

    @Override
    public Page<DataFormSubmission> findAllByForm(List<String> forms, Pageable pageable) {
        Query query = new Query(Criteria.where("form").in(forms));
        List<DataFormSubmission> submissions = mongoTemplate.find(query, DataFormSubmission.class);

//        if (flatten) {
//            List<DataFormSubmission> flattenSubmissions = flattenSubmissions(submissions, false);
//            return getDataFormSubmissions(pageable, flattenSubmissions);
//        }

        return getDataFormSubmissions(pageable, submissions);
    }

    @Override
    public Page<DataFormSubmission> findAllByUser(Pageable pageable) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repository.findAll(pageable);
        }

        if (SecurityUtils.getCurrentUserLogin().isPresent()) {
            String login = SecurityUtils.getCurrentUserLogin().get();
            Query query = new Query(Criteria.where("createdBy").is(login));
            List<DataFormSubmission> submissions = mongoTemplate.find(query, DataFormSubmission.class);
            return getDataFormSubmissions(pageable, submissions);
        }
        return Page.empty(pageable);
    }

    private List<DataFormSubmission> flattenSubmissions(List<DataFormSubmission> submissions, boolean includingArrays) {
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


    private static Page<DataFormSubmission> getDataFormSubmissions(Pageable pageable, List<DataFormSubmission> submissions) {
        if (!pageable.isPaged()) {
            return new PageImpl<>(submissions);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), submissions.size());
        if (start > end) {
            return Page.empty(pageable);
        }

        List<DataFormSubmission> sublist = submissions.subList(start, end);
        return new PageImpl<>(sublist, pageable, submissions.size());
    }

    @Override
    public List<String> getTeamsAfterDate(Date createdDate) {
        Query query = new Query();
        query.addCriteria(Criteria.where("created_date").gte(createdDate));

        return mongoTemplate.findDistinct(query, "team", "data_form_submission", String.class);
    }
}
