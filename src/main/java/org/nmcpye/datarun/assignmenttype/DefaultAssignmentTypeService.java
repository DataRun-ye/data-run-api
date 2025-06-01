package org.nmcpye.datarun.assignmenttype;

import org.nmcpye.datarun.assignmenttype.repository.AssignmentTypeRepository;
import org.nmcpye.datarun.common.jpa.DefaultJpaAuditableService;
import org.nmcpye.datarun.security.useraccess.UserAccessService;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class DefaultAssignmentTypeService
        extends DefaultJpaAuditableService<AssignmentType>
        implements AssignmentTypeService {

    private final AssignmentTypeRepository repository;

    public DefaultAssignmentTypeService(AssignmentTypeRepository repository,
                                        UserAccessService userAccessService,
                                        CacheManager cacheManager) {
        super(repository, cacheManager, userAccessService);
        this.repository = repository;
    }
}
