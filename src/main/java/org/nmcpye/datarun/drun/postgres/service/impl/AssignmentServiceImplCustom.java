package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.AssignmentServiceCustom;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalServiceImpl;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class AssignmentServiceImplCustom
    extends IdentifiableRelationalServiceImpl<Assignment>
    implements AssignmentServiceCustom {

    final AssignmentRelationalRepositoryCustom repositoryCustom;
    final TeamRelationalRepositoryCustom teamRepository;
    final OrgUnitRelationalRepositoryCustom orgUnitRepository;

    public AssignmentServiceImplCustom(AssignmentRelationalRepositoryCustom repositoryCustom,
                                       TeamRelationalRepositoryCustom teamRepository,
                                       OrgUnitRelationalRepositoryCustom orgUnitRepository) {
        super(repositoryCustom);
        this.repositoryCustom = repositoryCustom;
        this.teamRepository = teamRepository;
        this.orgUnitRepository = orgUnitRepository;
    }

    @Override
    public Assignment saveWithRelations(Assignment object) {
        return repositoryCustom.save(object);
    }

    @Override
    public Page<Assignment> findAllByUser(Pageable pageable) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repositoryCustom.findAll(pageable);
        }
        return repositoryCustom.findAllByUser(pageable);
    }
}
