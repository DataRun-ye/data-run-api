package org.nmcpye.datarun.security;

import org.hibernate.validator.internal.constraintvalidators.bv.EmailValidator;
import org.nmcpye.datarun.jpa.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Authenticate a user from the database.
 */
@Component("userDetailsService")
public class DomainUserDetailsService implements UserDetailsService {

    private static final Logger LOG = LoggerFactory.getLogger(DomainUserDetailsService.class);

    private final UserRepository userRepository;
    private final CurrentUserDetailsService currentUserDetailsService;

    public DomainUserDetailsService(UserRepository userRepository, CurrentUserDetailsService currentUserDetailsService) {
        this.userRepository = userRepository;
        this.currentUserDetailsService = currentUserDetailsService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(final String login) {
        LOG.debug("Authenticating {}", login);

        if (new EmailValidator().isValid(login, null)) {
            return userRepository
                .findOneWithAuthoritiesByEmailIgnoreCase(login)
                .map(currentUserDetailsService::createUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("User with email " + login + " was not found in the database"));
        }
        String lowercaseLogin = login.toLowerCase(Locale.ENGLISH);
        final var userLogin = userRepository.findOneWithAuthoritiesByLogin(lowercaseLogin);

        return userLogin.map(currentUserDetailsService::createUserDetails)
            .orElseThrow(() -> new UsernameNotFoundException("User " + lowercaseLogin + " was not found in the database"));
    }
}
