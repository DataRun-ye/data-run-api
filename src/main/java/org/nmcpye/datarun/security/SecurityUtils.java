package org.nmcpye.datarun.security;

import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Utility class for Spring Security.
 */
public final class SecurityUtils {

    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS512;

    public static final String AUTHORITIES_KEY = "auth";

    private SecurityUtils() {
    }

    public static Optional<CurrentUserDetails> getCurrentUserDetails() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .map(Authentication::getPrincipal)
            .filter(p -> p instanceof CurrentUserDetails)
            .map(CurrentUserDetails.class::cast);
    }

    public static CurrentUserDetails getCurrentUserDetailsOrNull() {
        return getCurrentUserDetails().orElse(null);
    }

    public static CurrentUserDetails getCurrentUserDetailsOrThrow() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .map(Authentication::getPrincipal)
            .filter(p -> p instanceof CurrentUserDetails)
            .map(CurrentUserDetails.class::cast).orElseThrow(() -> new SecurityException("User is not authenticated"));
    }

    public static Optional<String> getCurrentUserLocale() {
        return getCurrentUserDetails().map(CurrentUserDetails::getLangKey);
    }

    public static Optional<Boolean> isCurrentUserActivated() {
        return getCurrentUserDetails().map(CurrentUserDetails::isEnabled);
    }

    /**
     * Get the login of the current user.
     *
     * @return the login of the current user.
     */
    public static Optional<String> getCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(extractPrincipal(securityContext.getAuthentication()));
    }

    /**
     * Get the login of the current user or throw.
     *
     * @param errorMessage message
     * @return the login of the current user.
     */
    public static String getCurrentUserLoginOrThrow(ErrorMessage errorMessage) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(extractPrincipal(securityContext.getAuthentication()))
            .orElseThrow(() -> new SecurityException(errorMessage.getMessage()));
    }

    /**
     * Get the login of the current user.
     *
     * @return the login of the current user.
     */
    public static String getCurrentUserLoginOrThrow() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(extractPrincipal(securityContext.getAuthentication()))
            .orElseThrow(() -> new SecurityException("User is not authenticated"));
    }

    private static String extractPrincipal(Authentication authentication) {
        if (authentication == null) {
            return null;
        } else if (authentication.getPrincipal() instanceof UserDetails springSecurityUser) {
            return springSecurityUser.getUsername();
        } else if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        } else if (authentication.getPrincipal() instanceof String s) {
            return s;
        }
        return null;
    }

    /**
     * Get the JWT of the current user.
     *
     * @return the JWT of the current user.
     */
    public static Optional<String> getCurrentUserJWT() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(securityContext.getAuthentication())
            .filter(authentication -> authentication.getCredentials() instanceof String)
            .map(authentication -> (String) authentication.getCredentials());
    }

    /**
     * Check if a user is authenticated.
     *
     * @return true if the user is authenticated, false otherwise.
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && getAuthorities(authentication).noneMatch(AuthoritiesConstants.ANONYMOUS::equals);
    }

    /**
     * Checks if the current user has any of the authorities.
     *
     * @param authorities the authorities to check.
     * @return true if the current user has any of the authorities, false otherwise.
     */
    public static boolean hasCurrentUserAnyOfAuthorities(String... authorities) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (
            authentication != null && getAuthorities(authentication).anyMatch(authority -> Arrays.asList(authorities).contains(authority))
        );
    }

    /**
     * Checks if the current has super user authorities.
     *
     * @return true if the current user has any of the super user authorities, false otherwise.
     */
    public static boolean isSuper() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (
            authentication != null &&
                getAuthorities(authentication)
                    .anyMatch(authority -> List.of(AuthoritiesConstants.ADMIN).contains(authority))
        );
    }

    /**
     * Checks if the current user has none of the authorities.
     *
     * @param authorities the authorities to check.
     * @return true if the current user has none of the authorities, false otherwise.
     */
    public static boolean hasCurrentUserNoneOfAuthorities(String... authorities) {
        return !hasCurrentUserAnyOfAuthorities(authorities);
    }

    /**
     * Checks if the current user has a specific authority.
     *
     * @param authority the authority to check.
     * @return true if the current user has the authority, false otherwise.
     */
    public static boolean hasCurrentUserThisAuthority(String authority) {
        return hasCurrentUserAnyOfAuthorities(authority);
    }

    private static Stream<String> getAuthorities(Authentication authentication) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority);
    }
}
