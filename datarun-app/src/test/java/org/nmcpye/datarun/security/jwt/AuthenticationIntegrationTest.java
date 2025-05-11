package org.nmcpye.datarun.security.jwt;

import org.nmcpye.datarun.config.SecurityJwtConfiguration;
import org.nmcpye.datarun.config.WebConfigurer;
import org.nmcpye.datarun.config.datarun.DataRunSecurityConfig;
import org.nmcpye.datarun.management.SecurityMetersService;
import org.nmcpye.datarun.web.rest.v1.authenticate.AuthenticateResource;
import org.springframework.boot.test.context.SpringBootTest;
import tech.jhipster.config.JHipsterProperties;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    properties = {
        "jhipster.security.authentication.jwt.base64-secret=fd54a45s65fds737b9aafcb3412e07ed99b267f33413274720ddbb7f6c5e64e9f14075f2d7ed041592f0b7657baf8",
        "jhipster.security.authentication.jwt.token-validity-in-seconds=60000",
    },
    classes = {
        JHipsterProperties.class,
        WebConfigurer.class,
        DataRunSecurityConfig.class,
        SecurityJwtConfiguration.class,
        SecurityMetersService.class,
        AuthenticateResource.class,
        JwtAuthenticationTestUtils.class,
    }
)
public @interface AuthenticationIntegrationTest {
}
