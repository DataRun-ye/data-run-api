package org.nmcpye.datarun.config;

import java.util.Comparator;
import org.junit.jupiter.api.ClassDescriptor;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;
import org.nmcpye.datarun.IntegrationTest;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Plain tests first, then integration tests, then the classes that dirty their context. Closing a context closes
 * what the JVM shares between contexts: the JCache manager, which every cached Spring context in the run uses, and
 * the Sentry SDK. A class closing its context before others still use the shared one would leave them a closed
 * {@code usersByLogin} cache, failing every later request that authenticates a real token.
 */
public class SpringBootTestClassOrderer implements ClassOrderer {

    @Override
    public void orderClasses(ClassOrdererContext context) {
        context.getClassDescriptors().sort(Comparator.comparingInt(SpringBootTestClassOrderer::getOrder));
    }

    private static int getOrder(ClassDescriptor classDescriptor) {
        if (classDescriptor.findAnnotation(DirtiesContext.class).isPresent()) {
            return 3;
        }
        if (classDescriptor.findAnnotation(IntegrationTest.class).isPresent()) {
            return 2;
        }
        return 1;
    }
}
