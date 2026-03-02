package org.nmcpye.datarun.config.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.util.Base64;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.management.SecurityMetersService;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.DomainUserDetailsService;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.security.oauth2.AudienceValidator;
import org.nmcpye.datarun.web.filter.SpaWebFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.*;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.util.StringUtils;
import tech.jhipster.config.JHipsterProperties;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.nmcpye.datarun.security.SecurityUtils.JWT_ALGORITHM;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.PREFERRED_USERNAME;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfiguration {
    private final DomainUserDetailsService userDetailsService;

    private final JHipsterProperties jHipsterProperties;
    @Value("${jhipster.security.authentication.jwt.base64-secret}")
    private String jwtKey;

    @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}")
    private String issuerUri;

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(getSecretKey()));
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(jwtKey).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, JWT_ALGORITHM.getName());
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return (context) -> {
            // We only want to customize the Access Token (not the refresh token or ID token)
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {

                Authentication principal = context.getPrincipal();

                // 1. Replicate our exact authority mapping
                String authorities = principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(" "));

                // 2. Add the authorities to the JWT payload
                context.getClaims().claim(SecurityUtils.AUTHORITIES_KEY, authorities);

                // --- FUTURE PROOFING (Optional but recommended) ---
                // Since our CustomJwtAuthenticationConverter hits the DB on every request
                // to load the user's UID and Teams, we can inject them directly into the token here!
                if (principal.getPrincipal() instanceof CurrentUserDetails user) {
                    context.getClaims().claim("id", user.getId());
                    context.getClaims().claim("uid", user.getUid());
                    context.getClaims().claim("teams", user.getUserTeamsUIDs());
                    context.getClaims().claim("templates", user.getUserFormsUIDs());
                    context.getClaims().claim("user_groups", user.getUserGroupsUIDs());
                    context.getClaims().claim("activities", user.getActivityUIDs());
                    context.getClaims().claim("managed_teams", user.getManagedTeamsUIDs());
                    context.getClaims().claim("langKey", user.getLangKey());
                }
            }
        };
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain filterChain(HttpSecurity http, MvcRequestMatcher.Builder mvc) throws Exception {
        OAuth2AuthorizationServerConfigurer authServerConfigurer =
            OAuth2AuthorizationServerConfigurer.authorizationServer();

        http
            .cors(withDefaults())
            .csrf(csrf ->
                csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
            )
            .addFilterAfter(new SpaWebFilter(), BasicAuthenticationFilter.class)
            .securityMatcher(authServerConfigurer.getEndpointsMatcher())
            .headers(headers ->
                headers
                    .contentSecurityPolicy(csp -> csp.policyDirectives(jHipsterProperties.getSecurity().getContentSecurityPolicy()))
                    .frameOptions(FrameOptionsConfig::sameOrigin)
                    .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    .permissionsPolicyHeader(permissions ->
                        permissions.policy(
                            "camera=(), fullscreen=(self), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), midi=(), payment=(), sync-xhr=()"
                        )
                    )
            )
            .with(authServerConfigurer, (authorizationServer) ->
                authorizationServer.oidc(withDefaults())
            )
//            .authorizeHttpRequests(authz ->
//                // prettier-ignore
//                authz
//                    .requestMatchers(mvc.pattern("/index.html"), mvc.pattern("/*.js"), mvc.pattern("/*.txt"), mvc.pattern("/*.json"), mvc.pattern("/*.map"), mvc.pattern("/*.css")).permitAll()
//                    .requestMatchers(mvc.pattern("/*.ico"), mvc.pattern("/*.png"), mvc.pattern("/*.svg"), mvc.pattern("/*.webapp")).permitAll()
//                    .requestMatchers(mvc.pattern("/app/**")).permitAll()
//                    .requestMatchers(mvc.pattern("/i18n/**")).permitAll()
//                    .requestMatchers(mvc.pattern("/content/**")).permitAll()
////                    .requestMatchers(mvc.pattern("/swagger-ui/**")).permitAll()
////                    .requestMatchers(mvc.pattern("/api/authenticate")).permitAll()
////                    .requestMatchers(mvc.pattern("/api/auth-info")).permitAll()
////                    .requestMatchers(mvc.pattern("/api/admin/**")).hasAuthority(AuthoritiesConstants.ADMIN)
////                    .requestMatchers(mvc.pattern("/api/**")).authenticated()
////                    .requestMatchers(mvc.pattern("/v3/api-docs/**")).hasAuthority(AuthoritiesConstants.ADMIN)
////                    .requestMatchers(mvc.pattern("/management/health")).permitAll()
////                    .requestMatchers(mvc.pattern("/management/health/**")).permitAll()
////                    .requestMatchers(mvc.pattern("/management/info")).permitAll()
////                    .requestMatchers(mvc.pattern("/management/prometheus")).permitAll()
////                    .requestMatchers(mvc.pattern("/management/**")).hasAuthority(AuthoritiesConstants.ADMIN)
//            )
            .formLogin(Customizer.withDefaults())
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/"))
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            /*.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(new CustomJwtAuthenticationConverter(userDetailsService))
                )
            )*/;
        return http.build();
    }

    Converter<Jwt, AbstractAuthenticationToken> authenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
            jwt -> SecurityUtils.extractAuthorityFromClaims(jwt.getClaims())
        );
        jwtAuthenticationConverter.setPrincipalClaimName(PREFERRED_USERNAME);
        return jwtAuthenticationConverter;
    }


    @Bean
    public JwtDecoder jwtDecoder(SecurityMetersService metersService) {
        // 1. The NEW Asymmetric Decoder (Validates tokens from Spring Auth Server)
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(issuerUri).build();
        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(
            jHipsterProperties.getSecurity().getOauth2().getAudience());
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);

        jwtDecoder.setJwtValidator(withAudience);

        // 2. The LEGACY Symmetric Decoder (Validates your current homegrown tokens)
        NimbusJwtDecoder legacyDecoder = NimbusJwtDecoder.withSecretKey(getSecretKey())
            .macAlgorithm(JWT_ALGORITHM)
            .build();
        return token -> {
            try {
                // Try new standard first
                return jwtDecoder.decode(token);
            } catch (Exception newEx) {
                // If it fails, fallback to the legacy decoder
                try {
                    return legacyDecoder.decode(token);
                } catch (Exception e) {
                    // Your existing robust error tracking logic
                    if (e.getMessage().contains("Invalid signature")) {
                        metersService.trackTokenInvalidSignature();
                    } else if (e.getMessage().contains("Jwt expired at")) {
                        metersService.trackTokenExpired();
                    } else if (
                        e.getMessage().contains("Invalid JWT serialization") ||
                            e.getMessage().contains("Malformed token") ||
                            e.getMessage().contains("Invalid unsecured/JWS/JWE")
                    ) {
                        metersService.trackTokenMalformed();
                    } else {
                        log.error("Unknown JWT error {}", e.getMessage());
                    }
                    throw e; // Fails authentication completely
                }
            }
        };
    }


    /**
     * Custom CSRF handler to provide BREACH protection for Single-Page Applications (SPA).
     *
     * @see <a href="https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html#csrf-integration-javascript-spa">Spring Security Documentation - Integrating with CSRF Protection</a>
     * @see <a href="https://github.com/jhipster/generator-jhipster/pull/25907">JHipster - use customized SpaCsrfTokenRequestHandler to handle CSRF token</a>
     * @see <a href="https://stackoverflow.com/q/74447118/65681">CSRF protection not working with Spring Security 6</a>
     */
    static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
            /*
             * Always use XorCsrfTokenRequestAttributeHandler to provide BREACH protection of
             * the CsrfToken when it is rendered in the response body.
             */
            this.xor.handle(request, response, csrfToken);

            // Render the token value to a cookie by causing the deferred token to be loaded.
            csrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            /*
             * If the request contains a request header, use CsrfTokenRequestAttributeHandler
             * to resolve the CsrfToken. This applies when a single-page application includes
             * the header value automatically, which was obtained via a cookie containing the
             * raw CsrfToken.
             */
            if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
                return this.plain.resolveCsrfTokenValue(request, csrfToken);
            }
            /*
             * In all other cases (e.g. if the request contains a request parameter), use
             * XorCsrfTokenRequestAttributeHandler to resolve the CsrfToken. This applies
             * when a server-side rendered form includes the _csrf request parameter as a
             * hidden input.
             */
            return this.xor.resolveCsrfTokenValue(request, csrfToken);
        }
    }
}
