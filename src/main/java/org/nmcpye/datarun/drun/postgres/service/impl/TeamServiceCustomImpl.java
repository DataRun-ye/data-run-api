package org.nmcpye.datarun.drun.postgres.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.drun.postgres.common.TeamSpecifications;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.repository.ActivityRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.TeamServiceCustom;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalServiceImpl;
import org.nmcpye.datarun.repository.UserRepository;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.nmcpye.datarun.drun.postgres.common.TeamSpecifications.canRead;
import static org.nmcpye.datarun.drun.postgres.common.TeamSpecifications.isEnabled;

@Service
@Primary
@Transactional
public class TeamServiceCustomImpl
    extends IdentifiableRelationalServiceImpl<Team>
    implements TeamServiceCustom {
    private static final Logger log = LoggerFactory.getLogger(TeamServiceCustomImpl.class);

    final private TeamRelationalRepositoryCustom repository;

    final private UserRepository userRepository;
    final private ActivityRelationalRepositoryCustom activityRepository;

    public TeamServiceCustomImpl(TeamRelationalRepositoryCustom repository,
                                 UserRepository userRepository,
                                 ActivityRelationalRepositoryCustom activityRepository) {
        super(repository);
        this.repository = repository;
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
    }

    @Override
    public Team saveWithRelations(Team object) {
        Activity activity = null;

        if (object.getActivity() != null) {
            activity = findActivity(object.getActivity());
        }

        Set<Team> managedTeams = object.getManagedTeams();
        if (!managedTeams.isEmpty()) {
            Set<Team> teamsManaged = managedTeams.stream().map(this::findTeam).collect(Collectors.toSet());
            object.setManagedTeams(teamsManaged);
        }

        object.setActivity(activity);

        Set<Long> usersUids = object.getUsers().stream().map(User::getId).collect(Collectors.toSet());
        Set<User> users = new HashSet<>(userRepository.findAllById(usersUids));
        object.setUsers(users);
        return repository.save(object);
    }

    private Activity findActivity(Activity activity) {
        return Optional.ofNullable(activity.getId())
            .flatMap(activityRepository::findById)
            .or(() -> Optional.ofNullable(activity.getUid())
                .flatMap(activityRepository::findByUid))
            .orElseThrow(() -> {
                log.error("Activity not found: " + activity.getUid());
                return new PropertyNotFoundException("Activity uid not found: " + activity.getUid() + "activity:");
            });
    }

    private Team findTeam(Team team) {
        return Optional.ofNullable(team.getUid())
            .flatMap(repository::findByUid)
            .or(() -> Optional.ofNullable(team.getId())
                .flatMap(repository::findById))
            .or(() -> {
                String code = team.getCode();
                var activity = team.getActivity();
                String activityUid;
                if (activity != null) {
                    activityUid = activity.getUid();
                } else {
                    activityUid = null;
                }
                return repository.findByCodeAndActivityUid(code, activityUid);
            })
            .orElseThrow(() -> {
                log.error("Team not found: " + team.getUid());
                return new PropertyNotFoundException("Team not found: " + team);
            });
    }

    @Override
    public Page<Team> findAllByUser(Pageable pageable, QueryRequest queryRequest) {
        Specification<Team> spec = canRead();
        if (!queryRequest.isIncludeDisabled()) {
            spec = spec.and(isEnabled());
        }

        return repository.fetchBagRelationships(repository.findAll(spec, pageable));
    }

    @Override
    public List<Team> findAllByUser(QueryRequest queryRequest) {
        Specification<Team> spec = canRead();
        if (!queryRequest.isIncludeDisabled()) {
            spec = spec.and(isEnabled());
        }

        return repository.fetchBagRelationships(repository.findAll(spec));
    }

    @Override
    public Page<Team> findAllManagedByUser(Pageable pageable) {
        if (SecurityUtils.getCurrentUserLogin().isEmpty()) {
            return Page.empty();
        }

        Specification<Team> spec = TeamSpecifications.getManagedTeamsByUserTeams(SecurityUtils.getCurrentUserLogin().get());


        var userTeams = repository.findAll(canRead().and(isEnabled()));

        Specification<Team> spec3 = TeamSpecifications.getManagedTeamsForTeams(userTeams.stream()
            .flatMap(team -> team.getManagedTeams().stream()
                .distinct()).map(Team::getId).toList());

        // getManagedTeamsForTeams
        var managedTeams = repository.fetchBagRelationships(repository.findAll(spec, pageable));
        var managedTeams3 = repository.fetchBagRelationships(repository.findAll(spec3, pageable));


        return repository.fetchBagRelationships(repository.findAll(spec, pageable));
    }

    @Override
    public List<Team> findAllManagedByUser() {
        if (SecurityUtils.getCurrentUserLogin().isEmpty()) {
            return Collections.emptyList();
        }
        Specification<Team> spec = TeamSpecifications.getManagedTeamsByUserTeams(SecurityUtils.getCurrentUserLogin().get());
//            .and(isNotDisabled());

        return repository.fetchBagRelationships(repository.findAll(spec));
    }

    @Override
    public Optional<Team> partialUpdate(Team team) {
        log.debug("Request to partially update Team : {}", team);

        return repository.findByUid(team.getUid())
            .or(() -> repository
                .findById(team.getId()))
            .map(existingTeam -> {
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
}
