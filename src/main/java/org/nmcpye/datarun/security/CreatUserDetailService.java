package org.nmcpye.datarun.security;

import org.nmcpye.datarun.common.security.UserFormAccess;
import org.nmcpye.datarun.domain.Authority;
import org.nmcpye.datarun.domain.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CreatUserDetailService {
    final private CurrentUserInfoService currentUserInfoService;

    public CreatUserDetailService(CurrentUserInfoService currentUserInfoService) {
        this.currentUserInfoService = currentUserInfoService;
    }


    /// //////
    public CurrentUserDetails createUserDetails(User user) {
        if (!user.isActivated()) {
            throw new UserNotActivatedException("User " + user.getLogin().toLowerCase() + " was not activated");
        }

        final var userTeamInfo = currentUserInfoService
            .getUserTeamInfo(user.getLogin());

        final var userFormAccess = currentUserInfoService
            .getUserFormAccess
                (user.getLogin(), userTeamInfo.getTeamUIDs());

        return CurrentUserDetailsImpl.builder()
            .uid(user.getUid())
            .username(user.getLogin())
            .password(user.getPassword())
            .enabled(user.isActivated())
            .accountNonExpired(user.isActivated())
            .accountNonLocked(user.isActivated())
            .credentialsNonExpired(user.isActivated())
            // TODO: migrate to use User's roles
            .authorities(user.getAuthorities().stream()
                .map(authority -> new SimpleGrantedAuthority(authority.getName()))
                .collect(Collectors.toList()))

            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .mobile(user.getMobile())
            .langKey(user.getLangKey())
            .imageUrl(user.getImageUrl())

            .isSuper(user.getAuthorities()
                .stream()
                .map(Authority::getName)
                .anyMatch((s) ->
                    s.equals(AuthoritiesConstants.ADMIN)))

            .userTeamsUIDs(userTeamInfo.getTeamUIDs())
            .managedTeamsUIDs(userTeamInfo.getManagedTeamUIDs())

            .activityUIDs(currentUserInfoService
                .getUserActivityInfo(user.getLogin()).getActivityUIDs())


            .userGroupsUIDs(currentUserInfoService
                .getUserGroupIds(user.getLogin()).getUserGroupUIDs())

            .userFormsUIDs(userFormAccess.stream()
                .map(UserFormAccess::getForm)
                .collect(Collectors.toSet()))

            .formAccess(userFormAccess)

            .build();
    }
}
