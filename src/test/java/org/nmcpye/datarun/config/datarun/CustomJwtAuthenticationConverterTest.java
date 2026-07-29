package org.nmcpye.datarun.config.datarun;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.DomainUserDetailsService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class CustomJwtAuthenticationConverterTest {

    @Test
    void usesCurrentDatabaseAuthoritiesInsteadOfStaleTokenClaims() {
        DomainUserDetailsService userDetailsService =
            mock(DomainUserDetailsService.class);
        CurrentUserDetails userDetails = mock(CurrentUserDetails.class);
        when(userDetailsService.loadUserByUsername("field-user"))
            .thenReturn(userDetails);
        Collection<? extends GrantedAuthority> authorities =
            List.of(new SimpleGrantedAuthority("ROLE_USER"));
        doReturn(authorities).when(userDetails).getAuthorities();

        Jwt jwt = new Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(60),
            Map.of("alg", "HS512"),
            Map.of(
                "sub", "field-user",
                "auth", "ROLE_ADMIN"
            )
        );

        var authentication =
            new CustomJwtAuthenticationConverter(userDetailsService).convert(jwt);

        assertThat(authentication.getPrincipal()).isSameAs(userDetails);
        assertThat(authentication.getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_USER");
    }
}
