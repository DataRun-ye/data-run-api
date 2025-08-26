package org.nmcpye.datarun;

import org.nmcpye.datarun.config.AsyncSyncConfiguration;
import org.nmcpye.datarun.config.EmbeddedSQL;
import org.nmcpye.datarun.config.JacksonConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Base composite annotation for integration tests.
 */
/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = { DataRunApiApp.class, JacksonConfiguration.class, AsyncSyncConfiguration.class },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("testdev")
@ImportAutoConfiguration(exclude = { SecurityAutoConfiguration.class })
@EmbeddedSQL
public @interface IntegrationTest {
}
//@Target(ElementType.TYPE)
//@Retention(RetentionPolicy.RUNTIME)
//@SpringBootTest(classes = { DataRunApiApp.class,
//        JacksonConfiguration.class, AsyncSyncConfiguration.class })
//@EmbeddedSQL
//public @interface IntegrationTest {
//}
