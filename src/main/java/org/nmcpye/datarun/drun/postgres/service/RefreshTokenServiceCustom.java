package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.domain.RefreshToken;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.service.RefreshTokenService;

import java.util.Optional;

/**
 * Service Interface for managing {@link RefreshToken}.
 */
public interface RefreshTokenServiceCustom extends RefreshTokenService {
    Optional<RefreshToken> findByToken(String token);

    RefreshToken createRefreshToken(User user);

    RefreshToken verifyExpiration(RefreshToken token);

    void deleteByUserLogin(String login);
}
