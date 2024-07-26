package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.domain.RefreshToken;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.repository.RefreshTokenRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Custom Data JPA repository for the RefreshToken entity.
 */
@Repository
public interface RefreshTokenRepositoryCustom extends RefreshTokenRepository {
    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);
}
