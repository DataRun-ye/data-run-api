package org.nmcpye.datarun.caching;

import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;

/**
 * @author Hamza Assada 24/04/2025 <7amza.it@gmail.com>
 */
public class UserKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        final var userDetails = SecurityUtils.getCurrentUserDetailsOrThrow();
        String userId = userDetails.getUid();
        String arg = (String) params[0];
        if (arg == null) {
            return userId;
        }

        return userId + ":" + arg;
    }
}
