package org.nmcpye.datarun.web.rest.postgres.authenticate.jwt;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Object to return as body in the refresh token endpoint.
 *
 * @author Hamza Assada, 16/04/2025
 */
@Getter
@Setter
public class TokenRefreshRequest {
    @NotNull
    private String refreshToken;
}
