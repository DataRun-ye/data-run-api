package org.nmcpye.datarun.web.rest.postgres.authenticate;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import org.nmcpye.datarun.common.repository.UserRepository;
import org.nmcpye.datarun.domain.Authority;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.web.rest.common.UserTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.stream.Collectors;

import static org.nmcpye.datarun.security.SecurityUtils.AUTHORITIES_KEY;
import static org.nmcpye.datarun.security.SecurityUtils.JWT_ALGORITHM;

@RestController
@RequestMapping("/api/custom")
public class AuthenticateBasicResource {

    private final Logger log = LoggerFactory.getLogger(AuthenticateBasicResource.class);

    private final JwtEncoder jwtEncoder;

    @Value("${jhipster.security.authentication.jwt.token-validity-in-seconds:0}")
    private long tokenValidityInSeconds;

    @Value("${jhipster.security.authentication.jwt.token-validity-in-seconds-for-remember-me:0}")
    private long tokenValidityInSecondsForRememberMe;

    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    private final UserRepository userRepository;

    public AuthenticateBasicResource(JwtEncoder jwtEncoder,
                                     AuthenticationManagerBuilder authenticationManagerBuilder,
                                     UserRepository userRepository) {
        this.jwtEncoder = jwtEncoder;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.userRepository = userRepository;
    }

    @GetMapping("/authenticateBasic")
    public ResponseEntity<UserTokenResponse> authorize(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Basic ")) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String base64Credentials = header.substring(6);
        String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
        String[] values = credentials.split(":", 2);
        if (values.length != 2) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String username = values[0];
        String password = values[1];

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        boolean rememberMe = false; // Or derive this from your logic
        String jwt = this.createToken(authentication, rememberMe);

        // Fetch user information
        User user = userRepository.findOneWithAuthoritiesByLogin(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Create response object
        UserTokenResponse userWithToken = new UserTokenResponse();
        userWithToken.setId(user.getId());
        userWithToken.setUid(user.getUid());
        userWithToken.setLogin(user.getLogin());
        userWithToken.setFirstName(user.getFirstName());
        userWithToken.setLastName(user.getLastName());
        userWithToken.setEmail(user.getEmail());
        userWithToken.setImageUrl(user.getImageUrl());
        userWithToken.setActivated(user.isActivated());
        userWithToken.setLangKey(user.getLangKey());
        userWithToken.setAuthorities(user.getAuthorities()
            .stream().map(Authority::getName).collect(Collectors.toSet()));
//        userWithToken.setToken(jwt);
//        userWithToken.setAuthType("bearer");

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(jwt);
        return new ResponseEntity<>(userWithToken, httpHeaders, HttpStatus.OK);
    }

    private String createToken(Authentication authentication, boolean rememberMe) {
        String authorities = authentication
            .getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(" "));

        Instant now = Instant.now();
        Instant validity;
        if (rememberMe) {
            validity = now.plus(this.tokenValidityInSecondsForRememberMe, ChronoUnit.SECONDS);
        } else {
            validity = now.plus(this.tokenValidityInSeconds, ChronoUnit.SECONDS);
        }

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(validity)
            .subject(authentication.getName())
            .claim(AUTHORITIES_KEY, authorities)
            .build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    static class JWTToken {

        private String idToken;

        JWTToken(String idToken) {
            this.idToken = idToken;
        }

        @JsonProperty("token")
        String getIdToken() {
            return idToken;
        }

        void setIdToken(String idToken) {
            this.idToken = idToken;
        }
    }
}

