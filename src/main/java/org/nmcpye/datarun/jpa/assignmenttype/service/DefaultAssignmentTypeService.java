package org.nmcpye.datarun.jpa.assignmenttype.service;

import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.assignmenttype.AssignmentType;
import org.nmcpye.datarun.jpa.assignmenttype.repository.AssignmentTypeRepository;
import org.nmcpye.datarun.jpa.common.DefaultJpaIdentifiableService;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class DefaultAssignmentTypeService
        extends DefaultJpaIdentifiableService<AssignmentType>
        implements AssignmentTypeService {

    private final AssignmentTypeRepository repository;

    public DefaultAssignmentTypeService(AssignmentTypeRepository repository,
                                        UserAccessService userAccessService,
                                        CacheManager cacheManager) {
        super(repository, cacheManager, userAccessService);
        this.repository = repository;
    }
}
