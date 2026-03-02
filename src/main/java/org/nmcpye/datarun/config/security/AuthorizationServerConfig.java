package org.nmcpye.datarun.config.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.config.datarun.DatarunProperties;
import org.nmcpye.datarun.security.DomainUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class AuthorizationServerConfig {
    private final DomainUserDetailsService userDetailsService;

    @Bean
    public JWKSource<SecurityContext> jwkSource(DatarunProperties properties) {
        try {
            // 1. Decode the keys from your YML properties
            byte[] privateKeyBytes = Base64.getDecoder().decode(properties.getSecurity().getAuthentication().getJwt().getRsaPrivateKey());
            byte[] publicKeyBytes = Base64.getDecoder().decode(properties.getSecurity().getAuthentication().getJwt().getRsaPublicKey());

            // 2. Convert to RSA Key Objects
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            // 3. Build the JWK
            RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID("datarun-key-1") // Use a static ID so it doesn't change
                .build();

            return new ImmutableJWKSet<>(new JWKSet(rsaKey));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load RSA keys from properties", e);
        }
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(
        JdbcTemplate jdbcTemplate,
        RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }
}
