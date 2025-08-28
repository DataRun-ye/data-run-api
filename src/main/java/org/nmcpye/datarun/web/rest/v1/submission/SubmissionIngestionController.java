//package org.nmcpye.datarun.web.rest.v1.submission;
//
//import lombok.RequiredArgsConstructor;
//import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
//import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
//import org.nmcpye.datarun.jpa.etl.exception.MissingRepeatUidException;
//import org.nmcpye.datarun.jpa.etl.service.SubmissionService;
//import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
///**
// * @author Hamza Assada 10/08/2025 (7amza.it@gmail.com)
// */
//@RestController
//@RequestMapping("/api/v1/submissions")
//@RequiredArgsConstructor
//public class SubmissionIngestionController {
//    private final DataSubmissionService submissionService;
//
//    @PostMapping
//    public ResponseEntity<?> ingest(@RequestBody DataFormSubmission dto) {
//        try {
//            // load template DTO for submission.getForm()
//            DataTemplateInstanceDto template = templateService.loadTemplateInstance(submission.getForm(), submission.getVersionUid());
//            submissionService.ingestSubmission(dto, template);
//            return ResponseEntity.status(HttpStatus.CREATED).build();
//        } catch (MissingRepeatUidException ex) {
//            return ResponseEntity.badRequest().body(Map.of("error","MissingRepeatUids","details", ex.getDetails()));
//        } catch (Exception ex) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error","InternalError"));
//        }
//
//        try {
//
//            submissionService.ingestSubmission(dto);
//            return ResponseEntity.status(HttpStatus.CREATED).build();
//        } catch (MissingRepeatUidException ex) {
//            return ResponseEntity.badRequest().body(
//                java.util.Map.of(
//                    "error", "MissingRepeatUids",
//                    "message", ex.getMessage(),
//                    "details", ex.getDetails()
//                )
//            );
//        } catch (Exception ex) {
//            // In production, log the exception; return a safe message
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(java.util.Map.of("error", "InternalError", "message", ex.getMessage()));
//        }
//    }
//}
