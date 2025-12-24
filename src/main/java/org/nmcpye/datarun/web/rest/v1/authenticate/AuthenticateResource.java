package org.nmcpye.datarun.web.rest.v1.authenticate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.userrefreshtoken.dto.RefreshTokenDto;
import org.nmcpye.datarun.jpa.userrefreshtoken.service.TokenService;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.postgres.authenticate.jwt.TokenRefreshResponse;
import org.nmcpye.datarun.web.rest.vm.LoginVM;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import static org.nmcpye.datarun.web.rest.v1.authenticate.AuthenticateResource.V1;

/**
 * Controller to authenticate users.
 */
@RestController
@RequestMapping(V1)
@RequiredArgsConstructor
@Slf4j
public class AuthenticateResource {
    protected static final String V1 = ApiVersion.API_V1;

    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    private final TokenService tokenService;

    @PostMapping("/authenticate")
    public ResponseEntity<?> authorize(@Valid @RequestBody LoginVM loginVM) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
            loginVM.getUsername(),
            loginVM.getPassword()
        );

        Authentication authentication = authenticationManagerBuilder.getObject()
            .authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = tokenService.generateAccessToken(authentication.getName());
        RefreshTokenDto refreshToken = tokenService.createRefreshToken(authentication.getName());

        return ResponseEntity.ok()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .body(new TokenRefreshResponse(accessToken, refreshToken.getToken()));
    }

    /**
     * {@code GET /authenticate} : check if the user is authenticated, and return its login.
     *
     * @param request the HTTP request.
     * @return the login if the user is authenticated.
     */
    @GetMapping("/authenticate")
    public String isAuthenticated(HttpServletRequest request) {
        log.debug("REST request to check if the current user is authenticated");
        return request.getRemoteUser();
    }
}
