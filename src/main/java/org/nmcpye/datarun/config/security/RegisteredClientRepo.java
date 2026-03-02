package org.nmcpye.datarun.config.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Configuration
@Slf4j
public class RegisteredClientRepo {
    @Bean
    @Transactional
    public RegisteredClientRepository registeredClientRepository(
        JdbcTemplate jdbcTemplate,
        PasswordEncoder passwordEncoder) {

        // 1. Initialize the JDBC-backed repository
        JdbcRegisteredClientRepository repository = new JdbcRegisteredClientRepository(jdbcTemplate);
        // 2. Seed the database with your initial client if it doesn't exist yet
        String clientId = "data-run-web";
        String angularClientId =  "datarun-angular-client";
        if (repository.findByClientId(angularClientId) == null) {
            RegisteredClient publicClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(angularClientId)
                // NO client secret for public clients!
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
//                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
//                .clientSecret(passwordEncoder.encode("secret"))
                // Redirect URI for Angular dev server

                .redirectUri("http://localhost:4200/callback")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .clientSettings(ClientSettings.builder()
                    .requireProofKey(true) // This enables PKCE
                    .requireAuthorizationConsent(true)
                    .build())
                .build();
            repository.save(publicClient);
        }
        if (repository.findByClientId(clientId) == null) {
            RegisteredClient webClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret(passwordEncoder.encode("web-app-secret")) // Store encoded!
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://127.0.0.1:8080/login/oauth2/code/data-run")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .tokenSettings(TokenSettings.builder()
                    .accessTokenTimeToLive(Duration.ofSeconds(86400))
                    .refreshTokenTimeToLive(Duration.ofSeconds(2592000))
                    .reuseRefreshTokens(false)
                    .build())
                .build();

            // Save it to the database automatically on startup
            repository.save(webClient);
            log.info("Initialized default OAuth2 client: {}", clientId);
        }

        return repository;
    }
}
