package org.nmcpye.datarun.web.v2;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The placement check's two rules: which patterns can answer v2 paths, and which packages are web.v2. */
class V2PlacementRuleTest {

    @Test
    void onlyWebV2AndItsSubpackagesAreWebV2() {
        assertThat(V2HandlerPlacementIT.inWebV2("org.nmcpye.datarun.web.v2")).isTrue();
        assertThat(V2HandlerPlacementIT.inWebV2("org.nmcpye.datarun.web.v2.fixtures")).isTrue();
        assertThat(V2HandlerPlacementIT.inWebV2("org.nmcpye.datarun.web.v2x")).isFalse();
        assertThat(V2HandlerPlacementIT.inWebV2("org.nmcpye.datarun.web.v2x.things")).isFalse();
        assertThat(V2HandlerPlacementIT.inWebV2("org.nmcpye.datarun.web.rest.v1")).isFalse();
    }

    @Test
    void patternsThatCanAnswerAV2PathCount() {
        for (String reaching : new String[] {"/api/v2", "/api/v2/things", "/api/v2/things/{id}:touch",
            "/api/v2/things:search", "/api/{version}/things", "/{anything}/**", "/**", "/api/v{n}/x"}) {
            assertThat(V2HandlerPlacementIT.canReachV2(reaching)).as(reaching).isTrue();
        }
        for (String elsewhere : new String[] {"/api/v1/things", "/api/v2x/things", "/api/v2x/{id}",
            "/api/v20/{id}", "/api/custom/{x}", "/management/{x}", "/api/v2:probe"}) {
            assertThat(V2HandlerPlacementIT.canReachV2(elsewhere)).as(elsewhere).isFalse();
        }
    }
}
