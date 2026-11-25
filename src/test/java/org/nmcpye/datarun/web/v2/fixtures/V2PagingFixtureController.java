package org.nmcpye.datarun.web.v2.fixtures;

import io.swagger.v3.oas.annotations.Hidden;
import org.nmcpye.datarun.web.v2.paging.Keyset;
import org.nmcpye.datarun.web.v2.paging.Page;
import org.nmcpye.datarun.web.v2.paging.PageQuery;
import org.nmcpye.datarun.web.v2.paging.PageResponses;
import org.nmcpye.datarun.web.v2.paging.QueryBinding;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

/**
 * A test-only v2 list over five fixed items, paged by keyset on their serial, filtered by group: the paging
 * convention end to end. Hidden from the OpenAPI document; it exists only on the test classpath.
 */
@Hidden
@RestController
@RequestMapping("/api/v2/fixtures/items")
public class V2PagingFixtureController {

    public record Item(long serial, String group) {
    }

    private static final List<Item> ITEMS =
        LongStream.rangeClosed(1, 5).mapToObj(serial -> new Item(serial, serial % 2 == 0 ? "even" : "odd")).toList();

    @GetMapping
    public ResponseEntity<Page<Item>> list(@RequestParam(required = false) String group, PageQuery page) {
        String binding = QueryBinding.of("fixture-items", group == null ? Map.of() : Map.of("group", List.of(group)));
        long after = page.after(binding).map(cursor -> cursor.longAt(0)).orElse(0L);
        List<Item> rows = ITEMS.stream()
            .filter(item -> group == null || item.group().equals(group))
            .filter(item -> item.serial() > after)
            .limit(page.fetchSize())
            .toList();
        return PageResponses.ok(Keyset.page(rows, page, binding, item -> List.of(item.serial()), item -> item));
    }
}
