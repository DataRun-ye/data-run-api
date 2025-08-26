//package org.nmcpye.datarun.jpa.outbox;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import org.junit.jupiter.api.*;
//import org.nmcpye.datarun.config.datarun.OutboxConfig;
//import org.nmcpye.datarun.config.datarun.OutboxProperties;
//import org.nmcpye.datarun.jpa.etl.service.EtlCoordinatorService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.context.annotation.Bean;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.testcontainers.containers.PostgreSQLContainer;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//import java.time.Instant;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
///**
// * Integration test for Outbox claim -> processing -> markDone/reschedule flows.
// * <p>
// * This test uses a TestSubmissionEtlService that intentionally fails for "fail" submissionId
// * to exercise the reschedule flow, and succeeds for "ok".
// * <p>
// * Ensure your OutboxEvent, OutboxEventRepository and OutboxEventRepositoryImpl are present and component-scanned.
// *
// * @author Hamza Assada 15/08/2025 (7amza.it@gmail.com)
// */
//@Testcontainers
//@SpringBootTest(
//    classes = {
//        OutboxRelay.class,
//        OutboxConfig.class,
//        OutboxEventRepository.class,
//        OutboxEventRepositoryImpl.class,
//        OutboxEvent.class,
////        OutboxProperties.class,
//        OutboxIntegrationTest.TestConfig.class
//    },
//    webEnvironment = SpringBootTest.WebEnvironment.NONE // no web server
//)
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//public class OutboxIntegrationTest {
//
//    // start Postgres container
//    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>(
//        "postgres:16.2")
//        .withDatabaseName("testdb")
//        .withUsername("test")
//        .withPassword("test");
//
//    static {
//        PG.start();
//    }
//
//    @DynamicPropertySource
//    static void datasourceProps(DynamicPropertyRegistry registry) {
//        registry.add("spring.datasource.url", PG::getJdbcUrl);
//        registry.add("spring.datasource.username", PG::getUsername);
//        registry.add("spring.datasource.password", PG::getPassword);
//        // use Hibernate to create schema automatically for test (optional)
//        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
//        registry.add("spring.datasource.driver-class-name", PG::getDriverClassName);
//    }
//
//    @Autowired
//    ObjectMapper objectMapper;
//
//    @Autowired
//    OutboxEventRepository outboxRepo;
//
//    @Autowired
//    OutboxRelay outboxRelay;
//
//    @Autowired
//    OutboxProperties outboxProperties;
//
//    @BeforeEach
//    void setup() {
//        // ensure table exists (if using hibernate.ddl-auto=update this is not strictly required)
//        // Clean previous rows
//        outboxRepo.deleteById(1L);
//    }
//
//    @Test
//    @Order(1)
//    void test_claim_process_markDone_and_reschedule() throws Exception {
//        // create two outbox events: one that will succeed, one that will fail in ETL
//        ObjectNode payloadOk = objectMapper.createObjectNode();
//        payloadOk.put("submissionId", "ok");
//
//        ObjectNode payloadFail = objectMapper.createObjectNode();
//        payloadFail.put("submissionId", "fail");
//
//        OutboxEvent evOk = new OutboxEvent();
//        evOk.setAggregateType("DataSubmission");
//        evOk.setAggregateId("ok");
//        evOk.setEventType("submission.saved");
//        evOk.setPayload(payloadOk);
//        evOk.setStatus(OutboxEventStatus.PENDING);
//        evOk.setAttempts(0);
//        evOk.setCreatedAt(Instant.now());
//        evOk.setAvailableAt(Instant.now());
//
//        OutboxEvent evFail = new OutboxEvent();
//        evFail.setAggregateType("DataSubmission");
//        evFail.setAggregateId("fail");
//        evFail.setEventType("submission.saved");
//        evFail.setPayload(payloadFail);
//        evFail.setStatus(OutboxEventStatus.PENDING);
//        evFail.setAttempts(0);
//        evFail.setCreatedAt(Instant.now());
//        evFail.setAvailableAt(Instant.now());
//
//        outboxRepo.persistAll(List.of(evOk, evFail));
//
//        // sanity: two pending rows
//        List<OutboxEvent> pending = outboxRepo.findAll();
//        assertThat(pending).hasSize(2);
//
//        // call the relay once to process the batch
//        // (pollAndProcess runs claimBatch -> process each -> markDone/reschedule)
//        outboxRelay.pollAndProcess();
//
//        // Give some time for worker threads to finish (OutboxRelay uses worker pool and await latch)
//        // pollAndProcess waits for latch; no further wait required, but we add a small sleep for safety.
//        Thread.sleep(200);
//
//        // fetch events from DB to verify statuses
//        List<OutboxEvent> all = outboxRepo.findAll();
//        Optional<OutboxEvent> okRow = all.stream().filter(e -> "ok".equals(e.getAggregateId())).findFirst();
//        Optional<OutboxEvent> failRow = all.stream().filter(e -> "fail".equals(e.getAggregateId())).findFirst();
//
//        assertThat(okRow).isPresent();
//        assertThat(failRow).isPresent();
//
//        OutboxEvent ok = okRow.get();
//        OutboxEvent fail = failRow.get();
//
//        // ok should be DONE
//        assertThat(ok.getStatus()).isEqualTo(OutboxEventStatus.DONE);
//        assertThat(ok.getProcessedAt()).isNotNull();
//
//        // fail should have been rescheduled, status back to PENDING and attempts >= 1
//        assertThat(fail.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
//        assertThat(fail.getAttempts()).isGreaterThanOrEqualTo(1);
//        assertThat(fail.getAvailableAt()).isAfterOrEqualTo(fail.getCreatedAt());
//    }
//
//    /**
//     * Test configuration: provides a TestSubmissionEtlService that succeeds for "ok" and fails for "fail".
//     * Also wires OutboxProperties to sensible small values for faster tests.
//     */
//    @TestConfiguration
//    static class TestConfig {
//
//        @Bean
//        public EtlCoordinatorService submissionEtlService(OutboxEventRepository outboxRepo) {
//            // a simple test ETL service
//            return new EtlCoordinatorService(null, null, null) {
//                @Override
//                public void processSubmission(String submissionId) {
//                    if ("fail".equals(submissionId)) {
//                        throw new RuntimeException("simulated ETL failure for testing");
//                    }
//                    // success case: do nothing (or record processed ids in a static list if you want)
//                }
//            };
//        }
//
//        @Bean
//        public OutboxProperties outboxProperties() {
//            OutboxProperties p = new OutboxProperties();
//            p.setBatchSize(10);
//            p.setWorkerThreads(2);
//            p.setMaxAttempts(5);
//            p.setPollIntervalMs(1000);
//            return p;
//        }
//    }
//}
