package org.nmcpye.datarun.jpa.datasubmission.service;

import jakarta.transaction.Transactional;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.mongo.service.impl.SequenceGeneratorService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataSubmissionMaintenanceService {

    private final SequenceGeneratorService sequenceGeneratorService;
    private final DataSubmissionRepository submissionRepository;

    public DataSubmissionMaintenanceService(SequenceGeneratorService sequenceGeneratorService,
                                            DataSubmissionRepository submissionRepository) {
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.submissionRepository = submissionRepository;
    }

    /**
     * Finds and fixes form data submissions that don't have a serial number assigned.
     * This method fetches all submissions without a serial number, generates a unique
     * serial number for each, and updates the submissions in the database.
     * This method is transactional, ensuring that all operations are completed
     * successfully or rolled back in case of an error.
     *
     * @throws RuntimeException if there's an error during the database operations
     */
    @Transactional
    public void findAndFixFormDataSerialNumbers() {
        // Fetch all submissions that don't have a serial number assigned
        List<DataSubmission> submissions = submissionRepository.findBySerialNumberIsNull();

        for (DataSubmission submission : submissions) {
            // Generate a unique serial number
            long serialNumber = sequenceGeneratorService.getNextSequence("dataFormSubmissionId");

            // Set the serial number
            submission.setSerialNumber(serialNumber);


            // Save the updated submission
            submissionRepository.save(submission);
        }
    }
}
