package org.nmcpye.datarun.drun.mongo.service.submissionmigration;

import jakarta.transaction.Transactional;
import org.nmcpye.datarun.drun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.drun.mongo.repository.DataFormSubmissionRepositoryCustom;
import org.nmcpye.datarun.drun.mongo.service.impl.SequenceGeneratorService;
import org.nmcpye.datarun.utils.CodeGenerator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DataFormSubmissionMigrationService {

    private final SequenceGeneratorService sequenceGeneratorService;
    private final DataFormSubmissionRepositoryCustom submissionRepository;

    public DataFormSubmissionMigrationService(SequenceGeneratorService sequenceGeneratorService,
                                              DataFormSubmissionRepositoryCustom submissionRepository) {
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.submissionRepository = submissionRepository;
    }

    @Transactional
    public void migrateAndAssignSerialNumbers() {
        // Fetch all submissions that don't have a serial number assigned
        List<DataFormSubmission> submissions = submissionRepository.findBySerialNumberNull();

        for (DataFormSubmission submission : submissions) {
            // Generate a unique serial number
            long serialNumber = sequenceGeneratorService.getNextSequence("dataFormSubmissionId");

            // Set the serial number
            submission.setSerialNumber(serialNumber);


            // Save the updated submission
            submissionRepository.save(submission);
        }
    }

    @Transactional
    public void migrateAndAddGroupIndices() {
        // Fetch all submissions that have formData containing arrays of objects
        List<DataFormSubmission> submissions = submissionRepository.findAll();

        for (DataFormSubmission submission : submissions) {
            Map<String, Object> formData = submission.getFormData();

            final Object id = CodeGenerator.generateCode(15);
            formData.put("_id", id);
            formData.remove("uid");
            formData.remove("_uid");
            formData.remove("_uuid");
            formData.remove("_formDataUid");

            // Recursively process formData to add group indices to arrays of objects
            Map<String, Object> updatedFormData = addGroupIndicesToFormData(formData, id);

            // Update formData in submission
            submission.setFormData(updatedFormData);

            // Save updated submission
            submissionRepository.save(submission);
        }
    }

    private Map<String, Object> addGroupIndicesToFormData(Map<String, Object> formData, Object parentId) {
        Map<String, Object> updatedFormData = new HashMap<>();
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            Object value = entry.getValue();

            // If it's an array of objects, add group indices
            if (value instanceof List) {
                List<?> list = (List<?>) value;
                if (!list.isEmpty() && list.get(0) instanceof Map) {
                    List<Map<String, Object>> updatedList = new ArrayList<>();
                    for (int i = 0; i < list.size(); i++) {
                        Map<String, Object> objectInArray = (Map<String, Object>) list.get(i);
                        objectInArray.put("_parentId", parentId);
                        objectInArray.put("_id", CodeGenerator.generateCode(16));  // Add groupIndex (s
                        // Add groupIndex (starting from 1)
                        objectInArray.put("_index", i + 1);  // Add repeatIndex (starting from 1)

                        objectInArray.remove("repeatUid");  // Add repeatIndex (starting from 1)
                        objectInArray.remove("index");  // Add repeatIndex (starting from 1)
                        objectInArray.remove("repeatIndex");  // Add repeatIndex (starting from 1)
                        objectInArray.remove("parentUid");  // Add repeatIndex (starting from 1)
                        objectInArray.remove("_formDataUid");  // Add repeatIndex (starting from 1)

                        updatedList.add(objectInArray);
                    }
                    updatedFormData.put(entry.getKey(), updatedList);
                } else {
                    // If it's not an array of objects, just copy as is
                    updatedFormData.put(entry.getKey(), list);
                }
            } else if (value instanceof Map) {
                // If it's a nested map, recursively process it
                ((Map<String, Object>) value).remove("uid");
                ((Map<String, Object>) value).remove("_uid");
                ((Map<String, Object>) value).remove("_uuid");
                ((Map<String, Object>) value).remove("_formDataUid");
                updatedFormData.put(entry.getKey(), addGroupIndicesToFormData((Map<String, Object>) value, parentId));
            } else {
                // If it's a simple value, just copy as is
                updatedFormData.put(entry.getKey(), value);
            }
        }
        return updatedFormData;
    }
}
