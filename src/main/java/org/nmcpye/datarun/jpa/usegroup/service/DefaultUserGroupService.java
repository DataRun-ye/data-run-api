package org.nmcpye.datarun.jpa.usegroup.service;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaAuditableService;
import org.nmcpye.datarun.jpa.usegroup.UserGroup;
import org.nmcpye.datarun.jpa.usegroup.repository.UserGroupRepository;
import org.nmcpye.datarun.jpa.usegroup.repository.UserGroupSpecifications;
import org.nmcpye.datarun.jpa.user.User;
import org.nmcpye.datarun.jpa.user.repository.UserRepository;
import org.nmcpye.datarun.security.SecurityUtils;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Primary
@Transactional
public class DefaultUserGroupService extends DefaultJpaAuditableService<UserGroup> implements UserGroupService {
    private static final Logger log = LoggerFactory.getLogger(DefaultUserGroupService.class);

    final private UserGroupRepository repository;

    final private UserRepository userRepository;

    public DefaultUserGroupService(UserGroupRepository repository, UserRepository userRepository, CacheManager cacheManager, UserAccessService userAccessService) {
        super(repository, cacheManager, userAccessService);
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Override
    public UserGroup saveWithRelations(UserGroup userGroup) {
        Set<UserGroup> managedGroups = userGroup.getManagedGroups();
        if (!managedGroups.isEmpty()) {
            Set<UserGroup> teamsManaged = managedGroups.stream().map(this::findUserGroup).collect(Collectors.toSet());
            userGroup.setManagedGroups(teamsManaged);
        }

        Set<User> users = userGroup.getUsers().stream().map(this::findUser).collect(Collectors.toSet());
        userGroup.setUsers(users);

        this.clearGroupCaches(userGroup);
        return save(userGroup);
    }

    private UserGroup findUserGroup(UserGroup userGroup) {
        return Optional.ofNullable(userGroup.getUid()).flatMap(repository::findByUid).or(() -> Optional.ofNullable(userGroup.getId()).flatMap(repository::findById)).or(() -> Optional.ofNullable(userGroup.getCode()).flatMap(repository::findByCode)).orElseThrow(() -> {
            log.error("UserGroup not found: " + userGroup.getUid());
            return new PropertyNotFoundException("UserGroup not found: " + userGroup);
        });
    }

    private User findUser(User user) {
        return Optional.ofNullable(user.getUid()).flatMap(userRepository::findByUid).or(() -> Optional.ofNullable(user.getLogin()).flatMap(userRepository::findOneByLogin)).or(() -> Optional.ofNullable(user.getId()).flatMap(userRepository::findById)).orElseThrow(() -> {
            log.error("User not found: " + user.getUid());
            return new PropertyNotFoundException("UserGroup not found: " + user);
        });
    }

//    @Override
//    public Page<UserGroup> findAllByUser(Pageable pageable, QueryRequest queryRequest) {
//        Specification<UserGroup> spec = canRead();
//        if (queryRequest == null || !queryRequest.isIncludeDisabled()) {
//            spec = spec.and(UserGroupSpecifications.isEnabled());
//        }
//
//        return repository.fetchBagRelationships(repository.findAll(spec, pageable));
//    }
//
//    @Override
//    public List<UserGroup> findAllByUser(QueryRequest queryRequest) {
//        Specification<UserGroup> spec = canRead();
//        if (queryRequest == null || !queryRequest.isIncludeDisabled()) {
//            spec = spec.and(UserGroupSpecifications.isEnabled());
//        }
//
//        return repository.fetchBagRelationships(repository.findAll(spec));
//    }

    @Override
    public Page<UserGroup> findAllManagedByUser(Pageable pageable) {
        Specification<UserGroup> spec = UserGroupSpecifications.getManagedGroupsByUserGroups(SecurityUtils.getCurrentUserLoginOrThrow(new ErrorMessage(ErrorCode.E3004, getClass().getName()))).and(UserGroupSpecifications.isEnabled());

        return repository.fetchBagRelationships(repository.findAll(spec, pageable));
    }

    @Override
    public List<UserGroup> findAllManagedByUser() {
        Specification<UserGroup> spec = UserGroupSpecifications.getManagedGroupsByUserGroups(SecurityUtils.getCurrentUserLoginOrThrow(new ErrorMessage(ErrorCode.E3004, getClass().getName())));
//            .and(isNotDisabled());

        return repository.fetchBagRelationships(repository.findAll(spec));
    }

    private void clearGroupCaches(UserGroup userGroup) {
        userGroup.getUsers().forEach(user -> {
            this.clearCaches(UserRepository.USERS_BY_LOGIN_CACHE, user.getLogin());
            this.clearCaches(UserRepository.USERS_BY_EMAIL_CACHE, user.getEmail());
            this.clearCaches(UserRepository.USER_TEAM_IDS_CACHE, user.getLogin());
            this.clearCaches(UserRepository.USER_GROUP_IDS_CACHE, user.getLogin());
        });
    }
}
