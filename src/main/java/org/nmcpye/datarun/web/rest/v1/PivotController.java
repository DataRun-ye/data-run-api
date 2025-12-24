package org.nmcpye.datarun.web.rest.v1;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.etl.pivot.BuildResponse;
import org.nmcpye.datarun.etl.pivot.PivotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pivot")
@RequiredArgsConstructor
public class PivotController {
    private final PivotService pivotService;

    @PostMapping("/build_by_template")
    public ResponseEntity<BuildResponse> buildByTemplate(@RequestParam("template_uid") String templateUid) {
        BuildResponse resp = pivotService.buildTemplate(templateUid);
        return ResponseEntity.ok(resp);
    }
}
