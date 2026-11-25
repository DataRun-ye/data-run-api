package org.nmcpye.datarun.web.v2.paging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/** The paging convention through the running application, on a test-only list of five items. */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.USER)
class V2PagingIT {

    private static final String ITEMS = "/api/v2/fixtures/items";
    private static final Pattern NEXT_LINK = Pattern.compile("^<([^>]+)>; rel=\"next\"$");

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper json = new ObjectMapper();

    private MockHttpServletResponse get(String uri) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get(uri))
            .andReturn().getResponse();
    }

    @Test
    void pagesFollowTheNextLinkToTheEndWithoutTotals() throws Exception {
        MockHttpServletResponse first = get(ITEMS + "?group=odd&limit=2");
        assertThat(first.getStatus()).isEqualTo(200);
        JsonNode body = json.readTree(first.getContentAsByteArray());
        assertThat(body.get("items").findValuesAsText("serial")).containsExactly("1", "3");
        assertThat(body.fieldNames()).toIterable().containsExactlyInAnyOrder("items", "next");
        Matcher link = NEXT_LINK.matcher(first.getHeader(HttpHeaders.LINK));
        assertThat(link.matches()).isTrue();
        assertThat(link.group(1)).startsWith(ITEMS + "?group=odd&limit=2&cursor=")
            .endsWith(body.get("next").asText());

        MockHttpServletResponse last = get(link.group(1));
        JsonNode lastBody = json.readTree(last.getContentAsByteArray());
        assertThat(lastBody.get("items").findValuesAsText("serial")).containsExactly("5");
        assertThat(lastBody.has("next")).as("next is always present").isTrue();
        assertThat(lastBody.get("next").isNull()).isTrue();
        assertThat(last.getHeader(HttpHeaders.LINK)).isNull();
    }

    @Test
    void theDefaultLimitIs50() throws Exception {
        JsonNode body = json.readTree(get(ITEMS).getContentAsByteArray());
        assertThat(body.get("items")).hasSize(5);
        assertThat(body.get("next").isNull()).isTrue();
    }

    @Test
    void aCursorOnlyContinuesItsOwnQuery() throws Exception {
        JsonNode odd = json.readTree(get(ITEMS + "?group=odd&limit=1").getContentAsByteArray());
        String cursor = odd.get("next").asText();

        assertThat(get(ITEMS + "?group=odd&limit=3&cursor=" + cursor).getStatus())
            .as("the page size may change between pages").isEqualTo(200);

        MockHttpServletResponse otherQuery = get(ITEMS + "?group=even&cursor=" + cursor);
        assertThat(otherQuery.getStatus()).isEqualTo(400);
        JsonNode problem = json.readTree(otherQuery.getContentAsByteArray());
        assertThat(problem.get("code").asText()).isEqualTo("invalid-cursor");
        assertThat(otherQuery.getContentAsString()).doesNotContain(cursor);

        assertThat(json.readTree(get(ITEMS + "?cursor=garbage").getContentAsByteArray()).get("code").asText())
            .isEqualTo("invalid-cursor");
    }

    @Test
    void aLimitOutsideOneTo200IsMalformed() throws Exception {
        for (String limit : new String[] {"0", "201", "abc"}) {
            MockHttpServletResponse response = get(ITEMS + "?limit=" + limit);
            assertThat(response.getStatus()).as("limit=%s", limit).isEqualTo(400);
            JsonNode problem = json.readTree(response.getContentAsByteArray());
            assertThat(problem.get("code").asText()).isEqualTo("bad-request");
            assertThat(problem.get("errors").get(0).get("parameter").asText()).isEqualTo("limit");
        }
    }
}
