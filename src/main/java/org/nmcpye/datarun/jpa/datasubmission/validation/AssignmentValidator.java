package org.nmcpye.datarun.jpa.datasubmission.validation;

import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.mongo.accessfilter.FormAccessService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Hamza Assada 15/08/2025 (7amza.it@gmail.com)
 */
@Component
public class AssignmentValidator implements SubmissionValidator {

    private final AssignmentRepository assignmentRepository;
    private final FormAccessService formAccessService;

    public AssignmentValidator(AssignmentRepository assignmentRepository, FormAccessService formAccessService) {
        this.assignmentRepository = assignmentRepository;
        this.formAccessService = formAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public DataSubmission validateAndEnrich(DataSubmission submission) {
        String assignmentUid = submission.getAssignment();
        if (assignmentUid == null) {
            throw new DomainValidationException("Assignment is required");
        }

        Assignment assignment = assignmentRepository.findByUid(assignmentUid)
            .orElseThrow(() -> new DomainValidationException("Assignment not found: " + assignmentUid));

        // Enrich submission with assignment details (team/orgUnit) for easy query and later processing
        submission.setAssignment(assignment.getUid());
        submission.setTeam(assignment.getTeam().getUid());
        submission.setTeamCode(assignment.getTeam().getCode());
        submission.setOrgUnit(assignment.getOrgUnit().getUid());
        submission.setOrgUnitCode(assignment.getOrgUnit().getCode());
        submission.setOrgUnitName(assignment.getOrgUnit().getName());
        submission.setActivity(assignment.getActivity().getUid());

        return submission;
    }
}
