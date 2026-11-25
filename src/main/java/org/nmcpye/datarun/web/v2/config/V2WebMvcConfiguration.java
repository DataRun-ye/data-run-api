package org.nmcpye.datarun.web.v2.config;

import org.nmcpye.datarun.web.v2.caller.ApiCallerResolver;
import org.nmcpye.datarun.web.v2.idempotency.CommandRequestResolver;
import org.nmcpye.datarun.web.v2.paging.PageQueryResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * The handler parameters v2 conventions supply, and the v2 settings ({@link V2Properties}). Each resolver answers
 * only its own parameter type, which no v1 handler declares.
 */
@Configuration
@EnableConfigurationProperties(V2Properties.class)
public class V2WebMvcConfiguration implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new PageQueryResolver());
        resolvers.add(new ApiCallerResolver());
        resolvers.add(new CommandRequestResolver());
    }
}
