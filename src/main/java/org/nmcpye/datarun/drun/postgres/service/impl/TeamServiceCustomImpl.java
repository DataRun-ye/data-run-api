package org.nmcpye.datarun.drun.postgres.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.drun.postgres.common.TeamSpecifications;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.repository.ActivityRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.TeamServiceCustom;
import org.nmcpye.datarun.repository.UserRepository;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Primary
@Transactional
public class TeamServiceCustomImpl
    extends TeamSpecifications
    implements TeamServiceCustom {
    private static final Logger log = LoggerFactory.getLogger(TeamServiceCustomImpl.class);

    TeamRelationalRepositoryCustom repository;

    final UserRepository userRepository;
    final ActivityRelationalRepositoryCustom activityRepository;

    public TeamServiceCustomImpl(TeamRelationalRepositoryCustom repository,
                                 UserRepository userRepository, ActivityRelationalRepositoryCustom activityRepository) {
        super(repository);
        this.repository = repository;
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
    }

    @Override
    public Team saveWithRelations(Team object) {
//        Team parent = object.getParent();
//        if (parent != null) {
//            parent = findTeam(parent);
//            object.setParent(parent);
//        }

        Set<Long> usersUids = object.getUsers().stream().map(User::getId).collect(Collectors.toSet());
        Set<User> users = new HashSet<>(userRepository.findAllById(usersUids));
        object.setUsers(users);

        return repository.save(object);
    }

    private Team findTeam(Team parent) {
        return Optional.ofNullable(parent.getUid())
            .flatMap(repository::findByUid)
            .or(() -> Optional.ofNullable(parent.getId())
                .flatMap(repository::findById))
            .or(() -> {
                String code = parent.getCode();
                var activity = parent.getActivity();
                String activityUid;
                if (activity != null) {
                    activityUid = activity.getUid();
                } else {
                    activityUid = null;
                }
                return repository.findByCodeAndActivityUid(code, activityUid);
            })
            .orElseThrow(() -> new PropertyNotFoundException("Team not found: " + parent));
    }

    @Override
    public Page<Team> findAllByUser(Pageable pageable, QueryRequest queryRequest) {
        Specification<Team> spec = hasAccess();
        if(!queryRequest.isIncludeDisabled()) {
            spec = spec.and(isNotDisabled());
        }
//        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
//
//            return repository.findAll(spec, pageable);
//        }
//        if (!SecurityUtils.isAuthenticated()) {
//            return Page.empty(pageable);
//        }

        return repository.fetchBagRelationships(this.findAll(spec, pageable));
    }

    @Override
    public List<Team> findAllByUser(QueryRequest queryRequest) {
        Specification<Team> spec = hasAccess();
        if(!queryRequest.isIncludeDisabled()) {
            spec = spec.and(isNotDisabled());
        }
//
//        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
//            return repository.findAll();
//        }
//        if (!SecurityUtils.isAuthenticated()) {
//            return Collections.emptyList();
//        }
        return repository.findAll(spec);

//        return repository.fetchBagRelationships(this.findAll(hasAccess()));
    }

    @Override
    public Optional<Team> partialUpdate(Team team) {
        log.debug("Request to partially update Team : {}", team);

        return repository.findByUid(team.getUid())
            .or(() -> repository
                .findById(team.getId()))
            .map(existingTeam -> {
//                if (team.getParent() != null) {
//                    existingTeam.setParent(findTeam(existingTeam.getParent()));
//                }

                if (!team.getUsers().isEmpty()) {
                    Set<String> usersLogins = team.getUsers().stream()
                        .map(User::getLogin)
                        .collect(Collectors.toSet());
                    Set<User> users = new HashSet<>(userRepository.findByLoginIn(usersLogins));

                    existingTeam.setUsers(users);
                }

                if (team.getActivity() != null) {
                    var activity = activityRepository.findByUid(team.getActivity().getUid())
                        .or(() -> activityRepository.findById(team.getActivity().getId()));
                    existingTeam.setActivity(activity.get());
                }

                if (team.getUid() != null) {
                    existingTeam.setUid(team.getUid());
                }
                if (team.getCode() != null) {
                    existingTeam.setCode(team.getCode());
                }
                if (team.getName() != null) {
                    existingTeam.setName(team.getName());
                }
                if (team.getDescription() != null) {
                    existingTeam.setDescription(team.getDescription());
                }
                if (team.getDisabled() != null) {
                    existingTeam.setDisabled(team.getDisabled());
                }
                if (team.getDeleteClientData() != null) {
                    existingTeam.setDeleteClientData(team.getDeleteClientData());
                }
                if (team.getCreatedBy() != null) {
                    existingTeam.setCreatedBy(team.getCreatedBy());
                }
                if (team.getCreatedDate() != null) {
                    existingTeam.setCreatedDate(team.getCreatedDate());
                }
                if (team.getLastModifiedBy() != null) {
                    existingTeam.setLastModifiedBy(team.getLastModifiedBy());
                }
                if (team.getLastModifiedDate() != null) {
                    existingTeam.setLastModifiedDate(team.getLastModifiedDate());
                }

                return existingTeam;
            })
            .map(repository::save);
    }

//    @Transactional
//    @Override
//    public void updatePaths() {
//        repository.updatePaths();
//    }

//    /**
//     * This is scheduled to get fired everyday, at 03:19 (am).
//     * - **`0`**: Second (`0` seconds)
//     * - **`19`**: Minute (`15` minutes)
//     * - **`3`**: Hour (`3` AM)
//     * - **`* *`**: Day of month and Month (`* *` means every day of every month)
//     * - **`?`**: Day of the week (`?` is used when you don't care about the specific day of the week)
//     */
////    @Scheduled(cron = "0 0 3 * * ?")
//    @Scheduled(cron = "0 19 3 * * ?")
//    @Override
//    @Transactional
//    public void forceUpdatePaths() {
//        repository.forceUpdatePaths();
//    }
//
}
