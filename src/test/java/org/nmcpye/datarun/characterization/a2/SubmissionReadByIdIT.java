package org.nmcpye.datarun.characterization.a2;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.Group.G3;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.item;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.ordinary;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.uid;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ADMIN;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T1_V1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FIELD_1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FORM_A;
import static org.nmcpye.datarun.characterization.a2.A2Seed.version;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * {@code GET /dataSubmission/{id}} is admin only (6.5.5), on the v1 and the custom mapping, through the production
 * filter chain with real tokens. A field user gets 403 even for the submission they uploaded, without a submission
 * in the body; an admin still reads it; and the G0 pull, {@code objects?paged=false} on the same controller, is
 * unchanged for a field user (A2 R1 pins its whole answer). Not a golden: it reuses the A2 seed.
 */
class SubmissionReadByIdIT extends A2GoldenSupport {

    private static final List<String> MAPPINGS = List.of("/api/v1/dataSubmission/", "/api/custom/dataSubmission/");

    @Test
    void aFieldUserIsRefusedEvenForTheirOwnSubmission() throws Exception {
        String uid = uploadedByFieldUser("rid1");

        for (String mapping : MAPPINGS) {
            MockHttpServletResponse response = read(mapping + uid, FIELD_1);
            assertThat(response.getStatus()).as(mapping).isEqualTo(403);
            JsonNode body = body(response);
            assertThat(body.path("message").asText()).as(mapping).isEqualTo("error.http.403");
            assertThat(body.has("formData")).as("no submission in the answer").isFalse();
        }
    }

    @Test
    void anAdminStillReadsIt() throws Exception {
        String uid = uploadedByFieldUser("rid2");

        for (String mapping : MAPPINGS) {
            MockHttpServletResponse response = read(mapping + uid, ADMIN);
            assertThat(response.getStatus()).as(mapping).isEqualTo(200);
            assertThat(body(response).path("uid").asText()).as(mapping).isEqualTo(uid);
        }
    }

    @Test
    void theG0PullIsUnchangedForAFieldUser() throws Exception {
        uploadedByFieldUser("rid3");

        MockHttpServletResponse response = read("/api/v1/dataSubmission/objects?paged=false", FIELD_1);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(body(response).path("totalElements").asInt(-1)).as("empty without an EDIT grant").isZero();
    }

    private String uploadedByFieldUser(String tag) throws Exception {
        String uid = uid(tag);
        String body = json.writeValueAsString(A2Payloads.body(item(G3, uid, FORM_A, version("FORM_A.v1"),
            ASSIGN_T1_V1, ordinary(G3, uid, "first", "r1", "s1", 1))));
        MockHttpServletResponse response = mvc.perform(post(A2Payloads.BULK_VERSIONED)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + A2Tokens.token(jwtKey, FIELD_1))
                .header("X-App-Version", "6.1.2+57")
                .content(body))
            .andReturn().getResponse();
        assertThat(body(response).path("created").path(0).asText()).as("the upload saved it").isEqualTo(uid);
        return uid;
    }

    private MockHttpServletResponse read(String path, String login) throws Exception {
        return mvc.perform(get(path).header("Authorization", "Bearer " + A2Tokens.token(jwtKey, login)))
            .andReturn().getResponse();
    }

    private JsonNode body(MockHttpServletResponse response) throws Exception {
        return json.readTree(response.getContentAsString(StandardCharsets.UTF_8));
    }
}
