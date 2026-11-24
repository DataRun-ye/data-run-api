package org.nmcpye.datarun.characterization.a2;

import org.nmcpye.datarun.security.jwt.JwtAuthenticationTestUtils;

/**
 * Real access tokens, so a request passes the production filter chain and the server loads the user, its teams
 * and its grants from the database, as for a phone.
 */
final class A2Tokens {

    private A2Tokens() {
    }

    static String token(String jwtKey, String login) {
        return JwtAuthenticationTestUtils.createValidTokenForUser(jwtKey, login);
    }
}
