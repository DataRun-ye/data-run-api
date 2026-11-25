package org.nmcpye.datarun.web.v2;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every handler that can answer a path under {@code /api/v2} is declared in {@code web.v2}, where the ArchUnit
 * rules, the OpenAPI contract test's slice and the conventions check see it. A pattern whose literal start could
 * still reach {@code /api/v2} (such as {@code /api/{version}/...}) counts too.
 */
@IntegrationTest
class V2HandlerPlacementIT {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mappings;

    @Test
    void everyV2HandlerLivesInWebV2() {
        List<String> misplaced = new ArrayList<>();
        int v2Handlers = 0;
        for (Map.Entry<RequestMappingInfo, HandlerMethod> mapping : mappings.getHandlerMethods().entrySet()) {
            for (String pattern : mapping.getKey().getPatternValues()) {
                if (!canReachV2(pattern)) {
                    continue;
                }
                v2Handlers++;
                Class<?> type = mapping.getValue().getBeanType();
                if (!inWebV2(type.getPackageName())) {
                    misplaced.add(pattern + " in " + type.getName());
                }
            }
        }
        assertThat(v2Handlers).as("the test fixtures are v2 handlers").isPositive();
        assertThat(misplaced).isEmpty();
    }

    /** The package web.v2 itself or one below it; never a sibling such as web.v2x. */
    static boolean inWebV2(String packageName) {
        return packageName.equals(WEB_V2) || packageName.startsWith(WEB_V2 + ".");
    }

    private static final String WEB_V2 = "org.nmcpye.datarun.web.v2";

    /**
     * Whether the pattern can answer a v2 path ({@link ApiV2#isV2}: /api/v2 or below it). A literal pattern must be
     * the base or under it; a pattern with a variable or wildcard counts when its literal start is still a prefix of
     * {@code /api/v2/} (such as {@code /api/{version}/...}) or is the base or under it. {@code /api/v2x/...} is not.
     */
    static boolean canReachV2(String pattern) {
        int open = indexOfAny(pattern, '{', '*', '?');
        String literal = open < 0 ? pattern : pattern.substring(0, open);
        boolean baseOrBelow = literal.equals(ApiV2.BASE) || literal.startsWith(ApiV2.BASE + "/");
        return open < 0 ? baseOrBelow : baseOrBelow || (ApiV2.BASE + "/").startsWith(literal);
    }

    private static int indexOfAny(String text, char... characters) {
        int first = -1;
        for (char c : characters) {
            int at = text.indexOf(c);
            if (at >= 0 && (first < 0 || at < first)) {
                first = at;
            }
        }
        return first;
    }
}
