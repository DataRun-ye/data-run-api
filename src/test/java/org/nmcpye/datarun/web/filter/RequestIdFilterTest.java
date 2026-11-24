package org.nmcpye.datarun.web.filter;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The request id in the log and in error reports: the client's when it is a UUID, else a new UUID. */
class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void aUuidFromTheClientIsKept() throws Exception {
        String uuid = UUID.randomUUID().toString();
        assertThat(served(uuid)).containsExactly(uuid, uuid);
        String upper = uuid.toUpperCase();
        assertThat(served(upper)).containsExactly(upper, upper);
    }

    @Test
    void anyOtherIdIsReplacedByANewOne() throws Exception {
        for (String sent : new String[]{
            "777000111", "phone-7f3a9c01", "abc\r\n2026-11-24 ERROR forged line", "login 777000111",
            UUID.randomUUID() + "0", "", "ïd"}) {
            List<String> seen = served(sent);
            assertThat(seen.get(0)).as(sent).isNotEqualTo(sent).satisfies(id -> UUID.fromString(id));
            assertThat(seen.get(1)).as("the response names the id the log used").isEqualTo(seen.get(0));
        }
    }

    @Test
    void withoutAnIdANewOneIsMade() throws Exception {
        List<String> seen = served(null);

        assertThat(seen.get(0)).satisfies(id -> UUID.fromString(id));
        assertThat(seen.get(1)).isEqualTo(seen.get(0));
    }

    /** The id the log saw while the request was served, then the one the response named; the MDC is cleared. */
    private List<String> served(String sentId) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/assignments");
        if (sentId != null) {
            request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, sentId);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        List<String> seen = new ArrayList<>();

        filter.doFilter(request, response, (req, res) -> seen.add(MDC.get(RequestIdFilter.MDC_REQUEST_ID_KEY)));

        seen.add(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER));
        assertThat(MDC.get(RequestIdFilter.MDC_REQUEST_ID_KEY)).isNull();
        return seen;
    }
}
