package org.nmcpye.datarun.security;

import org.nmcpye.datarun.common.repository.UserRepository;
import org.nmcpye.datarun.common.security.CurrentUserActivityInfo;
import org.nmcpye.datarun.common.security.CurrentUserGroupInfo;
import org.nmcpye.datarun.common.security.CurrentUserTeamInfo;
import org.nmcpye.datarun.common.security.UserFormAccess;
import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.drun.postgres.common.TeamSpecifications;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.domain.UserGroup;
import org.nmcpye.datarun.drun.postgres.repository.TeamRepository;
import org.nmcpye.datarun.drun.postgres.repository.UserGroupRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.nmcpye.datarun.common.repository.UserRepository.*;

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

//    @Cacheable(cacheNames = USER_FORM_IDS_CACHE, key = "#userLogin")
//    public CurrentUserFormInfo getUserFormInfo(String userLogin) {
//        final var user = userRepository.findOneWithAuthoritiesByLogin(userLogin).orElseThrow(() ->
//            new UsernameNotFoundException("User with login " + userLogin + " was not found in the database"));
//
//        final var teams = new HashSet<>(teamRepository.findAllByUserLogin(userLogin, false));
//
//        final var formUIDs = teams.stream()
//            .map(Team::getFormPermissions)
//            .flatMap(Collection::stream)
//            .map(TeamFormPermissions::getForm)
//            .collect(Collectors.toSet());
//
//        return CurrentUserFormInfo
//            .builder()
//            .userId(user.getId())
//            .userUID(user.getUid())
//            .formUIDs(formUIDs)
//            .build();
//    }

    @Cacheable(cacheNames = USER_TEAM_FORM_ACCESS_CACHE, key = "#userLogin")
    public List<UserFormAccess> getUserFormAccess(String userLogin, Collection<String> teamUIDs) {
        final var user = userRepository.findOneWithAuthoritiesByLogin(userLogin).orElseThrow(() ->
            new UsernameNotFoundException("User with login " + userLogin + " was not found in the database"));
        final var teams = new HashSet<>(teamRepository.findAll(TeamSpecifications.isEnabled()
            .and((root, query, cb) -> root.get("uid").in(teamUIDs))));

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
