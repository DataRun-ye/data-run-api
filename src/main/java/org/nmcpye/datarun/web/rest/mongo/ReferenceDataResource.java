package org.nmcpye.datarun.web.rest.mongo;

import org.nmcpye.datarun.drun.mongo.domain.ReferenceData;
import org.nmcpye.datarun.drun.mongo.repository.SubmissionReferenceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/custom/referenceData")
public class ReferenceDataResource {
    final SubmissionReferenceRepository referenceRepository;

    public ReferenceDataResource(
        SubmissionReferenceRepository referenceRepository) {

        this.referenceRepository = referenceRepository;
    }

    @GetMapping("")
    public ResponseEntity<Set<ReferenceData>> getAllByCurrentUser() {
        return ResponseEntity.ok(referenceRepository.getTeamReferenceData());
    }
}
