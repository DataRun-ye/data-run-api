package org.nmcpye.datarun.mongo.service.submissionmigration;

import jakarta.transaction.Transactional;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.repository.DataFormSubmissionRepository;
import org.nmcpye.datarun.mongo.service.impl.SequenceGeneratorService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubmissionMaintenanceService {

    private final SequenceGeneratorService sequenceGeneratorService;
    private final DataFormSubmissionRepository submissionRepository;

    public SubmissionMaintenanceService(SequenceGeneratorService sequenceGeneratorService,
                                        DataFormSubmissionRepository submissionRepository) {
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

    /**
     * Finds and fixes the indices of repeat items in all form data submissions.
     * This method fetches all submissions, populates their form data attributes,
     * and saves the updated submissions back to the database.
     * This operation is particularly useful for ensuring that all repeat items
     * in the form data have correct and consistent indices. It's designed to be
     * run as a maintenance task to correct any inconsistencies in the data.
     * This method is transactional, ensuring that all operations are completed
     * successfully or rolled back in case of an error.
     *
     * @throws RuntimeException if there's an error during the database operations
     */
    @Transactional
    public void findAndFixRepeatItemsIndices() {
        // Fetch all submissions that have formData containing arrays of objects
        List<DataFormSubmission> submissions = submissionRepository.findAll();

        for (DataFormSubmission submission : submissions) {
            submission.createSubmission().populateFormDataAttributes();
            submissionRepository.save(submission);
        }
    }
}
