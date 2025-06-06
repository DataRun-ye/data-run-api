package org.nmcpye.datarun.mongo.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.bulk.BulkWriteResult;
import org.nmcpye.datarun.common.WithIdentifierObject;
import org.nmcpye.datarun.jpa.migration.FormDataMigrator;
import org.nmcpye.datarun.mongo.domain.DataForm;
import org.nmcpye.datarun.mongo.domain.DataFormSubmissionBu;
import org.nmcpye.datarun.mongo.domain.DataFormSubmissionFinal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

//@Component
//@Transactional
public class TemplateDrivenMigrator implements CommandLineRunner {
    // Add these constants at the class level
    private static final int BATCH_SIZE = 1000; // Adjust based on document size
    private static final int LOG_INTERVAL = 500; // Progress logging frequency
    private static final Logger log = LoggerFactory.getLogger(TemplateDrivenMigrator.class);
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;
    private final FormDataMigrator formDataMigrator;

    public TemplateDrivenMigrator(MongoTemplate mongoTemplate, ObjectMapper objectMapper, FormDataMigrator formDataMigrator) {
        this.objectMapper = objectMapper;
        this.mongoTemplate = mongoTemplate;
        this.formDataMigrator = formDataMigrator;
    }

    @Override
    public void run(String... args) throws Exception {
        runMainStream();
    }

    private void runMainStream() {
        Query formTemplateQuery = Query.query(Criteria.where("uid").is("Tcf3Ks9ZRpB"));

        DataForm formTemplate = mongoTemplate.findOne(formTemplateQuery, DataForm.class);

        Query query = Query.query(Criteria.where("form").is("Tcf3Ks9ZRpB")/*.and("processed").ne(Boolean.TRUE)*/);

        List<DataFormSubmissionBu> v1Submissions = mongoTemplate.find(query, DataFormSubmissionBu.class);
        AtomicInteger counter = new AtomicInteger();
//        List<DataFormSubmissionFinal> batch = new ArrayList<>(BATCH_SIZE);


        v1Submissions.parallelStream().forEach(v1Submission -> {
            try {
                final var submissionFinal = formDataMigrator.migrateV1ToV2(v1Submission, formTemplate);

//                    batch.add(submissionFinal);
                mongoTemplate.save(submissionFinal);

                // mark as processed
                v1Submission.setProcessed(true);
                v1Submission.setLastProcessedAt(System.currentTimeMillis());
                mongoTemplate.save(v1Submission);

//                    if (batch.size() >= BATCH_SIZE) {
//                        saveBatch(batch);
//                        batch.clear();
//                    }

                if (counter.incrementAndGet() % LOG_INTERVAL == 0) {
                    log.info("Processed {} submissions", counter.get());
                }
            } catch (Exception e) {
                log.info("Failed to process submission: {}", v1Submission.getUid(), e);
                log.error("Failed to process submission: {}", v1Submission.getId(), e);
                storeInDLQ(v1Submission, e);
            }
        });

        // **Ensure remaining batch is saved after the loop**
//        if (!batch.isEmpty()) {
//            saveBatch(batch);
//            batch.clear();
//        }
    }

    private void saveBatch(List<DataFormSubmissionFinal> batch) {
        try {
            BulkOperations bulkOps = mongoTemplate.bulkOps(
                BulkOperations.BulkMode.UNORDERED,
                DataFormSubmissionFinal.class
            );

            batch.forEach(bulkOps::insert);

            BulkWriteResult result = bulkOps.execute();
            log.info("Saved batch of {} docs", batch.size());
            log.debug("Saved batch of {} docs. Inserted: {}",
                batch.size(), result.getInsertedCount());

        } catch (Exception e) {
            log.error("Batch save failed for {} documents. Error: {}",
                batch.size(), e.getMessage());
            // Optionally retry individual documents
            retryFailedDocuments(batch, e);
        }
    }

    private void retryFailedDocuments(List<DataFormSubmissionFinal> batch, Exception originalError) {
        log.info("Retrying failed documents individually");

        batch.forEach(doc -> {
            try {
                mongoTemplate.save(doc);
            } catch (Exception e) {
                log.error("Permanent failure for document {}: {}",
                    doc.getUid(), e.getMessage());
                // Store in dead letter queue
                storeInDLQ(doc, originalError);
            }
        });
    }

    private <T extends WithIdentifierObject> void storeInDLQ(T doc, Exception error) {
        try {
            // Convert entity to Map
            DataFormSubmissionBu errorDocument = objectMapper.convertValue(doc, DataFormSubmissionBu.class);

            // Append error details
            errorDocument.setErrorMessage(error.getMessage());
            errorDocument.setErrorStackTrace(getStackTraceAsString(error));
            errorDocument.setFailedAt(System.currentTimeMillis()); // Store timestamp

            // Store in the custom error collection
            mongoTemplate.save(errorDocument, "data_form_submission_errors");

            log.info("Stored failed document {} in DLQ", doc.getUid());
        } catch (Exception e) {
            log.error("Failed to store document {} in DLQ: {}", doc.getUid(), e.getMessage());
        }
    }

    private String getStackTraceAsString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
