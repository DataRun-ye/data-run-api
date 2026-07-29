package org.nmcpye.datarun.config.datarun;

import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.DomainUserDetailsService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

public class CustomJwtAuthenticationConverter
    implements Converter<Jwt, AbstractAuthenticationToken> {

    private final DomainUserDetailsService userDetailsService;

    public CustomJwtAuthenticationConverter(DomainUserDetailsService uds) {
        this.userDetailsService = uds;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        CurrentUserDetails userDetails =
            (CurrentUserDetails) userDetailsService.loadUserByUsername(jwt.getSubject());

        return new UsernamePasswordAuthenticationToken(
            userDetails, jwt.getTokenValue(), userDetails.getAuthorities()
        );
    }
}
