package org.nmcpye.datarun.web.v2.paging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.web.v2.problem.ApiProblemException;
import org.nmcpye.datarun.web.v2.problem.ProblemCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/** Keyset paging: limits, cursors bound to their query, pages without totals, and the next link. */
class PagingTest {

    private static final String BINDING = QueryBinding.of("things", Map.of("group", List.of("a")));

    @AfterEach
    void noRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void theLimitDefaultsTo50AndIsAWholeNumberFrom1To200() {
        assertThat(PageQuery.of(null, null)).isEqualTo(new PageQuery(50, null));
        assertThat(PageQuery.of("", "")).isEqualTo(new PageQuery(50, null));
        assertThat(PageQuery.of("1", null).limit()).isEqualTo(1);
        assertThat(PageQuery.of("200", null).limit()).isEqualTo(200);
        assertThat(PageQuery.of("10", null).fetchSize()).isEqualTo(11);
        for (String bad : List.of("0", "201", "-1", "abc", "1.5", "99999999999")) {
            ApiProblemException problem = catchThrowableOfType(ApiProblemException.class, () -> PageQuery.of(bad, null));
            assertThat(problem).as("limit=%s", bad).isNotNull();
            assertThat(problem.code()).isEqualTo(ProblemCode.BAD_REQUEST);
            assertThat(problem.errors().get(0).parameter()).isEqualTo("limit");
        }
    }

    @Test
    void aCursorRoundTripsItsKeysWithinItsQuery() {
        Instant at = Instant.parse("2026-12-03T14:02:11.123456Z");
        String token = CursorCodec.encode(BINDING, List.of(42L, "01JDQ3ZK5V8Y7M2N4P6R8T0W2X", at, 7));
        assertThat(token).matches("[A-Za-z0-9_-]+");

        Cursor cursor = PageQuery.of(null, token).after(BINDING).orElseThrow();
        assertThat(cursor.size()).isEqualTo(4);
        assertThat(cursor.longAt(0)).isEqualTo(42L);
        assertThat(cursor.stringAt(1)).isEqualTo("01JDQ3ZK5V8Y7M2N4P6R8T0W2X");
        assertThat(cursor.instantAt(2)).isEqualTo(at);
        assertThat(cursor.longAt(3)).isEqualTo(7L);
        assertThat(PageQuery.of(null, null).after(BINDING)).isEmpty();
    }

    @Test
    void aCursorOfAnotherQueryOrNoCursorAtAllIsRefusedWithoutRepeatingIt() {
        String token = CursorCodec.encode(BINDING, List.of(42L));
        String otherQuery = QueryBinding.of("things", Map.of("group", List.of("b")));
        String otherList = QueryBinding.of("others", Map.of("group", List.of("a")));
        List<String> refused = List.of(
            "%%%",
            Base64.getUrlEncoder().encodeToString("not json".getBytes(StandardCharsets.UTF_8)),
            b64("{\"v\":2,\"b\":\"" + BINDING + "\",\"k\":[1]}"),
            b64("{\"v\":1,\"b\":\"" + BINDING + "\",\"k\":[]}"),
            b64("{\"v\":1,\"b\":\"" + BINDING + "\",\"k\":[1.5]}"),
            b64("{\"v\":1,\"b\":\"" + BINDING + "\",\"k\":[{\"x\":1}]}"),
            b64("[1,2]"),
            token.substring(0, token.length() - 3));
        for (String bad : refused) {
            assertInvalid(() -> CursorCodec.decode(bad, BINDING), bad);
        }
        assertInvalid(() -> CursorCodec.decode(token, otherQuery), token);
        assertInvalid(() -> CursorCodec.decode(token, otherList), token);
        assertInvalid(() -> PageQuery.of(null, "a".repeat(CursorCodec.MAX_LENGTH + 1)), "a".repeat(20));
        Cursor cursor = CursorCodec.decode(token, BINDING);
        assertInvalid(() -> cursor.stringAt(0), token);
        assertInvalid(() -> cursor.longAt(1), token);
        assertInvalid(() -> CursorCodec.decode(CursorCodec.encode(BINDING, List.of("x")), BINDING).instantAt(0), token);
    }

    @Test
    void aBindingIgnoresTheOrderOfFiltersAndValuesButNotTheirContent() {
        String one = QueryBinding.of("things", Map.of("team", List.of("t1", "t2"), "form", List.of("f")));
        String two = QueryBinding.of("things", Map.of("form", List.of("f"), "team", List.of("t2", "t1")));
        assertThat(one).isEqualTo(two).hasSize(16);
        assertThat(QueryBinding.of("things", Map.of("form", List.of("f"), "team", List.of()))).isEqualTo(
            QueryBinding.of("things", Map.of("form", List.of("f"))));
        assertThat(QueryBinding.of("things", Map.of("ab", List.of("c"))))
            .isNotEqualTo(QueryBinding.of("things", Map.of("a", List.of("bc"))));
        assertThat(QueryBinding.of("things", Map.of("form", List.of("f"))))
            .isNotEqualTo(QueryBinding.of("things", Map.of("form", List.of("g"))));
    }

    @Test
    void aKeysetPageShowsTheLimitAndContinuesAfterTheLastRowShown() {
        PageQuery query = PageQuery.of("2", null);
        List<Long> rows = List.of(10L, 20L, 30L);

        Page<String> page = Keyset.page(rows, query, BINDING, row -> List.of(row), row -> "item-" + row);

        assertThat(page.items()).containsExactly("item-10", "item-20");
        assertThat(CursorCodec.decode(page.next(), BINDING).longAt(0)).isEqualTo(20L);
        assertThat(Keyset.page(List.of(10L, 20L), query, BINDING, row -> List.of(row), row -> row).next())
            .as("exactly the limit: the last page").isNull();
        assertThat(Keyset.page(List.of(), query, BINDING, row -> List.of(row), row -> row).items()).isEmpty();
        List<Long> tooMany = LongStream.rangeClosed(1, 4).boxed().toList();
        assertThatThrownBy(() -> Keyset.page(tooMany, query, BINDING, row -> List.of(row), row -> row))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aGetPageLinksItsNextPageWithTheSameFiltersAndNoOtherResponseDoes() {
        MockHttpServletRequest get = new MockHttpServletRequest("GET", "/api/v2/things");
        get.setQueryString("group=a&limit=2&cursor=old");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(get));

        ResponseEntity<Page<String>> response = PageResponses.ok(new Page<>(List.of("x"), "next-token"));
        assertThat(response.getHeaders().getFirst(HttpHeaders.LINK))
            .isEqualTo("</api/v2/things?group=a&limit=2&cursor=next-token>; rel=\"next\"");
        assertThat(PageResponses.ok(new Page<>(List.of("x"), null)).getHeaders().containsKey(HttpHeaders.LINK))
            .as("last page").isFalse();

        MockHttpServletRequest post = new MockHttpServletRequest("POST", "/api/v2/things/search");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(post));
        assertThat(PageResponses.ok(new Page<>(List.of("x"), "next-token")).getHeaders().containsKey(HttpHeaders.LINK))
            .as("a search body carries next in the body only").isFalse();
    }

    private static String b64(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertInvalid(Runnable decode, String token) {
        ApiProblemException problem = catchThrowableOfType(ApiProblemException.class, decode::run);
        assertThat(problem).as("refused: %s", token).isNotNull();
        assertThat(problem.code()).isEqualTo(ProblemCode.INVALID_CURSOR);
        assertThat(problem.detail()).doesNotContain(token);
    }
}
