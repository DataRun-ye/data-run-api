package org.nmcpye.datarun.drun.mongo.service.impl;

import org.nmcpye.datarun.drun.mongo.domain.Counter;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class SequenceGeneratorService {

    private final MongoOperations mongoOperations;

    public SequenceGeneratorService(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    public long getNextSequence(String seqName) {
        Query query = new Query(Criteria.where("_id").is(seqName));
        Update update = new Update().inc("sequenceValue", 1);

        // Find and update the counter, return the new sequence value
        Counter counter = mongoOperations.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true).upsert(true), Counter.class);

        return counter != null ? counter.getSequenceValue() : 1;
    }
}
