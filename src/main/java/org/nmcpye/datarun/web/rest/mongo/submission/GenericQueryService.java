package org.nmcpye.datarun.web.rest.mongo.submission;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GenericQueryService {

    private final MongoTemplate mongoTemplate;

    public GenericQueryService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public <T> Page<T> query(QueryRequest request, Class<T> entityClass) {
        Query query = MongoQueryBuilder.buildQuery(request.getFilters());
        query = MongoQueryBuilder.addProjections(query, request.getFields());

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        query.with(pageable);

        List<T> results = mongoTemplate.find(query, entityClass);
        long total = mongoTemplate.count(query, entityClass);

        return new PageImpl<>(results, pageable, total);
    }
}

//@Service
//public class GenericQueryService<T> {
//
//    final private MongoTemplate mongoTemplate;
//
//    final private Class<T> entityClass;
//
//    public GenericQueryService(MongoTemplate mongoTemplate, Class<T> entityClass) {
//        this.mongoTemplate = mongoTemplate;
//        this.entityClass = entityClass;
//    }
//
//    public Page<T> query(/*Class<T> entityClass, */QueryRequest request) {
//        Query query = MongoQueryBuilder.buildQuery(request.parseFilters());
//        query = MongoQueryBuilder.addProjections(query, request.getFields());
//
//        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
//        query.with(pageable);
//
//        List<T> results = mongoTemplate.find(query, entityClass);
//        long total = mongoTemplate.count(query, entityClass);
//
//        return new PageImpl<>(results, pageable, total);
//    }
//}
