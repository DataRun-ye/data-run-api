package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.mongo.domain.AssignmentSubmissionHistory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;

public class AssignmentSubmissionHistoryRepositoryCustomImpl implements AssignmentSubmissionHistoryRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public AssignmentSubmissionHistoryRepositoryCustomImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<AssignmentSubmissionHistory> findByUidAndTeam(String uid, String team) {
        Query query = new Query(Criteria.where("uid").is(uid)
            .and("entries.submissionTeam").is(team));
        return mongoTemplate.find(query, AssignmentSubmissionHistory.class);
    }

    @Override
    public Optional<AssignmentSubmissionHistory> findLastEntryByUid(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid))
            .with(Sort.by(Sort.Direction.DESC, "entries.entryDate"))
            .limit(1);
        return Optional.ofNullable(mongoTemplate.findOne(query, AssignmentSubmissionHistory.class));
    }

    @Override
    public Optional<AssignmentSubmissionHistory> findLastEntryByUidAndAssignedTeam(String uid, String assignedTeam) {
        Query query = new Query(Criteria.where("uid").is(uid)
            .and("entries.assignedTeam").is(assignedTeam))
            .with(Sort.by(Sort.Direction.DESC, "entries.entryDate"))
            .limit(1);
        return Optional.ofNullable(mongoTemplate.findOne(query, AssignmentSubmissionHistory.class));
    }

    @Override
    public Optional<AssignmentSubmissionHistory> findLastEntryByUidAndTeam(String uid, String team) {
        Query query = new Query(Criteria.where("uid").is(uid)
            .and("entries.submissionTeam").is(team))
            .with(Sort.by(Sort.Direction.DESC, "entries.entryDate"))
            .limit(1);
        return Optional.ofNullable(mongoTemplate.findOne(query, AssignmentSubmissionHistory.class));
    }

    @Override
    public Optional<AssignmentSubmissionHistory> findLastEntryByUidAndUser(String uid, String login) {
        Query query = new Query(Criteria.where("uid").is(uid)
            .and("entries.submissionUser").is(login))
            .with(Sort.by(Sort.Direction.DESC, "entries.entryDate"))
            .limit(1);
        return Optional.ofNullable(mongoTemplate.findOne(query, AssignmentSubmissionHistory.class));
    }
}
