package org.nmcpye.datarun.config.datarun;

import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import tech.jhipster.config.JHipsterProperties;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class DataRunSecurityConfig {

    private final JHipsterProperties jHipsterProperties;

    public DataRunSecurityConfig(JHipsterProperties jHipsterProperties) {
        this.jHipsterProperties = jHipsterProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http, MvcRequestMatcher.Builder mvc,
                                           AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
        http
            .cors(withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(
                authz ->
                    // prettier-ignore
                    authz
                        .requestMatchers(mvc.pattern(HttpMethod.POST, "/api/authenticate")).permitAll()
                        .requestMatchers(mvc.pattern(HttpMethod.GET, "/api/authenticate")).permitAll()

                        // Data Run Added
                        // For basic Auth (Basic username:password) in header
                        .requestMatchers(mvc.pattern(HttpMethod.POST, "/api/custom/authenticateBasic")).permitAll()
                        .requestMatchers(mvc.pattern(HttpMethod.GET, "/api/custom/authenticateBasic")).permitAll()
                        .requestMatchers(mvc.pattern("/api/custom/register")).permitAll()
                        .requestMatchers(mvc.pattern("/api/custom/activate")).permitAll()
                        .requestMatchers(mvc.pattern("/api/custom/me/reset-password/init")).permitAll()
                        .requestMatchers(mvc.pattern("/api/custom/me/reset-password/finish")).permitAll()

                        .requestMatchers(mvc.pattern("/api/register")).permitAll()
                        .requestMatchers(mvc.pattern("/api/activate")).permitAll()
                        .requestMatchers(mvc.pattern("/api/account/reset-password/init")).permitAll()
                        .requestMatchers(mvc.pattern("/api/account/reset-password/finish")).permitAll()
                        .requestMatchers(mvc.pattern("/api/admin/**")).hasAuthority(AuthoritiesConstants.ADMIN)
                        .requestMatchers(mvc.pattern("/api/**")).authenticated()
                        .requestMatchers(mvc.pattern("/v3/api-docs/**")).hasAuthority(AuthoritiesConstants.ADMIN)
                        .requestMatchers(mvc.pattern("/management/health")).permitAll()
                        .requestMatchers(mvc.pattern("/management/health/**")).permitAll()
                        .requestMatchers(mvc.pattern("/management/info")).permitAll()
                        .requestMatchers(mvc.pattern("/management/prometheus")).permitAll()
                        .requestMatchers(mvc.pattern("/management/**")).hasAuthority(AuthoritiesConstants.ADMIN)
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(
                exceptions ->
                    exceptions
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults())).addFilterBefore(
                new CustomUsernamePasswordAuthenticationFilter
                    (/*authenticationManagerBuilder.getObject()*/), UsernamePasswordAuthenticationFilter.class)
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    MvcRequestMatcher.Builder mvc(HandlerMappingIntrospector introspector) {
        return new MvcRequestMatcher.Builder(introspector);
    }

    // Data Run
//    @Autowired
//    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
//        auth
//            .authenticationProvider(basicAuthenticationProvider());
//    }

//    @Bean
//    public AuthenticationProvider basicAuthenticationProvider() {
//        return new DaoAuthenticationProvider();
//    }

//    @Bean
//    @Order(1)
//    public SecurityFilterChain basicFilterChain(
//        HttpSecurity http, MvcRequestMatcher.Builder mvc,
//        AuthenticationManagerBuilder authenticationManagerBuilder
//
//    ) throws Exception {
//        http
//            .cors(withDefaults())
//            .csrf(csrf -> csrf.disable())
//            .authorizeHttpRequests(
//                authz ->
//                    authz
//                        .requestMatchers(mvc.pattern(HttpMethod.POST, "/api/authenticate")).permitAll()
//                        .requestMatchers(mvc.pattern(HttpMethod.GET, "/api/authenticate")).permitAll()
//
//                        // Data Run Added
//                        // For basic Auth (Basic username:password) in header
//                        .requestMatchers(mvc.pattern(HttpMethod.POST, "/api/custom/authenticateBasic")).permitAll()
//                        .requestMatchers(mvc.pattern(HttpMethod.GET, "/api/custom/authenticateBasic")).permitAll()
//                        .requestMatchers(mvc.pattern("/api/custom/**")).authenticated()
//                        .requestMatchers(mvc.pattern("/api/register")).permitAll()
//                        .requestMatchers(mvc.pattern("/api/activate")).permitAll()
//                        .requestMatchers(mvc.pattern("/api/account/reset-password/init")).permitAll()
//                        .requestMatchers(mvc.pattern("/api/account/reset-password/finish")).permitAll()
//                        .requestMatchers(mvc.pattern("/api/admin/**")).hasAuthority(AuthoritiesConstants.ADMIN)
//                        .requestMatchers(mvc.pattern("/api/**")).authenticated()
//                        .requestMatchers(mvc.pattern("/v3/api-docs/**")).hasAuthority(AuthoritiesConstants.ADMIN)
//                        .requestMatchers(mvc.pattern("/management/health")).permitAll()
//                        .requestMatchers(mvc.pattern("/management/health/**")).permitAll()
//                        .requestMatchers(mvc.pattern("/management/info")).permitAll()
//                        .requestMatchers(mvc.pattern("/management/prometheus")).permitAll()
//                        .requestMatchers(mvc.pattern("/management/**")).hasAuthority(AuthoritiesConstants.ADMIN)
//            )
//            .addFilterBefore(
//                new CustomBasicAuthenticationFilter
//                    (authenticationManagerBuilder.getObject()), UsernamePasswordAuthenticationFilter.class)
//            .httpBasic(Customizer.withDefaults());
//        return http.build();
//    }

}
