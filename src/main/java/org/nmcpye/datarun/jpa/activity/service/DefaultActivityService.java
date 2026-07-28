package org.nmcpye.datarun.jpa.activity.service;

import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.activity.repository.ActivityRepository;
import org.nmcpye.datarun.jpa.common.DefaultJpaIdentifiableService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.apiquery.QueryRequest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Hamza Assada 11/02/2022
 */
@Service
@Primary
@Transactional
public class DefaultActivityService
    extends DefaultJpaIdentifiableService<Activity>
    implements ActivityService {

    private final ActivityRepository repository;


    public DefaultActivityService(ActivityRepository repository, CacheManager cacheManager,
                                  UserAccessService userAccessService) {
        super(repository, cacheManager, userAccessService);
        this.repository = repository;
    }

    @Override
    public Page<Activity> findAllByUser(QueryRequest queryRequest, String jsonQueryBody) {
        Pageable pageable = queryRequest.getPageable();

        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repository.findAll(pageable);
        }
        return repository.findAllByUser(pageable);
    }
}
