package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.common.jpa.impl.DefaultJpaAuditableService;
import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.drun.postgres.repository.ActivityRepository;
import org.nmcpye.datarun.drun.postgres.service.ActivityService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.security.useraccess.UserAccessService;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class DefaultActivityService extends DefaultJpaAuditableService<Activity> implements ActivityService {

    private final ActivityRepository repository;


    public DefaultActivityService(ActivityRepository repository, CacheManager cacheManager, UserAccessService userAccessService) {
        super(repository, cacheManager, userAccessService);
        this.repository = repository;
    }

    @Override
    public Page<Activity> findAllByUser(Pageable pageable, QueryRequest queryRequest) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repository.findAll(pageable);
        }
        return repository.findAllByUser(pageable);
    }

    private void clearCaches(Activity activity) {
//        team.getUsers().forEach(user -> {
//            this.clearCaches(UserRepository.USERS_BY_LOGIN_CACHE, user.getLogin());
//            this.clearCaches(UserRepository.USERS_BY_EMAIL_CACHE, user.getEmail());
//            this.clearCaches(UserRepository.USER_TEAM_IDS_CACHE, user.getLogin());
//            this.clearCaches(UserRepository.USER_GROUP_IDS_CACHE, user.getLogin());
//        });
    }
}
