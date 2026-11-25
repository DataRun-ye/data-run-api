package org.nmcpye.datarun.web.v2;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/** A v2 request is recognized on the decoded path, as MVC routes it, however its characters are encoded. */
class ApiV2Test {

    private static boolean v2(String rawUri) {
        return ApiV2.isV2(new MockHttpServletRequest("GET", rawUri));
    }

    @Test
    void encodedSpellingsOfTheBaseAreV2() {
        for (String uri : new String[] {"/api/v2", "/api/v2/", "/api/v2/things", "/api/v2/things/a:touch",
            "/api/%762/things", "/api/v%32/things", "/api/%76%32/things",
            "/api/v2/things%3Asearch"}) {
            assertThat(v2(uri)).as(uri).isTrue();
        }
    }

    @Test
    void otherPathsAreNot() {
        for (String uri : new String[] {"/api/v1/things", "/api/v2x", "/api/v20/things", "/apiv2/things", "/api",
            "/api/custom/v2", "/management/health", "/api/V2/things"}) {
            assertThat(v2(uri)).as(uri).isFalse();
        }
    }

    @Test
    void theContextPathIsLeftOut() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ctx/api/%762/things");
        request.setContextPath("/ctx");
        assertThat(ApiV2.isV2(request)).isTrue();
    }
}
