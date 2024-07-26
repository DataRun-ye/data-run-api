package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.domain.RefreshToken;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.drun.postgres.repository.RefreshTokenRepositoryCustom;
import org.nmcpye.datarun.repository.UserRepository;
import org.nmcpye.datarun.security.datarun.TokenRefreshException;
import org.nmcpye.datarun.drun.postgres.service.RefreshTokenServiceCustom;
import org.nmcpye.datarun.service.impl.RefreshTokenServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Primary
@Transactional
public class RefreshTokenServiceCustomImpl
    extends RefreshTokenServiceImpl
    implements RefreshTokenServiceCustom {

    @Value("${app.jwtRefreshExpirationMs: 2592000}")
    private Long refreshTokenDurationMs;

    final private RefreshTokenRepositoryCustom refreshTokenRepository;

    private final UserRepository userRepository;

    public RefreshTokenServiceCustomImpl(RefreshTokenRepositoryCustom refreshTokenRepository, UserRepository userRepository) {
        super(refreshTokenRepository);
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString());

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new login request");
        }

        return token;
    }

    @Override
    public void deleteByUserLogin(String login) {
        User user = userRepository.findOneByLogin(login).orElseThrow(() -> new RuntimeException("User not found"));
        refreshTokenRepository.deleteByUser(user);
    }
}
