package org.nmcpye.datarun.web.v2.fixtures;

import org.nmcpye.datarun.security.jwt.JwtAuthenticationTestUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Accounts for the v2 integration tests, created once in the shared test database: two active field accounts
 * (ROLE_USER, no team) and one deactivated account. Tokens are real, so requests pass the v2 chain and the server
 * loads the account from the database as it would for a tool.
 */
public final class V2TestUsers {

    public static final String CALLER_A = "v2-caller-a";
    public static final String CALLER_A_ID = "01V2CALLERA000000000000000";
    public static final String CALLER_B = "v2-caller-b";
    public static final String CALLER_B_ID = "01V2CALLERB000000000000000";
    public static final String INACTIVE = "v2-inactive";

    /** The bcrypt hash of the seeded test account's password; these accounts never sign in with it. */
    private static final String PASSWORD_HASH = "$2a$10$VEjxo0jq2YG9Rbk2HmX9S.k1uZBGYUHdUcid3g/vfiEl7lwWgOH/K";

    private V2TestUsers() {
    }

    public static void ensure(NamedParameterJdbcTemplate jdbc, PlatformTransactionManager transactions) {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            user(jdbc, CALLER_A_ID, "v2CallerAaa", CALLER_A, true);
            user(jdbc, CALLER_B_ID, "v2CallerBbb", CALLER_B, true);
            user(jdbc, "01V2INACTIVE00000000000000", "v2Inactive1", INACTIVE, false);
        });
    }

    public static String bearer(String jwtKey, String login) {
        return JwtAuthenticationTestUtils.BEARER + JwtAuthenticationTestUtils.createValidTokenForUser(jwtKey, login);
    }

    private static void user(NamedParameterJdbcTemplate jdbc, String id, String uid, String login, boolean active) {
        MapSqlParameterSource row = new MapSqlParameterSource().addValue("id", id).addValue("uid", uid)
            .addValue("login", login).addValue("hash", PASSWORD_HASH).addValue("active", active);
        int inserted = jdbc.update("""
            INSERT INTO app_user (id, uid, login, password_hash, activated, lang_key, created_by)
            SELECT :id, :uid, :login, :hash, :active, 'en', 'v2-tests'
            WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE id = :id)""", row);
        if (inserted == 1) {
            jdbc.update("INSERT INTO app_user_authority (user_id, authority_name) VALUES (:id, 'ROLE_USER')", row);
        }
    }
}
