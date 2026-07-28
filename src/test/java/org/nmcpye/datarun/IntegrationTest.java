package org.nmcpye.datarun;

import org.nmcpye.datarun.config.AsyncSyncConfiguration;
import org.nmcpye.datarun.config.EmbeddedSQL;
import org.nmcpye.datarun.config.JacksonConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = { DataRunApiApp.class, JacksonConfiguration.class, AsyncSyncConfiguration.class },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedSQL
public @interface IntegrationTest {
}
