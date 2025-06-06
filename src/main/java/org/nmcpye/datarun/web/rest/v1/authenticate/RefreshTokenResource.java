package org.nmcpye.datarun.web.rest.v1.authenticate;

import jakarta.validation.Valid;
import org.nmcpye.datarun.jpa.userrefreshtoken.service.TokenService;
import org.nmcpye.datarun.jpa.userrefreshtoken.dto.RefreshTokenDto;
import org.nmcpye.datarun.jpa.userrefreshtoken.repository.RefreshTokenRepository;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.postgres.authenticate.jwt.TokenRefreshRequest;
import org.nmcpye.datarun.web.rest.postgres.authenticate.jwt.TokenRefreshResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.nmcpye.datarun.web.rest.v1.authenticate.RefreshTokenResource.V1;

/**
 * @author Hamza Assada 16/04/2025 <7amza.it@gmail.com>
 */
@RestController
@RequestMapping(V1)
public class RefreshTokenResource {
    protected static final String V1 = ApiVersion.API_V1;

    private final TokenService tokenService;
    private final RefreshTokenRepository tokenRepository;

    public RefreshTokenResource(TokenService tokenService, RefreshTokenRepository tokenRepository) {
        this.tokenService = tokenService;
        this.tokenRepository = tokenRepository;
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        final var refreshTokenOpt = tokenRepository.findByToken(request.getRefreshToken());
        if (refreshTokenOpt.isEmpty() || refreshTokenOpt.get().isExpired()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token");
        }

        // Retrieve the user. How you do this depends on your user management.

        return refreshTokenOpt
            .map(refreshToken -> {
                String username = refreshTokenOpt.get().getUser().getLogin();
                // generate another valid accessToken
                String newAccessToken = tokenService.generateAccessToken(username);

                // Revoke old refresh token/tokens (optional - implement rotation)
                // will delete all user's stored refresh tokens and create a single one
                RefreshTokenDto newRefreshToken = tokenService.rotateRefreshToken(refreshToken);

                return ResponseEntity.ok(new TokenRefreshResponse(
                    newAccessToken,
                    newRefreshToken.getToken()
                ));
            })
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid refresh token"));
    }

}
