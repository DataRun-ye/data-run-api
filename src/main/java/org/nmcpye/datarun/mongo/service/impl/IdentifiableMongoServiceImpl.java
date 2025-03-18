package org.nmcpye.datarun.mongo.service.impl;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.drun.postgres.common.IdentifiableEntity;
import org.nmcpye.datarun.mongo.repository.IdentifiableMongoRepository;
import org.nmcpye.datarun.mongo.service.IdentifiableMongoService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.MongoQueryBuilder;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;

public abstract class IdentifiableMongoServiceImpl<T extends IdentifiableEntity<String>>
    implements IdentifiableMongoService<T> {
    private final Logger log = LoggerFactory.getLogger(IdentifiableMongoServiceImpl.class);

    protected final IdentifiableMongoRepository<T> repository;
    final protected MongoTemplate mongoTemplate;
    private final Class<T> entityType;

    @SuppressWarnings("unchecked")
    public IdentifiableMongoServiceImpl(IdentifiableMongoRepository<T> repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;

        this.entityType = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public Class<T> getClazz() {
        return entityType;
    }

    @Override
    public boolean existsByUid(String uid) {
        return repository.findByUid(uid).isPresent();
    }

    @Override
    public boolean existsById(String id) {
        return repository.findById(id).isPresent();
    }

    @Override
    public Optional<T> findByUid(String uid) {
        return repository.findByUid(uid);
    }

    @Override
    public void deleteByUid(String uid) {
        repository.findByUid(uid).ifPresent(repository::delete);
    }

    @Override
    public T save(T object) {
        return saveWithRelations(object);
    }

    @Override
    public T saveWithRelations(T object) {
        return repository.save(object);
    }

    @Override
    public T update(T object) {
        T existingEntity = repository
            .findByUid(object.getUid())
            .orElseThrow(() ->
                new IllegalQueryException(new ErrorMessage(ErrorCode.E1106, "Id", object.getUid())));

        log.debug("Request to update T : {}", object);
        object.setId(existingEntity.getId());
        return saveWithRelations(object);
    }

    @Override
    public Page<T> findAll(Pageable pageable, QueryRequest queryRequest) {
        Query query = new Query();

        List<T> submissions = getFormSubmissions(query, queryRequest);
        long total = submissions.size();

        return new PageImpl<>(submissions, pageable, total);
    }

    @Override
    public List<T> findAll(QueryRequest queryRequest) {
        Query query = new Query();

        List<T> submissions = getFormSubmissions(query, queryRequest);

        return submissions;
    }

    @Override
    public Page<T> findAllByUser(Pageable pageable, QueryRequest queryRequest) {
        Page<T> submissions = query(queryRequest);
        return submissions;
    }

    @Override
    public List<T> findAllByUser(QueryRequest queryRequest) {
        log.debug("Request to get all MEntities");
        Query query = new Query();
        var allEntities = getFormSubmissions(query, queryRequest);
        return allEntities;
    }

    public Page<T> query(QueryRequest request) {
        Query query = MongoQueryBuilder.buildQuery(request.parseFilters());
        query = MongoQueryBuilder.addProjections(query, request.getFields());

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        query.with(pageable);

        final Query totalQuery = Query.of(query).limit(-1).skip(-1);

        List<T> results = mongoTemplate.find(query, getClazz());

        Page<T> resultsPage = PageableExecutionUtils.getPage(
            results,
            pageable,
            () -> mongoTemplate.count(totalQuery, getClazz()));


        return resultsPage;
    }

    protected List<T> getFormSubmissions(Query query, QueryRequest queryRequest) {
        applySecurityConstraints(query);
        if (!queryRequest.isIncludeDeleted()) {
            query.addCriteria(Criteria.where("deleted").is(false));
        }
        return mongoTemplate.find(query, getClazz());
    }

    private static void applySecurityConstraints(Query query) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            // No additional constraints for admin
            return;
        }

        String currentUserLogin = SecurityUtils.getCurrentUserLoginOrThrow();
        String login = currentUserLogin;
        query.addCriteria(Criteria.where("createdBy").is(login));
    }

    @Override
    public Optional<T> findOne(String id) {
        log.debug("Request to get T : {}", id);
        return repository.findById(id);
    }

    @Override
    public void delete(String id) {
        log.debug("Request to delete T : {}", id);
        repository.deleteById(id);
    }
}
