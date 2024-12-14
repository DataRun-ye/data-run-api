package org.nmcpye.datarun.web.rest.mongo.submission;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GenericQueryService {

    private final MongoTemplate mongoTemplate;

    public GenericQueryService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public <T> Page<T> query(QueryRequest request, Class<T> entityClass) {
        Query query = MongoQueryBuilder.buildQuery(request.parseFilters());
        query = MongoQueryBuilder.addProjections(query, request.getFields());

        if (!request.isIncludeDeleted()) {
            query.addCriteria(Criteria.where("deleted").is(false));
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        query.with(pageable);

        final Query totalQuery = Query.of(query).limit(-1).skip(-1);

        List<T> results = mongoTemplate.find(query, entityClass);

        Page<T> resultsPage = PageableExecutionUtils.getPage(
            results,
            pageable,
            () -> mongoTemplate.count(totalQuery, entityClass));


        return resultsPage;
    }
}
