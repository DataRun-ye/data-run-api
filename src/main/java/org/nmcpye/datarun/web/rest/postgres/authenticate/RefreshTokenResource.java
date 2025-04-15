package org.nmcpye.datarun.web.rest.postgres.authenticate;

import jakarta.validation.Valid;
import org.nmcpye.datarun.drun.postgres.service.impl.TokenService;
import org.nmcpye.datarun.web.rest.postgres.authenticate.jwt.TokenRefreshRequest;
import org.nmcpye.datarun.web.rest.postgres.authenticate.jwt.TokenRefreshResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * @author Hamza Assada, 16/04/2025
 */
@RestController
@RequestMapping("/api/custom")
public class RefreshTokenResource {

    private final TokenService refreshTokenService;

    public RefreshTokenResource(TokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();


        return Optional.of(refreshTokenService.verifyRefreshToken(requestRefreshToken))
            .map(refreshToken -> ResponseEntity.ok(TokenRefreshResponse
                .builder()
                .accessToken(refreshTokenService.generateAccessToken(refreshToken.getUser().getLogin()))
                .refreshToken(refreshTokenService.rotateRefreshToken(refreshToken).getToken())))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid refresh token"));
    }
}
