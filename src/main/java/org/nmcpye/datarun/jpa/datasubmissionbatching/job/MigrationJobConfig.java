package org.nmcpye.datarun.jpa.datasubmissionbatching.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datasubmission.service.FormDataProcessor;
import org.nmcpye.datarun.jpa.datasubmission.validation.CompositeSubmissionValidator;
import org.nmcpye.datarun.jpa.datasubmissionbatching.service.MigrationSkipListener;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.PlatformTransactionManager;

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

    // =================================================================
    // == MONGO to POSTGRES MIGRATION JOB
    // =================================================================

    @Bean
    public Job mongoToPostgresJob(JobRepository jobRepository,
                                  @Qualifier("mongoToPgStep") Step migrationStep) {
        return new JobBuilder("mongoToPostgresJob", jobRepository)
            .start(migrationStep)
            .build();
    }

    @Bean
    public Step mongoToPgStep(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              ItemReader<DataFormSubmission> mongoItemReader,
                              ItemProcessor<DataFormSubmission, DataSubmission> migrationProcessor,
                              @Qualifier("submissionWriter") ItemWriter<DataSubmission> submissionWriter,
                              MigrationSkipListener migrationSkipListener) {
        return new StepBuilder("mongoToPgStep", jobRepository)
            .<DataFormSubmission, DataSubmission>chunk(chunkSize, transactionManager)
            .reader(mongoItemReader)
            .processor(migrationProcessor)
            .writer(submissionWriter)
            .faultTolerant()
            .skipLimit(Integer.MAX_VALUE)        // ensure the job doesn't fail on many skips
//            .skipPolicy(migrationSkipPolicy())   // or use .skip(...) lines below
            .skip(Exception.class)
            // .skip(DataIntegrityViolationException.class)
//            .skip(Exception.class)
            .retryLimit(2)
            .retry(Exception.class)
            .listener(migrationSkipListener)
            .build();
    }

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

    @Bean
    public ItemProcessor<DataFormSubmission, DataSubmission> migrationProcessor(
        FormDataProcessor formDataProcessor,
        CompositeSubmissionValidator compositeValidator,
        ObjectMapper objectMapper
    ) {
        return mongoDoc -> {
            DataSubmission ds = mapMongoToJpa(mongoDoc, objectMapper);
            formDataProcessor.enrichFormData(ds, true);
            compositeValidator.validateAndEnrich(ds);

            return ds;
        };
    }

    @Bean
    public ItemWriter<DataSubmission> submissionWriter(DataSubmissionRepository submissionRepository) {
        return submissionRepository::persistAllAndFlush;
    }

    private DataSubmission mapMongoToJpa(DataFormSubmission mongo, ObjectMapper objectMapper) {
        DataSubmission ds = new DataSubmission();
        ds.setId(CodeGenerator.nextUlid());
        ds.setUid(mongo.getUid());
        ds.setForm(mongo.getForm());
        ds.setFormVersion(mongo.getFormVersion());
        ds.setVersion(mongo.getVersion());
        ds.setTeam(mongo.getTeam());
        ds.setTeamCode(mongo.getTeamCode());
        ds.setOrgUnit(mongo.getOrgUnit());
        ds.setOrgUnitCode(mongo.getOrgUnitCode());
        ds.setOrgUnitName(mongo.getOrgUnitName());
        ds.setDeleted(mongo.getDeleted());
        ds.setDeletedAt(mongo.getDeletedAt());
        ds.setActivity(mongo.getActivity());
        ds.setAssignment(mongo.getAssignment());
        ds.setStatus(mongo.getStatus());
        ds.setStartEntryTime(mongo.getStartEntryTime());
        ds.setFinishedEntryTime(mongo.getFinishedEntryTime());
        ds.setFormData(objectMapper.convertValue(mongo.getFormData(), JsonNode.class));
        ds.setSerialNumber(mongo.getSerialNumber());
        ds.setCreatedBy(mongo.getCreatedBy());
        ds.setCreatedDate(mongo.getCreatedDate());
//        if (mongo.getSerialNumber() != null) ds.setSerialNumber(mongo.getSerialNumber());
        return ds;
    }

//    @Bean
//    public SkipPolicy migrationSkipPolicy() {
//        return (throwable, skipCount) -> {
//            // skip validation-type exceptions always
//            return throwable instanceof DomainValidationException;
////            // maybe skip DB constraint exceptions up to a threshold:
////            if (throwable instanceof DataIntegrityViolationException && skipCount < 10000) {
////                return true;
////            }
//            // otherwise don't skip
//        };
//    }
}
