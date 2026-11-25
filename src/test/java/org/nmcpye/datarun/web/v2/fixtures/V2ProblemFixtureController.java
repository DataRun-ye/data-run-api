package org.nmcpye.datarun.web.v2.fixtures;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.nmcpye.datarun.web.v2.problem.ApiProblemException;
import org.nmcpye.datarun.web.v2.problem.ProblemCode;
import org.nmcpye.datarun.web.v2.problem.ProblemError;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Test-only v2 routes that fail in each way a v2 handler can, so the problem answers are checked through the real
 * filter chain and dispatcher. Hidden from the OpenAPI document; it exists only on the test classpath.
 */
@Hidden
@RestController
@RequestMapping("/api/v2/fixtures/problems")
public class V2ProblemFixtureController {

    public static final String SECRET = "s3cr3t-value-0779";

    public record Body(@NotBlank String name, @Min(1) Integer count) {
    }

    @GetMapping("/refusal")
    public Map<String, Object> refusal() {
        throw ApiProblemException.of(ProblemCode.VALIDATION_FAILED)
            .detail("One finding.")
            .error(ProblemError.atParameter("count", "too-small", "Must be at least 1."));
    }

    @PostMapping(path = "/body", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Body body(@Valid @RequestBody Body body) {
        return body;
    }

    @GetMapping("/typed")
    public Map<String, Object> typed(@RequestParam int count) {
        return Map.of("count", count);
    }

    public record Filter(@Min(1) Integer count) {
    }

    /** Query parameters bound to an object and validated: findings locate parameters, never repeat values. */
    @GetMapping("/filtered")
    public Map<String, Object> filtered(@Valid Filter filter) {
        return Map.of("count", filter.count());
    }

    @GetMapping("/fault")
    public Map<String, Object> fault() {
        throw new IllegalStateException("failed while holding " + SECRET);
    }
}
