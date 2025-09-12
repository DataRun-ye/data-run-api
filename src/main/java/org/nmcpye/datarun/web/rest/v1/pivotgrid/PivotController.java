//package org.nmcpye.datarun.web.rest.v1.pivotgrid;
//
//import lombok.RequiredArgsConstructor;
//import org.nmcpye.datarun.analytics.dto.PivotMetadataResponse;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
///**
// * @author Hamza Assada 17/08/2025 (7amza.it@gmail.com)
// */
//@RestController
//@RequestMapping("/api/pivot")
//@RequiredArgsConstructor
//public class PivotController {
//
//    private final PivotService pivotService;
//
//    @GetMapping("/metadata")
//    public ResponseEntity<PivotMetadataResponse> getPivotMetadata() {
//        PivotMetadataResponse metadata = pivotService.getMetadata();
//        return ResponseEntity.ok(metadata);
//    }
//}
