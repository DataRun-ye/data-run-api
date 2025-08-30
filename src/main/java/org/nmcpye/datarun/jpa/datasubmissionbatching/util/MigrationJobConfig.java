package org.nmcpye.datarun.jpa.datasubmissionbatching.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.SubmissionDataProcessor;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datasubmission.service.FormDataProcessor;
import org.nmcpye.datarun.jpa.datasubmission.validation.CompositeSubmissionValidator;
import org.nmcpye.datarun.jpa.datasubmissionbatching.service.MigrationSkipListener;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.OutboxEvent;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.OutboxEventStatus;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.repository.OutboxEventRepository;
import org.nmcpye.datarun.jpa.datatemplate.service.TemplateElementService;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Migration job config that pages docs from Mongo and writes them to Postgres (and Outbox).
 *
 * @author Hamza Assada 16/08/2025 (7amza.it@gmail.com)
 */
//@Configuration
@Slf4j
public class MigrationJobConfig {

    @Value("${migration.mongo.collection:dataFormSubmission}")
    private String mongoCollection;

    @Value("${migration.chunk.size:200}")
    private int chunkSize;

    @Value("${migration.page.size:200}")
    private int pageSize;

    @Value("${migration.generate-missing-ids:true}")
    private boolean generateMissingIds;

    // Beans required from your app: mongoTemplate, submissionProcessor, migrationGen, repos, mapper
    @Bean
    public Job mongoToPostgresJob(JobRepository jobRepository, Step migrationStep) {
        return new JobBuilder("mongoToPostgresJob", jobRepository)
            .start(migrationStep)
            .build();
    }

    @Bean
    public Step migrationStep(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              ItemReader<DataFormSubmission> reader,
                              ItemProcessor<DataFormSubmission, DataSubmission> processor,
                              ItemWriter<DataSubmission> writer,
                              MigrationSkipListener migrationSkipListener) {

        // create builder with jobRepository, then use chunk with explicit tx manager
        return new StepBuilder("mongoToPgStep", jobRepository)
            .<DataFormSubmission, DataSubmission>chunk(chunkSize, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .faultTolerant()
            .skipLimit(1000)
            .skip(Exception.class)
            .retryLimit(3)
            .retry(Exception.class)
            .listener(migrationSkipListener)
            .build();
    }

    // Reader: wraps the MongoRangeItemReader into an ItemReader that returns single items sequentially
    @Bean
    public ItemReader<DataFormSubmission> mongoItemReader(MongoTemplate mongoTemplate) {
        var rangeReader = new MongoRangeItemReader<>(mongoTemplate, DataFormSubmission.class, mongoCollection, pageSize);
        return new ItemReader<>() {
            private List<DataFormSubmission> buffer = new ArrayList<>();

            @Override
            public DataFormSubmission read() {
                if (buffer.isEmpty()) {
                    buffer = rangeReader.readNextPage();
                    if (buffer.isEmpty()) return null;
                }
                return buffer.remove(0);
            }
        };
    }

    // Processor: map Mongo entity -> DataSubmission JPA entity, validate/enrich, generate missing repeat ids (migration)
    @Bean
    public ItemProcessor<DataFormSubmission, DataSubmission> migrationProcessor(
        SubmissionDataProcessor submissionDataProcessor,
        FormDataProcessor formDataProcessor,
        CompositeSubmissionValidator compositeValidator,
        TemplateElementService templateElementService,
        ObjectMapper objectMapper
    ) {
        return mongoDoc -> {
            // map fields: adapt to your mongo entity fields
            DataSubmission ds = mapMongoToJpa(mongoDoc, objectMapper);

            formDataProcessor.enrichFormData(ds, true);
            // Run validators that are safe for migration (you may skip strict client checks)
            compositeValidator.validateAndEnrich(ds);

            // Generate missing repeat ids for migration
            ObjectNode root = (ObjectNode) (ds.getFormData() == null ? objectMapper.createObjectNode() : ds.getFormData().deepCopy());
            final var migrationRepeatIdGenerator = new MigrationRepeatIdGenerator(objectMapper, templateElementService.getTemplateElementMap(ds.getForm(), ds.getVersion()));
            int generated = migrationRepeatIdGenerator.generateMissingIdsForMigration(root, ds.getUid());
            if (generated > 0) {
                ds.setFormData(root);
            }

            return ds;
        };
    }

    // Writer: persist submissions + outbox rows in same transaction (chunk transaction)
    @Bean
    public ItemWriter<DataSubmission> migrationWriter(DataSubmissionRepository submissionRepository,
                                                      OutboxEventRepository outboxEventRepository,
                                                      ObjectMapper objectMapper) {
        return items -> {
            if (items == null || items.isEmpty()) return;
            // Persist all submissions (BaseJpaRepository.persistAllAndFlush or similar)
            submissionRepository.persistAllAndFlush(items);

            // Create outbox rows for each saved submission
            List<OutboxEvent> outboxEvents = new ArrayList<>(items.size());
            for (DataSubmission ds : items) {
                OutboxEvent ev = new OutboxEvent();
                ev.setAggregateType("DataSubmission");
                ev.setAggregateId(ds.getId());
                ev.setEventType("submission.saved");
                ObjectNode p = objectMapper.createObjectNode();
                p.put("submissionId", ds.getId());
                p.put("submissionVersion", ds.getLockVersion()); // or ds.getSubmissionVersion()
                ev.setPayload(p);
                ev.setStatus(OutboxEventStatus.PENDING);
                ev.setAttempts(0);
                ev.setCreatedAt(Instant.now());
                ev.setAvailableAt(Instant.now());
                outboxEvents.add(ev);
            }
            // Persist outbox events in same tx
            outboxEventRepository.persistAllAndFlush(outboxEvents);
        };
    }

    // Helper: map Mongo doc -> DataSubmission. Adjust field mappings to your mongo entity.
    private DataSubmission mapMongoToJpa(DataFormSubmission mongo, ObjectMapper objectMapper) {
        DataSubmission ds = new DataSubmission();

        ds.setId(CodeGenerator.nextUlid()); // adapt getter name
        ds.setUid(mongo.getUid());
        ds.setForm(mongo.getForm());
        ds.setFormVersion(mongo.getFormVersion());
        ds.setVersion(mongo.getVersion()); // adapt
        ds.setTeam(mongo.getTeam());
        ds.setTeamCode(mongo.getTeamCode());
        ds.setOrgUnit(mongo.getOrgUnit());
        ds.setOrgUnitCode(mongo.getOrgUnitCode());
        ds.setOrgUnitName(mongo.getOrgUnitName());
        ds.setActivity(mongo.getActivity());
        ds.setAssignment(mongo.getAssignment());
        ds.setStatus(mongo.getStatus());
        ds.setStartEntryTime(mongo.getStartEntryTime());
        ds.setFinishedEntryTime(mongo.getFinishedEntryTime());
        // convert formData map -> JsonNode
        ds.setFormData(objectMapper.convertValue(mongo.getFormData(), objectMapper.getTypeFactory().constructType(JsonNode.class)));

        // if mongo had serialNumber and you want to preserve:
        if (mongo.getSerialNumber() != null) ds.setSerialNumber(mongo.getSerialNumber());

        return ds;
    }
}
