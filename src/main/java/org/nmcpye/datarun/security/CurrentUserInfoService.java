package org.nmcpye.datarun.security;

import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.jpa.team.repository.TeamSpecifications;
import org.nmcpye.datarun.jpa.team.repository.TeamRepository;
import org.nmcpye.datarun.jpa.usegroup.UserGroup;
import org.nmcpye.datarun.jpa.usegroup.repository.UserGroupRepository;
import org.nmcpye.datarun.jpa.user.repository.UserRepository;
import org.nmcpye.datarun.userdetail.CurrentUserActivityInfo;
import org.nmcpye.datarun.userdetail.CurrentUserGroupInfo;
import org.nmcpye.datarun.userdetail.CurrentUserTeamInfo;
import org.nmcpye.datarun.userdetail.UserFormAccess;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.nmcpye.datarun.jpa.user.repository.UserRepository.*;

@Service
@Transactional(readOnly = true)
public class CurrentUserInfoService {
    final private TeamRepository teamRepository;
    final private UserRepository userRepository;
    final private UserGroupRepository userGroupRepository;

    public CurrentUserInfoService(TeamRepository teamRepository, UserRepository userRepository,
                                  UserGroupRepository userGroupRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.userGroupRepository = userGroupRepository;
    }

    @Cacheable(cacheNames = USER_TEAM_IDS_CACHE, key = "#userLogin")
    public CurrentUserTeamInfo getUserTeamInfo(String userLogin) {
        final var user = userRepository.findOneWithAuthoritiesByLogin(userLogin).orElseThrow(() ->
            new UsernameNotFoundException("User with login " + userLogin + " was not found in the database"));
        final var teams = new HashSet<>(teamRepository.findAllByUserLogin(userLogin, false));
        final var managedTeams = teams.stream()
            .flatMap(team -> team.getManagedTeams().stream())
            .filter(team -> !team.getDisabled())
            .filter(team -> !team.getActivity().getDisabled())
            .toList();

        final var teamIds = teams
            .stream().map(Team::getUid)
            .collect(Collectors.toSet());

        final var managedTeamUids = managedTeams
            .stream().map(Team::getUid)
            .collect(Collectors.toSet());

        return CurrentUserTeamInfo
            .builder()
            .teamUIDs(teamIds)
            .managedTeamUIDs(managedTeamUids)
            .userId(user.getId())
            .userUID(user.getUid())
            .build();
    }

    @Cacheable(cacheNames = USER_ACTIVITY_IDS_CACHE, key = "#userLogin")
    public CurrentUserActivityInfo getUserActivityInfo(String userLogin) {
        final var user = userRepository.findOneWithAuthoritiesByLogin(userLogin).orElseThrow(() ->
            new UsernameNotFoundException("User with login " + userLogin + " was not found in the database"));

        final var teams = new HashSet<>(teamRepository.findAllByUserLogin(userLogin, false));

        final var activities = teams.stream()
            .map(Team::getActivity)
            .toList();

        return CurrentUserActivityInfo
            .builder()
            .userId(user.getId())
            .userUID(user.getUid())
            .activityUIDs(activities
                .stream()
                .map(Activity::getUid)
                .collect(Collectors.toSet()))
            .build();
    }

    @Cacheable(cacheNames = USER_TEAM_FORM_ACCESS_CACHE, key = "#userLogin")
    public List<UserFormAccess> getUserFormAccess(String userLogin, Collection<String> teamUIDs) {
        final var user = userRepository.findOneWithAuthoritiesByLogin(userLogin).orElseThrow(() ->
            new UsernameNotFoundException("User with login " + userLogin + " was not found in the database"));
        final var teams = new HashSet<>(teamRepository.findAll(TeamSpecifications.isEnabled()
            .and((root, query, cb) -> root.get("id").in(teamUIDs))));

        List<UserFormAccess> formAccesses = new ArrayList<>();
        for (final Team team : teams) {
            formAccesses.addAll(team.getFormPermissions()
                .stream()
                .map(formPermissions -> UserFormAccess.builder()
                    .form(formPermissions.getForm())
                    .team(team.getUid())
                    .user(user.getUid())
                    .permissions(formPermissions.getPermissions()).build()).toList());
        }

//        final var formAccess = formAccesses.stream()
//            .collect(Collectors
//                .toMap(UserFormAccess::getForm,
//                    userFormAccess -> userFormAccess));

        return formAccesses;
    }

    @Cacheable(cacheNames = USER_GROUP_IDS_CACHE, key = "#userLogin")
    public CurrentUserGroupInfo getUserGroupIds(String userLogin) {
        final var user = userRepository.findOneWithAuthoritiesByLogin(userLogin).orElseThrow(() ->
            new UsernameNotFoundException("User with login " + userLogin + " was not found in the database"));

        final var userGroupIds = new HashSet<>(userGroupRepository.findAllByUserLogin(userLogin, false))
            .stream().map(UserGroup::getUid)
            .collect(Collectors.toSet());
        return CurrentUserGroupInfo
            .builder()
            .userGroupUIDs(userGroupIds)
            .userId(user.getId())
            .userUID(user.getUid())
            .build();
    }
}
