package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.drun.postgres.domain.RefreshToken;
import org.nmcpye.datarun.drun.postgres.dto.RefreshTokenDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * @author Hamza Assada, 16/04/2025
 */
@SuppressWarnings("unused")
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
//    Optional<RefreshToken> findByToken(String token);

    <T> Optional<T> findByToken(String lastname, Class<T> type);

    Optional<RefreshTokenDto> findByToken(String token);

    void deleteByUserUid(String userUid);

    void deleteAllByExpiryDateBefore(Instant time);
}
