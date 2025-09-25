package org.nmcpye.datarun.config.datarun;

import org.nmcpye.datarun.jpa.datasubmissionoutbox.BackoffPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Hamza Assada
 * @since 24/09/2025
 */
@Configuration
@EnableConfigurationProperties(OutboxProperties.class)
public class WorkerConfiguration {

    private final OutboxProperties outboxProperties;

    public WorkerConfiguration(OutboxProperties outboxProperties) {
        this.outboxProperties = outboxProperties;
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService outboxExecutor() {
        int threads = Math.max(2, outboxProperties.getWorkerThreads());
        return Executors.newFixedThreadPool(threads);
    }

    @Bean
    public BackoffPolicy backoffPolicy() {
        return BackoffPolicy.defaultPolicy();
    }

    // Extractor worker bean
//    @Bean
//    public ExtractorWorker extractorWorker(OutboxEventRepository outboxRepo,
//                                           PlatformTransactionManager txManager,
//                                           ExtractorService extractorService,
//                                           ExecutorService outboxExecutor,
//                                           BackoffPolicy backoff) {
//        // ExtractorWorker (constructor: repo, txManager, props, extractorService, threadPoolSize)
//        ExtractorWorker w = new ExtractorWorker(outboxRepo, txManager, outboxProperties, extractorService, Math.max(1, outboxProperties.getWorkerThreads() / 2));
//        // if your ExtractorWorker accepts an ExecutorService, wire it instead of creating new pool inside
//        return w;
//    }
//
//    // ETL worker bean
//    @Bean
//    public EtlWorker etlWorker(OutboxEventRepository outboxRepo,
//                               PlatformTransactionManager txManager,
//                               EtlCoordinatorService etlService,
//                               ExecutorService outboxExecutor,
//                               BackoffPolicy backoff) {
//        EtlWorker w = new EtlWorker(outboxRepo, txManager, outboxProperties, etlService, Math.max(1, outboxProperties.getWorkerThreads() / 2));
//        return w;
//    }

    // Reclaim job bean (scheduled)
//    @Bean
//    public ReclaimStaleJob reclaimStaleJob(OutboxEventRepository outboxRepo) {
//        return new ReclaimStaleJob(outboxRepo, outboxProperties);
//    }
}
