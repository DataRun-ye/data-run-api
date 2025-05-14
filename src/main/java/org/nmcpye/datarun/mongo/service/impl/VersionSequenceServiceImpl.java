package org.nmcpye.datarun.mongo.service.impl;

import org.nmcpye.datarun.mongo.domain.dataform.FormTemplate;
import org.nmcpye.datarun.mongo.service.VersionSequenceService;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * @author Hamza Assada, <7amza.it@gmail.com> <13-05-2025>
 */
@Service
public class VersionSequenceServiceImpl implements VersionSequenceService {
    private final MongoTemplate mongoTemplate;

    public VersionSequenceServiceImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public FormTemplate incrementAndGet(String templateUid) {
        Query q = Query.query(Criteria.where("uid").is(templateUid));
        Update u = new Update().inc("latestVersion", 1)
            .setOnInsert("uid", templateUid)
            .setOnInsert("disabled", false)    // default on first insert
            .setOnInsert("deleted", false);    // default on first insert
        FindAndModifyOptions opts = FindAndModifyOptions.options()
            .returnNew(true)
            .upsert(true);
        FormTemplate updated = mongoTemplate.findAndModify(q, u, opts, FormTemplate.class);
        return updated;
    }
}
