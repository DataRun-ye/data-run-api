package org.nmcpye.datarun.mongo.service.impl;

import org.nmcpye.datarun.mongo.domain.DataForm;
import org.nmcpye.datarun.mongo.domain.OptionSet;
import org.nmcpye.datarun.mongo.domain.dataelement.DataElement;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.domain.enumeration.ValueType;
import org.nmcpye.datarun.mongo.repository.DataFormTemplateRepository;
import org.nmcpye.datarun.mongo.repository.OptionSetRepository;
import org.nmcpye.datarun.mongo.service.OptionSetService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service Implementation for managing {@link DataForm}.
 */
@Service
@Primary
@Transactional
public class OptionSetServiceImpl
    extends IdentifiableMongoServiceImpl<OptionSet>
    implements OptionSetService {

    private final Logger log = LoggerFactory.getLogger(OptionSetServiceImpl.class);

    private final OptionSetRepository repository;
    private final DataFormTemplateRepository formTemplateRepository;

    public OptionSetServiceImpl(OptionSetRepository repository,
                                MongoTemplate mongoTemplate,
                                DataFormTemplateRepository formTemplateRepository) {
        super(repository, mongoTemplate);
        this.repository = repository;
        this.formTemplateRepository = formTemplateRepository;
    }

    @Override
    public Page<OptionSet> findAllByUser(Pageable pageable, QueryRequest queryRequest) {

        if (!SecurityUtils.isAuthenticated()) {
            return Page.empty(pageable);
        }

        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repository.findAll(pageable);
        }

        List<String> userOptionSets = formTemplateRepository.findByFieldType(List.of(ValueType.SelectMulti, ValueType.SelectOne))
            .stream()
            .flatMap((form) -> form.getFields().stream())
            .filter(de -> de.getType().isOptionsType())
            .map(FormDataElementConf::getOptionSet).toList();

        Query query = new Query();
        query = query.addCriteria(Criteria.where("uid").in(userOptionSets));

        query.with(pageable);

        final Query totalQuery = Query.of(query).limit(-1).skip(-1);

        List<OptionSet> results = mongoTemplate.find(query, OptionSet.class);

        Page<OptionSet> resultsPage = PageableExecutionUtils.getPage(
            results,
            pageable,
            () -> mongoTemplate.count(totalQuery, DataElement.class));

        return resultsPage;
    }
}
