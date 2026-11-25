package org.nmcpye.datarun.web.v2.fixtures;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Test-only v2 routes in the shapes the API naming rules use: a resource read by id, actions on it as
 * {@code POST /{id}:verb} (AIP-136), an action on the collection ({@code :search}), and a sub-collection next to
 * them. Each answers which route ran and with which id, so a test sees exactly how Spring routed the path. Hidden
 * from the OpenAPI document; it exists only on the test classpath.
 * <p>
 * The class maps the parent, not the collection: a method mapping {@code ":search"} under a class mapped at
 * {@code /things} would combine to {@code /things/:search}, so a collection action needs the collection in the
 * method's own pattern.
 */
@Hidden
@RestController
@RequestMapping("/api/v2/fixtures")
public class V2ActionRoutingFixtureController {

    @GetMapping("/things/{id}")
    public Map<String, String> read(@PathVariable String id) {
        return Map.of("route", "read", "id", id);
    }

    @PostMapping("/things/{id}:touch")
    public Map<String, String> touch(@PathVariable String id) {
        return Map.of("route", "touch", "id", id);
    }

    @PostMapping("/things/{id}:tidyUp")
    public Map<String, String> tidyUp(@PathVariable String id) {
        return Map.of("route", "tidyUp", "id", id);
    }

    @PostMapping("/things:search")
    public Map<String, String> search() {
        return Map.of("route", "search");
    }

    @GetMapping("/things/{id}/notes")
    public Map<String, String> notes(@PathVariable String id) {
        return Map.of("route", "notes", "id", id);
    }

    /** The pitfall, kept to prove it: mapped at the collection, {@code ":probe"} lands at {@code /combined/:probe}. */
    @Hidden
    @RestController
    @RequestMapping("/api/v2/fixtures/combined")
    public static class MappedAtTheCollection {

        @PostMapping(":probe")
        public Map<String, String> probe() {
            return Map.of("route", "probe");
        }
    }
}
