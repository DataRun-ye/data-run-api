package org.nmcpye.datarun.web.v2.config;

import org.nmcpye.datarun.web.v2.paging.PageQueryResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * The handler parameters v2 conventions supply. Each resolver answers only its own parameter type, which no v1
 * handler declares.
 */
@Configuration
public class V2WebMvcConfiguration implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new PageQueryResolver());
    }
}
