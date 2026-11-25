package org.nmcpye.datarun.web.v2.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.nmcpye.datarun.config.datarun.CustomJwtAuthenticationConverter;
import org.nmcpye.datarun.security.DomainUserDetailsService;
import org.nmcpye.datarun.web.v2.ApiV2;
import org.nmcpye.datarun.web.v2.problem.Problem;
import org.nmcpye.datarun.web.v2.problem.ProblemCode;
import org.nmcpye.datarun.web.v2.problem.ProblemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * The security chain of {@code /api/v2/**}, separate from the application's main chain, which it leaves as it is:
 * <ul>
 *   <li>every v2 route requires an authenticated caller; finer decisions belong to each module's own policy, never
 *       to a URL rule here;</li>
 *   <li>the only credential is the bearer JWT that {@code POST /api/v1/authenticate} issues, read as for v1: the
 *       token names the account, and the request's authority comes from the account's current database state,
 *       so a deactivated account or a changed role takes effect at once. HTTP Basic, which the main chain still
 *       accepts for operational compatibility, is not part of v2;</li>
 *   <li>a refusal at this layer is a v2 problem too: 401 {@code unauthenticated} with the bearer
 *       {@code WWW-Authenticate} challenge, 403 {@code forbidden}. These answers log nothing: a missing token is
 *       the client's, and a rejected bearer token is no failed login (AuthenticationFailureListener).</li>
 * </ul>
 * It comes first among the chains and matches only v2 paths, so every other request still meets the main chain.
 */
@Configuration
public class V2SecurityConfiguration {

    static final String REJECTED_TOKEN_DETAIL =
        "The bearer token was rejected: it is invalid or expired, or its account cannot sign in.";

    @Bean
    @Order(1)
    public SecurityFilterChain v2SecurityFilterChain(HttpSecurity http,
                                                     DomainUserDetailsService userDetailsService,
                                                     ProblemWriter problems) throws Exception {
        AuthenticationEntryPoint unauthenticated = new ProblemAuthenticationEntryPoint(problems);
        AccessDeniedHandler forbidden = new ProblemAccessDeniedHandler(problems);
        http
            // The one definition of a v2 request, also the problem resolver's: the decoded path, as MVC routes.
            .securityMatcher(ApiV2::isV2)
            .cors(withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(new CustomJwtAuthenticationConverter(userDetailsService)))
                .authenticationEntryPoint(unauthenticated)
                .accessDeniedHandler(forbidden))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(unauthenticated)
                .accessDeniedHandler(forbidden));
        return http.build();
    }

    /** 401 with the bearer challenge (RFC 6750), and the problem as body. */
    static final class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

        private final BearerTokenAuthenticationEntryPoint challenge = new BearerTokenAuthenticationEntryPoint();
        private final ProblemWriter problems;

        ProblemAuthenticationEntryPoint(ProblemWriter problems) {
            this.problems = problems;
        }

        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response,
                             AuthenticationException exception) throws IOException {
            challenge.commence(request, response, exception);
            // No credential at all reaches here as InsufficientAuthenticationException; anything else is a token
            // that was read and refused, for itself or for its account (unknown, deactivated).
            String detail = exception instanceof InsufficientAuthenticationException
                ? "Send a bearer token from POST /api/v1/authenticate in the Authorization header."
                : REJECTED_TOKEN_DETAIL;
            Problem problem = problems.problem(request, response, ProblemCode.UNAUTHENTICATED, detail, List.of(),
                Map.of());
            problems.write(response, problem, null);
        }
    }

    static final class ProblemAccessDeniedHandler implements AccessDeniedHandler {

        private final ProblemWriter problems;

        ProblemAccessDeniedHandler(ProblemWriter problems) {
            this.problems = problems;
        }

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response,
                           AccessDeniedException exception) throws IOException {
            Problem problem = problems.problem(request, response, ProblemCode.FORBIDDEN,
                "This account may not make this request.", List.of(), Map.of());
            problems.write(response, problem, null);
        }
    }
}
