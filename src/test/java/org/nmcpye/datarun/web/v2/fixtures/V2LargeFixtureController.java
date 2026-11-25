package org.nmcpye.datarun.web.v2.fixtures;

import io.swagger.v3.oas.annotations.Hidden;
import org.nmcpye.datarun.web.v2.concurrency.EntityTag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * A test-only versioned v2 resource large enough (over 1 KB) for the production profile's response compression to
 * apply, with a strong entity tag. It exists only on the test classpath.
 */
@Hidden
@RestController
public class V2LargeFixtureController {

    @GetMapping("/api/v2/fixtures/large")
    public ResponseEntity<Map<String, Object>> large() {
        return ResponseEntity.ok().eTag(EntityTag.ofVersion(5).toString())
            .body(Map.of("version", 5, "text", "x".repeat(4096)));
    }
}
