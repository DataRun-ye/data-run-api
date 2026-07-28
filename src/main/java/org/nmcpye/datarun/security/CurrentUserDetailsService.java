package org.nmcpye.datarun.security;

import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.jpa.team.TeamFormPermissions;
import org.nmcpye.datarun.jpa.team.repository.TeamRepository;
import org.nmcpye.datarun.jpa.user.User;
import org.nmcpye.datarun.jpa.user.UserNotActivatedException;
import org.nmcpye.datarun.jpa.userauthority.Authority;
import org.nmcpye.datarun.userdetail.UserFormAccess;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds the authorization snapshot used for one authenticated request.
 *
 * <p>Do not add a cross-request cache here without owning every team, activity,
 * and form-grant invalidation path. Access changes must be visible when the
 * next request rebuilds its principal.</p>
 */
@Service
@Transactional(readOnly = true)
public class CurrentUserDetailsService {
    private final TeamRepository teamRepository;

    public CurrentUserDetailsService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    public CurrentUserDetails createUserDetails(User user) {
        if (!user.isActivated()) {
            throw new UserNotActivatedException("User " + user.getLogin().toLowerCase() + " was not activated");
        }

        Set<Team> teams = new HashSet<>(teamRepository.findAllByUserLogin(user.getLogin(), false));
        Set<Team> managedTeams = teams.stream()
            .map(Team::getManagedTeams)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .filter(this::isEnabled)
            .collect(Collectors.toSet());
        List<UserFormAccess> formAccess = teams.stream()
            .flatMap(team -> Optional.ofNullable(team.getFormPermissions())
                .orElseGet(Collections::emptySet)
                .stream()
                .map(permission -> toUserFormAccess(user, team, permission)))
            .toList();

        return CurrentUserDetailsImpl.builder()
            .id(user.getId())
            .uid(user.getUid())
            .username(user.getLogin())
            .password(user.getPassword())
            .enabled(user.isActivated())
            .accountNonExpired(user.isActivated())
            .accountNonLocked(user.isActivated())
            .credentialsNonExpired(user.isActivated())
            .authorities(user.getAuthorities().stream()
                .map(authority -> new SimpleGrantedAuthority(authority.getName()))
                .collect(Collectors.toList()))
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .mobile(user.getMobile())
            .email(user.getEmail())
            .langKey(user.getLangKey())
            .imageUrl(user.getImageUrl())
            .isSuper(user.getAuthorities().stream()
                .map(Authority::getName)
                .anyMatch(AuthoritiesConstants.ADMIN::equals))
            .userTeamsUIDs(teams.stream().map(Team::getUid).collect(Collectors.toSet()))
            .managedTeamsUIDs(managedTeams.stream().map(Team::getUid).collect(Collectors.toSet()))
            .activityUIDs(teams.stream()
                .map(Team::getActivity)
                .filter(Objects::nonNull)
                .map(activity -> activity.getUid())
                .collect(Collectors.toSet()))
            .userFormsUIDs(formAccess.stream().map(UserFormAccess::getForm).collect(Collectors.toSet()))
            .formAccess(formAccess)
            .build();
    }

    private boolean isEnabled(Team team) {
        return team != null
            && !Boolean.TRUE.equals(team.getDisabled())
            && team.getActivity() != null
            && !Boolean.TRUE.equals(team.getActivity().getDisabled());
    }

    private UserFormAccess toUserFormAccess(User user, Team team, TeamFormPermissions permission) {
        return UserFormAccess.builder()
            .form(permission.getForm())
            .team(team.getUid())
            .user(user.getUid())
            .permissions(permission.getPermissions())
            .build();
    }
}
