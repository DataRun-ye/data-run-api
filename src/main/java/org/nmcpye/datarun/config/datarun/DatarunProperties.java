package org.nmcpye.datarun.config.datarun;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Hamza Assada 16/04/2025 (7amza.it@gmail.com)
 */
@Getter
@ConfigurationProperties(
    prefix = "datarun",
    ignoreUnknownFields = true,
    ignoreInvalidFields = true
)
public class DatarunProperties {
    private final Security security = new Security();

    public DatarunProperties() {
    }

    @Getter
    public static class Security {
        private final Security.ClientAuthorization clientAuthorization = new Security.ClientAuthorization();
        private final Security.Authentication authentication = new Security.Authentication();

        public Security() {
        }

        @Setter
        @Getter
        public static class ClientAuthorization {
            private String accessTokenUri;
            private String tokenServiceId;
            private String jwkSetUri;
            private String clientId;
            private String clientSecret;

            public ClientAuthorization() {
                this.accessTokenUri = DatarunDefaults.Security.ClientAuthorization.accessTokenUri;
                this.tokenServiceId = DatarunDefaults.Security.ClientAuthorization.tokenServiceId;
                this.clientId = DatarunDefaults.Security.ClientAuthorization.clientId;
                this.clientSecret = DatarunDefaults.Security.ClientAuthorization.clientSecret;
                this.jwkSetUri = DatarunDefaults.Security.ClientAuthorization.jwkSetUri;
            }

        }

        @Getter
        public static class Authentication {
            private final Security.Authentication.Jwt jwt = new Security.Authentication.Jwt();
            private final String jwkSetUri = null;

            public Authentication() {
            }

            @Setter
            @Getter
            public static class Jwt {
                private String secret;
                private String base64Secret;
                private long tokenValidityInSeconds;
                private long tokenValidityInSecondsForRememberMe;
                private long refreshTokenValidityInSeconds;

                private String rsaPrivateKey; // Base64 encoded private key
                private String rsaPublicKey;  // Base64 encoded public key

                public Jwt() {
                    this.secret = DatarunDefaults.Security.Authentication.Jwt.secret;
                    this.base64Secret = DatarunDefaults.Security.Authentication.Jwt.base64Secret;
                    this.tokenValidityInSeconds = 1800L;
                    this.tokenValidityInSecondsForRememberMe = 2592000L;
                    this.refreshTokenValidityInSeconds = 2592000L;
                }

            }
        }
    }
}
