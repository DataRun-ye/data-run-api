package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.common.repository.UserRepository;
import org.nmcpye.datarun.domain.Authority;
import org.nmcpye.datarun.drun.postgres.domain.RefreshToken;
import org.nmcpye.datarun.drun.postgres.repository.RefreshTokenRepository;
import org.nmcpye.datarun.security.datarun.TokenRefreshException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.nmcpye.datarun.security.SecurityUtils.AUTHORITIES_KEY;
import static org.nmcpye.datarun.security.SecurityUtils.JWT_ALGORITHM;

/**
 * @author Hamza Assada, 16/04/2025
 */
@Service
@Transactional
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtEncoder jwtEncoder;

    @Value("${datarun.security.authentication.jwt.token-validity-in-seconds:600}")
    private long accessTokenValidity;

    @Value("${datarun.security.authentication.jwt.refresh-token-validity-in-seconds:2592000}") // 30 days
    private long refreshTokenValidity;

    public TokenService(RefreshTokenRepository refreshTokenRepository,
                        JwtEncoder jwtEncoder,
                        UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtEncoder = jwtEncoder;
        this.userRepository = userRepository;
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional(readOnly = true)
    public String generateAccessToken(String username) {
        Instant now = Instant.now();
        final var user = userRepository
            .findOneWithAuthoritiesByLogin(username)
            .orElseThrow(() -> new TokenRefreshException(username, "invalid"));
        String authorities = user.getAuthorities().stream()
            .map(Authority::getName)
            .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(now.plus(accessTokenValidity, ChronoUnit.SECONDS))
            .subject(user.getLogin())
            .claim(AUTHORITIES_KEY, authorities)
            .build();
        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader,claims)).getTokenValue();
    }

    public RefreshToken createRefreshToken(String username) {
        final var user = userRepository
            .findOneWithAuthoritiesByLogin(username)
            .orElseThrow(() -> new TokenRefreshException(username, "invalid"));
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plus(refreshTokenValidity, ChronoUnit.SECONDS));
        refreshToken.setToken(UUID.randomUUID().toString());

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshToken verifyRefreshToken(String token) {
        return refreshTokenRepository.findByToken(token)
            .filter(t -> !t.isExpired())
            .orElseThrow(() -> new TokenRefreshException(token, "Invalid refresh token"));
    }

    /**
     * Invalidate the current refresh token and issue a new one.
     */
    public RefreshToken rotateRefreshToken(RefreshToken refreshToken) {
        refreshTokenRepository.delete(refreshToken);
        return createRefreshToken(refreshToken.getUser().getLogin());
    }

    /**
     * Delete expired tokens, runs daily at midnight
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteAllByExpiryDateBefore(Instant.now());
    }
}
