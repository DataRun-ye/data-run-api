package org.nmcpye.datarun.web.rest.v1.authenticate;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.nmcpye.datarun.jpa.userrefreshtoken.dto.RefreshTokenDto;
import org.nmcpye.datarun.jpa.userrefreshtoken.service.TokenService;
import org.nmcpye.datarun.web.rest.vm.LoginVM;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticateResourceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesCredentialsTypedWithArabicIndicDigitsAsLatinDigits() {
        AuthenticationManagerBuilder builder = mock(AuthenticationManagerBuilder.class);
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(builder.getObject()).thenReturn(manager);
        Authentication authenticated = mock(Authentication.class);
        when(authenticated.getName()).thenReturn("user123");
        when(manager.authenticate(any())).thenReturn(authenticated);
        TokenService tokenService = mock(TokenService.class);
        when(tokenService.generateAccessToken("user123")).thenReturn("access-token");
        RefreshTokenDto refreshToken = new RefreshTokenDto();
        refreshToken.setToken("refresh-token");
        when(tokenService.createRefreshToken("user123")).thenReturn(refreshToken);
        LoginVM login = new LoginVM();
        login.setUsername("user١٢٣");
        login.setPassword("pass۴۵۶");

        new AuthenticateResource(builder, tokenService)
            .authorize(login, mock(HttpServletRequest.class));

        ArgumentCaptor<Authentication> attempt = ArgumentCaptor.forClass(Authentication.class);
        verify(manager).authenticate(attempt.capture());
        assertEquals("user123", attempt.getValue().getPrincipal());
        assertEquals("pass456", attempt.getValue().getCredentials());
    }
}
