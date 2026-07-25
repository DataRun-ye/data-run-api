package org.nmcpye.datarun.security.jwt;

import static org.nmcpye.datarun.security.SecurityUtils.AUTHORITIES_KEY;
import static org.nmcpye.datarun.security.SecurityUtils.JWT_ALGORITHM;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.util.Base64;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Collections;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.nmcpye.datarun.jpa.userrefreshtoken.service.TokenService;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.DomainUserDetailsService;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

public class JwtAuthenticationTestUtils {

    public static final String BEARER = "Bearer ";

    @Bean
    private HandlerMappingIntrospector mvcHandlerMappingIntrospector() {
        return new HandlerMappingIntrospector();
    }

    @Bean
    private MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    private DomainUserDetailsService domainUserDetailsService() {
        DomainUserDetailsService service = mock(DomainUserDetailsService.class);
        CurrentUserDetails userDetails = mock(CurrentUserDetails.class);
        when(userDetails.getUsername()).thenReturn("anonymous");
        doReturn(AuthorityUtils.createAuthorityList("ROLE_ADMIN")).when(userDetails).getAuthorities();
        when(service.loadUserByUsername(anyString())).thenReturn(userDetails);
        return service;
    }

    @Bean
    private TokenService tokenService() {
        return mock(TokenService.class);
    }

    public static String createValidToken(String jwtKey) {
        return createValidTokenForUser(jwtKey, "anonymous");
    }

    public static String createValidTokenForUser(String jwtKey, String user) {
        JwtEncoder encoder = jwtEncoder(jwtKey);

        var now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(now.plusSeconds(60))
            .subject(user)
            .claims(customClaim -> customClaim.put(AUTHORITIES_KEY, Collections.singletonList("ROLE_ADMIN")))
            .build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return encoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public static String createTokenWithDifferentSignature() {
        JwtEncoder encoder = jwtEncoder("Xfd54a45s65fds737b9aafcb3412e07ed99b267f33413274720ddbb7f6c5e64e9f14075f2d7ed041592f0b7657baf8");

        var now = Instant.now();
        var past = now.plusSeconds(60);

        JwtClaimsSet claims = JwtClaimsSet.builder().issuedAt(now).expiresAt(past).subject("anonymous").build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return encoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public static String createExpiredToken(String jwtKey) {
        JwtEncoder encoder = jwtEncoder(jwtKey);

        var now = Instant.now();
        var past = now.minusSeconds(600);

        JwtClaimsSet claims = JwtClaimsSet.builder().issuedAt(past).expiresAt(past.plusSeconds(1)).subject("anonymous").build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return encoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public static String createInvalidToken(String jwtKey) {
        return createValidToken(jwtKey).substring(1);
    }

    public static String createSignedInvalidJwt(String jwtKey) throws Exception {
        return calculateHMAC("foo", jwtKey);
    }

    private static JwtEncoder jwtEncoder(String jwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(getSecretKey(jwtKey)));
    }

    private static SecretKey getSecretKey(String jwtKey) {
        byte[] keyBytes = Base64.from(jwtKey).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, JWT_ALGORITHM.getName());
    }

    private static String calculateHMAC(String data, String key) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(Base64.from(key).decode(), "HmacSHA512");
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(secretKeySpec);
        return String.copyValueOf(Hex.encode(mac.doFinal(data.getBytes())));
    }
}
