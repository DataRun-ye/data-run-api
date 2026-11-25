package org.nmcpye.datarun.web.v2.caller;

import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.v2.problem.ApiProblemException;
import org.nmcpye.datarun.web.v2.problem.ProblemCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Who makes a v2 request: the account's durable user id and its current authorities, as the v2 chain loaded them
 * from the database for this request. A v2 handler takes it as a parameter, and a command receives it in its
 * context.
 * <p>
 * It is the seam for access decisions, not a decision: a module's policy (by action and scope, designed with the
 * domain glossary) reads the caller and the resource and decides. Records name the caller by {@link #userId},
 * never by login.
 *
 * @param userId      the account's id ({@code app_user.id})
 * @param authorities the account's current authorities, such as {@code ROLE_ADMIN}
 */
public record ApiCaller(String userId, Set<String> authorities) {

    public ApiCaller {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("A caller has a user id");
        }
        authorities = Set.copyOf(authorities);
    }

    public boolean hasAuthority(String authority) {
        return authorities.contains(authority);
    }

    /**
     * The caller of the current request.
     *
     * @throws ApiProblemException 401 {@code unauthenticated} when the request carries no account the v2 chain
     *                             loaded
     */
    public static ApiCaller current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
            && authentication.getPrincipal() instanceof CurrentUserDetails user && user.getId() != null) {
            return new ApiCaller(user.getId(), user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet()));
        }
        throw ApiProblemException.of(ProblemCode.UNAUTHENTICATED)
            .detail("Send a bearer token from POST /api/v1/authenticate in the Authorization header.")
            .header("WWW-Authenticate", "Bearer");
    }
}
