package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.drun.postgres.common.DefaultIdentifiableSpecifications;
import org.nmcpye.datarun.drun.postgres.repository.ActivityRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.ActivityServiceCustom;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class ActivityServiceCustomImpl
    extends DefaultIdentifiableSpecifications<Activity>
    implements ActivityServiceCustom {

    private final ActivityRelationalRepositoryCustom repositoryCustom;


    public ActivityServiceCustomImpl(ActivityRelationalRepositoryCustom repositoryCustom) {
        super(repositoryCustom);
        this.repositoryCustom = repositoryCustom;
    }

    @Override
    public Page<Activity> findAllByUser(Pageable pageable, QueryRequest queryRequest) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repositoryCustom.findAll(pageable);
        }
        return repositoryCustom.findAllByUser(pageable);
    }
}
