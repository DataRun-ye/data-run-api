package org.nmcpye.datarun.web.rest.postgres.authenticate;

import jakarta.validation.Valid;
import org.nmcpye.datarun.drun.postgres.dto.RefreshTokenDto;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.jwt.TokenService;
import org.nmcpye.datarun.web.rest.postgres.authenticate.jwt.TokenRefreshResponse;
import org.nmcpye.datarun.web.rest.vm.LoginVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller to authenticate users.
 */
@RestController
@RequestMapping("/api")
public class AuthenticateController {

    private static final Logger log = LoggerFactory.getLogger(AuthenticateController.class);

    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    private final TokenService tokenService;

    public AuthenticateController(AuthenticationManagerBuilder authenticationManagerBuilder,
                                  TokenService tokenService) {
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.tokenService = tokenService;
    }

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
     * {@code GET /authenticate} : check if the user is authenticated, and return its details.
     *
     * @param user the Authenticated principal request.
     * @return the user details if the user is authenticated.
     */
    @GetMapping("/authenticate")
    public CurrentUserDetails getMe(@AuthenticationPrincipal CurrentUserDetails user) {
        log.debug("REST request to check if the current user is authenticated");
        return user;
    }
}
