package org.nmcpye.datarun.security;

import org.hibernate.validator.internal.constraintvalidators.bv.EmailValidator;
import org.nmcpye.datarun.common.repository.UserRepository;
import org.nmcpye.datarun.common.security.UserFormAccess;
import org.nmcpye.datarun.domain.Authority;
import org.nmcpye.datarun.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Authenticate a user from the database.
 */
@Component("userDetailsService")
public class DomainUserDetailsService implements UserDetailsService {

    private static final Logger LOG = LoggerFactory.getLogger(DomainUserDetailsService.class);

    private final CurrentUserInfoService currentUserInfoService;
    private final UserRepository userRepository;

    public DomainUserDetailsService(CurrentUserInfoService currentUserInfoService, UserRepository userRepository) {
        this.currentUserInfoService = currentUserInfoService;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(final String login) {
        LOG.debug("Authenticating {}", login);

        if (new EmailValidator().isValid(login, null)) {
            return userRepository
                .findOneWithAuthoritiesByEmailIgnoreCase(login)
                .map(user -> createUserDetails(login, user))
                .orElseThrow(() -> new UsernameNotFoundException("User with email " + login + " was not found in the database"));
        }
        String lowercaseLogin = login.toLowerCase(Locale.ENGLISH);
        final var userLogin = userRepository.findOneWithAuthoritiesByLogin(lowercaseLogin);

        return userLogin.map(user -> createUserDetails(lowercaseLogin, user))
            .orElseThrow(() -> new UsernameNotFoundException("User " + lowercaseLogin + " was not found in the database"));
    }

    /////////
    public CurrentUserDetails createUserDetails(String lowercaseLogin, User user) {
        if (!user.isActivated()) {
            throw new UserNotActivatedException("User " + lowercaseLogin + " was not activated");
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

            .teams(userTeamInfo.getTeamUIDs())
            .managedTeams(userTeamInfo.getManagedTeamUIDs())

            .activities(currentUserInfoService
                .getUserActivityInfo(user.getLogin()).getActivityUIDs())


            .userGroups(currentUserInfoService
                .getUserGroupIds(user.getLogin()).getUserGroupUIDs())

            .forms(userFormAccess.stream()
                .map(UserFormAccess::getForm)
                .collect(Collectors.toSet()))

            .formAccess(userFormAccess)

            .build();
    }

//    // old jhipster
//    private org.springframework.security.core.userdetails.User createSpringSecurityUser(String lowercaseLogin, User user) {
//        if (!user.isActivated()) {
//            throw new UserNotActivatedException("User " + lowercaseLogin + " was not activated");
//        }
//        List<SimpleGrantedAuthority> grantedAuthorities = user
//            .getAuthorities()
//            .stream()
//            .map(Authority::getName)
//            .map(SimpleGrantedAuthority::new)
//            .toList();
//
//
//
//        return new org.springframework.security.core.userdetails.User(user.getLogin(), user.getPassword(), grantedAuthorities);
//    }
}
