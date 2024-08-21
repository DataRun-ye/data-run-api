package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.drun.postgres.repository.ActivityRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.ActivityServiceCustom;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalServiceImpl;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class ActivityServiceCustomImpl
    extends IdentifiableRelationalServiceImpl<Activity>
    implements ActivityServiceCustom {

    private final ActivityRelationalRepositoryCustom repositoryCustom;


    public ActivityServiceCustomImpl(ActivityRelationalRepositoryCustom repositoryCustom) {
        super(repositoryCustom);
        this.repositoryCustom = repositoryCustom;
    }

    @Override
    public Page<Activity> findAllByUser(Pageable pageable) {
        return repositoryCustom.findAllByUser(pageable);
    }
}
