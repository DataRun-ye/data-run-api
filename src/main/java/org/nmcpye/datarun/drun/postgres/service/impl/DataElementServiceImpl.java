package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.drun.postgres.domain.DataElement;
import org.nmcpye.datarun.drun.postgres.repository.DataElementRepository;
import org.nmcpye.datarun.drun.postgres.repository.OptionSetRepository;
import org.nmcpye.datarun.drun.postgres.service.DataElementService;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalServiceImpl;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.service.impl.UserAccessService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Primary
@Transactional
public class DataElementServiceImpl
    extends IdentifiableRelationalServiceImpl<DataElement>
    implements DataElementService {
    private final Logger log = LoggerFactory.getLogger(DataElementServiceImpl.class);

    private final DataElementRepository repository;

    private final OptionSetRepository optionSetRepository;

    private final UserAccessService userAccessService;

    public DataElementServiceImpl(DataElementRepository repository,
                                  CacheManager cacheManager, OptionSetRepository optionSetRepository, UserAccessService userAccessService) {
        super(repository, cacheManager);
        this.repository = repository;
        this.optionSetRepository = optionSetRepository;
        this.userAccessService = userAccessService;
    }

    @Override
    public DataElement saveWithRelations(DataElement element) {
        if (element.getType().isOptionsType() && element.getOptionSet() != null) {
            final var optionSet = optionSetRepository.findByUid(element.getOptionSet().getUid())
                .or(() -> optionSetRepository.findById(element.getOptionSet().getId()))
                .orElseThrow(() -> new IllegalStateException(element.getName() + "'s Option Set not found"));
            element.setOptionSet(optionSet);
        }
        return repository.save(element);
    }

    @Override
    public Page<DataElement> findAllByUser(Pageable pageable, QueryRequest queryRequest) {

        if (!SecurityUtils.isAuthenticated()) {
            return Page.empty(pageable);
        }

        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repository.findAll(pageable);
        }

        List<String> userDataElements = userAccessService.getUserFormsWithWritePermission(SecurityUtils.getCurrentUserLogin().get())
            .stream()
            .flatMap((form) -> form.getFields().stream())
            .map(FormDataElementConf::getId).toList();

        Page<DataElement> resultsPage = repository.findAllByUidIn(userDataElements, pageable);

        return resultsPage;
    }

    @Override
    public Page<DataElement> findAllByUser(Specification<DataElement> spec, Pageable pageable) {

        if (!SecurityUtils.isAuthenticated()) {
            return Page.empty(pageable);
        }

        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repository.findAll(spec, pageable);
        }

        List<String> userDataElements = userAccessService.getUserFormsWithWritePermission(SecurityUtils.getCurrentUserLogin().get())
            .stream()
            .flatMap((form) -> form.getFields().stream())
            .map(FormDataElementConf::getId).toList();

        Page<DataElement> resultsPage = repository.findAllByUidIn(userDataElements, pageable);

        return resultsPage;
    }
}
