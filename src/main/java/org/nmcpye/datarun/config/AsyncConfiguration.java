package org.nmcpye.datarun.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import tech.jhipster.async.ExceptionHandlingAsyncTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@RequiredArgsConstructor
@Slf4j
@Profile("!testdev & !testprod")
public class AsyncConfiguration implements AsyncConfigurer {

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    @Profile("!testdev & !testprod")
    @ConditionalOnProperty(
        name = "datarun.scheduling.enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    static class SchedulingConfiguration {
    }

    private final TaskExecutionProperties taskExecutionProperties;

    @Bean("lastSeenExecutor")
    public Executor lastSeenExecutor() {
        ThreadPoolTaskExecutor t = new ThreadPoolTaskExecutor();
        t.setCorePoolSize(2);
        t.setMaxPoolSize(4);
        t.setQueueCapacity(1000);
        t.setThreadNamePrefix("lastseen-");
        t.initialize();
        return t;
    }

    @Bean("errorLoggingExecutor")
    public Executor errorLoggingExecutor() {
        ThreadPoolTaskExecutor t = new ThreadPoolTaskExecutor();
        t.setCorePoolSize(2);
        t.setMaxPoolSize(5);
        t.setQueueCapacity(500);    // bounded queue
        t.setThreadNamePrefix("err-log-");
        t.initialize();
        return t;
    }

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        log.debug("Creating Async Task Executor");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(taskExecutionProperties.getPool().getCoreSize());
        executor.setMaxPoolSize(taskExecutionProperties.getPool().getMaxSize());
        executor.setQueueCapacity(taskExecutionProperties.getPool().getQueueCapacity());
        executor.setThreadNamePrefix(taskExecutionProperties.getThreadNamePrefix());
        return new ExceptionHandlingAsyncTaskExecutor(executor);
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
