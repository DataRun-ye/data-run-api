package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.drun.postgres.domain.OptionSet;
import org.nmcpye.datarun.drun.postgres.repository.OptionSetRepository;
import org.nmcpye.datarun.drun.postgres.service.OptionSetService;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalServiceImpl;
import org.nmcpye.datarun.mongo.service.impl.UserAccessService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Primary
@Transactional
public class OptionSetServiceImpl
    extends IdentifiableRelationalServiceImpl<OptionSet>
    implements OptionSetService {
    private final OptionSetRepository repository;

    private final UserAccessService userAccessService;

    public OptionSetServiceImpl(OptionSetRepository repository,
                                CacheManager cacheManager, UserAccessService userAccessService) {
        super(repository, cacheManager);
        this.repository = repository;
        this.userAccessService = userAccessService;
    }

    @Override
    public Page<OptionSet> findAllByUser(Pageable pageable, QueryRequest queryRequest) {

        if (!SecurityUtils.isAuthenticated()) {
            return Page.empty(pageable);
        }

        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repository.findAll(pageable);
        }

        userAccessService.getUserOptionSets(SecurityUtils.getCurrentUserLogin().get());
        var userOptionSets = getUserOptionSets(SecurityUtils.getCurrentUserLogin().get())
            .stream().map(OptionSet::getUid).toList();


        Page<OptionSet> resultsPage = repository.findAllByUidIn(userOptionSets, pageable);

        return resultsPage;
    }

    @Override
    public Page<OptionSet> findAllByUser(Specification<OptionSet> spec, Pageable pageable) {

        if (!SecurityUtils.isAuthenticated()) {
            return Page.empty(pageable);
        }

        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repository.findAll(spec, pageable);
        }

        userAccessService.getUserOptionSets(SecurityUtils.getCurrentUserLogin().get());
        var userOptionSets = getUserOptionSets(SecurityUtils.getCurrentUserLogin().get())
            .stream().map(OptionSet::getUid).toList();


        Page<OptionSet> resultsPage = repository.findAllByUidIn(userOptionSets, pageable);

        return resultsPage;
    }

    private Set<OptionSet> getUserOptionSets(String userLogin) {
        return userAccessService.getAllAccessibleUserForms(userLogin)
            .stream().flatMap(f -> f.getFields().stream())
            .filter(f -> f.getType().isOptionsType())
            .map(field -> repository.findByUid(field.getOptionSet()).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }
}
