package org.nmcpye.datarun.web.rest.v1.authenticate;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.userrefreshtoken.dto.RefreshTokenDto;
import org.nmcpye.datarun.jpa.userrefreshtoken.service.TokenService;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.postgres.authenticate.jwt.TokenRefreshRequest;
import org.nmcpye.datarun.web.rest.postgres.authenticate.jwt.TokenRefreshResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.nmcpye.datarun.web.rest.v1.authenticate.RefreshTokenResource.V1;

/**
 * @author Hamza Assada 16/04/2025 (7amza.it@gmail.com)
 */
@RestController
@RequestMapping(V1)
@RequiredArgsConstructor
public class RefreshTokenResource {
    protected static final String V1 = ApiVersion.API_V1;

    private final TokenService tokenService;

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        RefreshTokenDto newRefreshToken =
            tokenService.rotateRefreshToken(request.getRefreshToken());
        String newAccessToken = tokenService.generateAccessToken(
            newRefreshToken.getUser().getLogin()
        );
        return ResponseEntity.ok(new TokenRefreshResponse(
            newAccessToken,
            newRefreshToken.getToken()
        ));
    }
}
