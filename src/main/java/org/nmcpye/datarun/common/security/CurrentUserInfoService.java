package org.nmcpye.datarun.common.security;

import org.nmcpye.datarun.common.repository.UserRepository;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.domain.UserGroup;
import org.nmcpye.datarun.drun.postgres.repository.TeamRepository;
import org.nmcpye.datarun.drun.postgres.repository.UserGroupRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.nmcpye.datarun.common.repository.UserRepository.USER_GROUP_IDS_CACHE;
import static org.nmcpye.datarun.common.repository.UserRepository.USER_TEAM_IDS_CACHE;

@Service
@Transactional(readOnly = true)
public class CurrentUserInfoService {
    final private TeamRepository teamRepository;
    final private UserRepository userRepository;
    final private UserGroupRepository userGroupRepository;

    public CurrentUserInfoService(TeamRepository teamRepository, UserRepository userRepository, UserGroupRepository userGroupRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.userGroupRepository = userGroupRepository;
    }

    public Optional<User> findCurrentByLogin(String login) {
        return userRepository.findOneByLogin(login);
    }

    public Optional<User> findCurrentByEmail(String email) {
        return userRepository.findOneWithAuthoritiesByEmailIgnoreCase(email);
    }

    @Cacheable(cacheNames = USER_TEAM_IDS_CACHE, key = "#userLogin")
    public CurrentUserTeamInfo getUserTeamInfo(String userLogin) {
        final var user = findCurrentByLogin(userLogin).orElseThrow(() ->
            new UsernameNotFoundException("User with login " + userLogin + " was not found in the database"));
        final var teams = new HashSet<>(teamRepository.findAllByUserLogin(userLogin, false));
        final var managedTeams = teams.stream()
            .flatMap(team -> team.getManagedTeams().stream())
            .filter(team -> !team.getDisabled())
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


    @Cacheable(cacheNames = USER_GROUP_IDS_CACHE, key = "#userLogin")
    public CurrentUserGroupInfo getUserGroupIds(String userLogin) {
        final var user = findCurrentByLogin(userLogin).orElseThrow(() ->
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
