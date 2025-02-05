//package org.nmcpye.datarun.mongo.service.impl;
//
//import org.nmcpye.datarun.mongo.domain.DataForm;
//import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
//import org.nmcpye.datarun.security.AuthoritiesConstants;
//import org.nmcpye.datarun.security.SecurityUtils;
//import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.context.annotation.Primary;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.data.mongodb.core.query.Criteria;
//import org.springframework.data.mongodb.core.query.Query;
//import org.springframework.data.support.PageableExecutionUtils;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//
///**
// * Service Implementation for managing {@link DataForm}.
// */
//@Service
//@Primary
//@Transactional
//public class DataElementServiceImpl
//    extends IdentifiableMongoServiceImpl<DataElement>
//    implements DataElementService {
//
//    private final Logger log = LoggerFactory.getLogger(DataElementServiceImpl.class);
//
//    private final DataElementRepository repository;
//    private final OptionSetRepository optionSetRepository;
//    private final UserAccessService userAccessService;
//
//    public DataElementServiceImpl(DataElementRepository repository,
//                                  MongoTemplate mongoTemplate,
//                                  OptionSetRepository optionSetRepository,
//                                  UserAccessService userAccessService) {
//        super(repository, mongoTemplate);
//        this.repository = repository;
//        this.optionSetRepository = optionSetRepository;
//        this.userAccessService = userAccessService;
//    }
//
//
//    @Override
//    public DataElement saveWithRelations(DataElement element) {
//        if (element.getType().isOptionsType()) {
//            optionSetRepository.findByUid(element.getOptionSet()).orElseThrow();
//        }
//        return repository.save(element);
//    }
//
//    @Override
//    public Page<DataElement> findAllByUser(Pageable pageable, QueryRequest queryRequest) {
//
//        if (!SecurityUtils.isAuthenticated()) {
//            return Page.empty(pageable);
//        }
//
//        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
//            return repository.findAll(pageable);
//        }
//
//        List<String> userDataElements = userAccessService.getUserFormsWithWritePermission(SecurityUtils.getCurrentUserLogin().get())
//            .stream()
//            .flatMap((form) -> form.getFields().stream())
//            .map(FormDataElementConf::getId).toList();
//
//        Query query = new Query();
//        query = query.addCriteria(Criteria.where("uid").in(userDataElements));
//
//        query.with(pageable);
//
//        final Query totalQuery = Query.of(query).limit(-1).skip(-1);
//
//        List<DataElement> results = mongoTemplate.find(query, DataElement.class);
//
//        Page<DataElement> resultsPage = PageableExecutionUtils.getPage(
//            results,
//            pageable,
//            () -> mongoTemplate.count(totalQuery, DataElement.class));
//
//        return resultsPage;
//    }
//}
