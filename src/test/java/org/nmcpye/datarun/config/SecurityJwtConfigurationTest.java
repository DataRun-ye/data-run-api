package org.nmcpye.datarun.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.management.SecurityMetersService;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A token the decoder rejects for an unforeseen reason is the client's doing, so it is logged at WARN. */
class SecurityJwtConfigurationTest {

    /** The test profile's secret: a 512-bit key, as HS512 needs. */
    private static final String SECRET =
        "MjJmNDA2ZjQwMzg5NWE0ZDFiZmZmY2IzYzk2NmQ0MmI4OTM0NWU1MDBjYmU1N2VkYmNjOWM2ZmI2OGVlZWVmYTI0NDM0Y2JhY2ZhMWI2Nzg2ZGY2YWNhZjNmMTViYTcyZTNmMjMwNmFjZTRkMTAzOTVhYjlkNDRhOWZhNDJhY2I=";

    private final ListAppender<ILoggingEvent> logged = new ListAppender<>();

    @BeforeEach
    void captureLogs() {
        logged.start();
        logger().addAppender(logged);
    }

    @AfterEach
    void releaseLogs() {
        logger().detachAppender(logged);
    }

    @Test
    void anyoneCanSendATokenWithAnotherAlgorithmAndItIsAWarningNotAnError() throws Exception {
        SignedJWT foreign = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256),
            new JWTClaimsSet.Builder().subject("anyone").expirationTime(new Date(System.currentTimeMillis() + 60_000))
                .build());
        foreign.sign(new MACSigner(new byte[32]));

        assertThatThrownBy(() -> decoder().decode(foreign.serialize())).isInstanceOf(JwtException.class);

        assertThat(logged.list).singleElement().satisfies(line -> {
            assertThat(line.getLevel()).isEqualTo(Level.WARN);
            assertThat(line.getFormattedMessage()).startsWith("Unknown JWT error");
        });
    }

    private static JwtDecoder decoder() {
        SecurityJwtConfiguration configuration = new SecurityJwtConfiguration();
        ReflectionTestUtils.setField(configuration, "jwtKey", SECRET);
        return configuration.jwtDecoder(new SecurityMetersService(new SimpleMeterRegistry()));
    }

    private static Logger logger() {
        return (Logger) LoggerFactory.getLogger(SecurityJwtConfiguration.class);
    }
}
