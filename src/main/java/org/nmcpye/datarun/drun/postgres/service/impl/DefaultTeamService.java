package org.nmcpye.datarun.drun.postgres.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.common.jpa.impl.DefaultJpaAuditableService;
import org.nmcpye.datarun.user.repository.UserRepository;
import org.nmcpye.datarun.activity.Activity;
import org.nmcpye.datarun.user.User;
import org.nmcpye.datarun.team.Team;
import org.nmcpye.datarun.activity.repository.ActivityRepository;
import org.nmcpye.datarun.team.repository.TeamRepository;
import org.nmcpye.datarun.drun.postgres.service.TeamService;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.security.useraccess.UserAccessService;
import org.nmcpye.datarun.startupmigration.postgres.TeamFormPermissionsMigration;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.nmcpye.datarun.drun.postgres.common.TeamSpecifications.getManagedSpecification;

@Service
@Primary
@Transactional
public class DefaultTeamService extends DefaultJpaAuditableService<Team> implements TeamService {
    private static final Logger log = LoggerFactory.getLogger(DefaultTeamService.class);

    final private TeamRepository repository;

    final private UserRepository userRepository;
    final private ActivityRepository activityRepository;
    private final TeamFormPermissionsMigration formPermissionsMigration;

    public DefaultTeamService(TeamRepository repository, UserRepository userRepository, ActivityRepository activityRepository, CacheManager cacheManager, UserAccessService userAccessService, TeamFormPermissionsMigration formPermissionsMigration) {
        super(repository, cacheManager, userAccessService);
        this.repository = repository;
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
        this.formPermissionsMigration = formPermissionsMigration;
    }

    @Override
    public Team saveWithRelations(Team team) {
        Activity activity = null;

        if (team.getActivity() != null) {
            activity = findActivity(team.getActivity());
        }

        Set<Team> managedTeams = team.getManagedTeams();
        if (!managedTeams.isEmpty()) {
            Set<Team> teamsManaged = managedTeams.stream().map(this::findTeam).collect(Collectors.toSet());
            team.setManagedTeams(teamsManaged);
        }

        team.setActivity(activity);

        Set<Long> usersUids = team.getUsers().stream().map(User::getId).collect(Collectors.toSet());
        Set<User> users = new HashSet<>(userRepository.findAllById(usersUids));
        team.setUsers(users);

        this.clearTeamCaches(team);
        return save(team);
    }

    private Activity findActivity(Activity activity) {
        return Optional.ofNullable(activity.getId()).flatMap(activityRepository::findById).or(() -> Optional.ofNullable(activity.getUid()).flatMap(activityRepository::findByUid)).orElseThrow(() -> {
            log.error("Activity not found: " + activity.getUid());
            return new PropertyNotFoundException("Activity uid not found: " + activity.getUid() + "activity:");
        });
    }

    private Team findTeam(Team team) {
        return Optional.ofNullable(team.getUid()).flatMap(repository::findByUid).or(() -> Optional.ofNullable(team.getId()).flatMap(repository::findById)).or(() -> {
            String code = team.getCode();
            var activity = team.getActivity();
            String activityUid;
            if (activity != null) {
                activityUid = activity.getUid();
            } else {
                activityUid = null;
            }
            return repository.findByCodeAndActivityUid(code, activityUid);
        }).orElseThrow(() -> {
            log.error("Team not found: " + team.getUid());
            return new PropertyNotFoundException("Team not found: " + team);
        });
    }

//    @Override
//    public Page<Team> findAllByUser(Pageable pageable, QueryRequest queryRequest) {
//        Specification<Team> spec = canRead();
//        if (queryRequest == null || !queryRequest.isIncludeDisabled()) {
//            spec = spec.and(TeamSpecifications.isEnabled());
//        }
//
//        return repository.fetchBagRelationships(repository.findAll(spec, pageable));
//    }
//
//    @Override
//    public List<Team> findAllByUser(QueryRequest queryRequest) {
//        Specification<Team> spec = canRead();
//        if (queryRequest == null || !queryRequest.isIncludeDisabled()) {
//            spec = spec.and(TeamSpecifications.isEnabled());
//        }
//
//        return repository.fetchBagRelationships(repository.findAll(spec));
//    }

    @Override
    public Page<Team> findAllManagedByUser(Pageable pageable, QueryRequest queryRequest) {

//        Specification<Team> specManage = TeamSpecifications.getManagedTeamsByUserTeams(SecurityUtils.getCurrentUserLoginOrThrow(new ErrorMessage(ErrorCode.E3004, getClass().getName()))).and(TeamSpecifications.isEnabled());
//        if (queryRequest == null || !queryRequest.isIncludeDisabled()) {
//            specManage = Specification.where(specManage).and(TeamSpecifications.isEnabled());
//        }
        final var managedSpec = getManagedSpecification(SecurityUtils
                .getCurrentUserDetails().orElseThrow(() ->
                    new IllegalQueryException(new ErrorMessage(ErrorCode.E3004, getClass().getName()))),
            queryRequest);

        return repository.fetchBagRelationships(repository.findAll(managedSpec, pageable));
    }

    @Override
    public Optional<Team> partialUpdate(Team team) {
        log.debug("Request to partially update Team : {}", team);

        return repository.findByUid(team.getUid()).or(() -> repository.findById(Objects.requireNonNull(team.getId()))).map(existingTeam -> {
            this.clearTeamCaches(existingTeam);
            if (!team.getUsers().isEmpty()) {
                Set<String> usersLogins = team.getUsers().stream().map(User::getLogin).collect(Collectors.toSet());
                Set<User> users = new HashSet<>(userRepository.findByLoginIn(usersLogins));

                existingTeam.setUsers(users);
            }

            if (team.getActivity() != null) {
                var activity = activityRepository.findByUid(team.getActivity().getUid()).or(() -> activityRepository.findById(team.getActivity().getId()));
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
        }).map(repository::save);
    }

    @Override
    public void runFormPermissionsMigration() {
        formPermissionsMigration.processInChunks();
    }

    private void clearTeamCaches(Team team) {
        team.getUsers().forEach(user -> {
            this.clearCaches(UserRepository.USERS_BY_LOGIN_CACHE, user.getLogin());
            this.clearCaches(UserRepository.USERS_BY_EMAIL_CACHE, user.getEmail());
            this.clearCaches(UserRepository.USER_TEAM_IDS_CACHE, user.getLogin());
            this.clearCaches(UserRepository.USER_GROUP_IDS_CACHE, user.getLogin());
            this.clearCaches(UserRepository.USER_ACTIVITY_IDS_CACHE, user.getLogin());
            this.clearCaches(UserRepository.USER_TEAM_FORM_ACCESS_CACHE, user.getLogin());
        });
    }
}
