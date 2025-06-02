package org.nmcpye.datarun.jpa.userrefreshtoken.service;

import org.nmcpye.datarun.jpa.user.repository.UserRepository;
import org.nmcpye.datarun.jpa.userauthority.Authority;
import org.nmcpye.datarun.jpa.userrefreshtoken.RefreshToken;
import org.nmcpye.datarun.jpa.userrefreshtoken.TokenRefreshException;
import org.nmcpye.datarun.jpa.userrefreshtoken.dto.RefreshTokenDto;
import org.nmcpye.datarun.jpa.userrefreshtoken.repository.RefreshTokenRepository;
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

    private final RefreshTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JwtEncoder jwtEncoder;

    @Value("${datarun.security.authentication.jwt.token-validity-in-seconds:600}")
    private long accessTokenValidity;

    @Value("${datarun.security.authentication.jwt.refresh-token-validity-in-seconds:2592000}") // 30 days
    private long refreshTokenValidity;

    public TokenService(RefreshTokenRepository tokenRepository,
                        JwtEncoder jwtEncoder,
                        UserRepository userRepository) {
        this.tokenRepository = tokenRepository;
        this.jwtEncoder = jwtEncoder;
        this.userRepository = userRepository;
    }

    public Optional<RefreshTokenDto> findDtoByToken(String token) {
        return tokenRepository.findByToken(token);
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
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public RefreshTokenDto createRefreshToken(String username) {
        final var user = userRepository
            .findOneWithAuthoritiesByLogin(username)
            .orElseThrow(() -> new TokenRefreshException(username, "invalid"));
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plus(refreshTokenValidity, ChronoUnit.SECONDS));
        refreshToken.setToken(UUID.randomUUID().toString());

        return Optional.of(tokenRepository.save(refreshToken))
            .map(RefreshTokenDto::new).orElseThrow();
    }

    @Transactional(readOnly = true)
    public Optional<RefreshTokenDto> verifyRefreshToken(String token) {
        return tokenRepository.findByToken(token)
            .filter(t -> !t.isExpired());
    }

    /**
     * Invalidate the current refresh token and issue a new one.
     */
    public RefreshTokenDto rotateRefreshToken(RefreshTokenDto refreshToken) {
        tokenRepository.deleteByUserUid(refreshToken.getUser().getUid());
        return createRefreshToken(refreshToken.getUser().getLogin());
    }

    /**
     * Delete expired tokens, runs daily at midnight
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanupExpiredTokens() {
        tokenRepository.deleteAllByExpiryDateBefore(Instant.now());
    }
}
