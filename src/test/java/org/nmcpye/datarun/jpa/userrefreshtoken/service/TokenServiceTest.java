package org.nmcpye.datarun.jpa.userrefreshtoken.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.jpa.user.User;
import org.nmcpye.datarun.jpa.user.repository.UserRepository;
import org.nmcpye.datarun.jpa.userrefreshtoken.RefreshToken;
import org.nmcpye.datarun.jpa.userrefreshtoken.TokenRefreshException;
import org.nmcpye.datarun.jpa.userrefreshtoken.repository.RefreshTokenRepository;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class TokenServiceTest {

    private final RefreshTokenRepository tokenRepository =
        mock(RefreshTokenRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtEncoder jwtEncoder = mock(JwtEncoder.class);

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(
            tokenRepository,
            userRepository,
            jwtEncoder
        );
    }

    @Test
    void doesNotIssueAccessTokenForDeactivatedUser() {
        User user = user("field-user", false);
        when(userRepository.findOneWithAuthoritiesByLogin("field-user"))
            .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> tokenService.generateAccessToken("field-user"))
            .isInstanceOf(TokenRefreshException.class);

        verifyNoInteractions(jwtEncoder);
    }

    @Test
    void doesNotCreateRefreshTokenForDeactivatedUser() {
        User user = user("field-user", false);
        when(userRepository.findOneWithAuthoritiesByLogin("field-user"))
            .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> tokenService.createRefreshToken("field-user"))
            .isInstanceOf(TokenRefreshException.class);

        verify(tokenRepository, never()).save(
            org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void doesNotRotateRefreshTokenForDeactivatedUser() {
        User user = user("field-user", false);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");
        refreshToken.setExpiryDate(Instant.now().plusSeconds(60));
        refreshToken.setUser(user);
        when(tokenRepository.findByToken("refresh-token", RefreshToken.class))
            .thenReturn(Optional.of(refreshToken));
        when(userRepository.findOneWithAuthoritiesByLogin("field-user"))
            .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> tokenService.rotateRefreshToken("refresh-token"))
            .isInstanceOf(TokenRefreshException.class);

        verify(tokenRepository, never()).deleteByUserUid(user.getUid());
        verify(tokenRepository, never()).save(
            org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void rotatesRefreshTokenForActiveUser() {
        User user = user("field-user", true);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");
        refreshToken.setExpiryDate(Instant.now().plusSeconds(60));
        refreshToken.setUser(user);
        when(tokenRepository.findByToken("refresh-token", RefreshToken.class))
            .thenReturn(Optional.of(refreshToken));
        when(userRepository.findOneWithAuthoritiesByLogin("field-user"))
            .thenReturn(Optional.of(user));
        when(tokenRepository.save(any(RefreshToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(
            tokenService,
            "refreshTokenValidity",
            60L
        );

        var rotated = tokenService.rotateRefreshToken("refresh-token");

        assertThat(rotated.getToken())
            .isNotBlank()
            .isNotEqualTo("refresh-token");
        assertThat(rotated.getUser().getLogin()).isEqualTo("field-user");
        verify(tokenRepository).deleteByUserUid(user.getUid());
        verify(tokenRepository).save(any(RefreshToken.class));
    }

    private User user(String login, boolean activated) {
        User user = new User();
        user.setLogin(login);
        user.setUid("usr00000001");
        user.setActivated(activated);
        return user;
    }
}
