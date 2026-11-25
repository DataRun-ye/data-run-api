package org.nmcpye.datarun.web.v2.concurrency;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.web.v2.problem.ApiProblemException;
import org.nmcpye.datarun.web.v2.problem.ProblemCode;
import org.springframework.http.HttpHeaders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Strong entity tags and the If-Match rules of v2 commands: a missing header or {@code *} is 428, a value that is
 * not a list of entity tags 400, a stale or weak tag 412 with the current version and tag.
 */
class IfMatchTest {

    @Test
    void aVersionsTagIsTheVersionInQuotes() {
        assertThat(EntityTag.ofVersion(0)).hasToString("\"0\"");
        assertThat(EntityTag.ofVersion(42)).hasToString("\"42\"");
        assertThatThrownBy(() -> EntityTag.ofVersion(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void strongComparisonNeverMatchesAWeakTag() {
        EntityTag strong = new EntityTag("3", false);
        assertThat(strong.strongMatches(new EntityTag("3", false))).isTrue();
        assertThat(strong.strongMatches(new EntityTag("3", true))).isFalse();
        assertThat(new EntityTag("3", true).strongMatches(new EntityTag("3", true))).isFalse();
        assertThat(strong.strongMatches(new EntityTag("4", false))).isFalse();
    }

    @Test
    void theFieldIsParsedAsRfc9110Lists() {
        assertThat(IfMatch.parse(null)).isNull();
        assertThat(IfMatch.parse(List.of())).isNull();
        assertThat(IfMatch.parse(List.of("*")).any()).isTrue();
        assertThat(IfMatch.parse(List.of(" \"3\" ")).tags()).containsExactly(new EntityTag("3", false));
        assertThat(IfMatch.parse(List.of("\"3\", W/\"4\",\"a,b\"")).tags())
            .containsExactly(new EntityTag("3", false), new EntityTag("4", true), new EntityTag("a,b", false));
        assertThat(IfMatch.parse(List.of("\"1\"", "\"2\"")).tags())
            .as("several field lines form one list").hasSize(2);
        assertThat(IfMatch.parse(List.of("\"3\", W/\"4\"")).canonical()).isEqualTo("\"3\", W/\"4\"");
        assertThat(IfMatch.parse(List.of("\"3\"  ,  ")).canonical()).isEqualTo("\"3\"");
    }

    @Test
    void aValueThatIsNotAListOfEntityTagsIs400() {
        for (String value : List.of("3", "\"3", "W\"3\"", "\"3\" \"4\"", "\"3\" x", "*, \"3\"", ",", "", "w/\"3\"",
            "\"a b\"")) {
            ApiProblemException problem = catchThrowableOfType(ApiProblemException.class,
                () -> IfMatch.parse(List.of(value)));
            assertThat(problem).as("If-Match: %s", value).isNotNull();
            assertThat(problem.code()).isEqualTo(ProblemCode.INVALID_IF_MATCH);
        }
    }

    @Test
    void aCommandNeedsTagsSoAMissingHeaderOrAWildcardIs428() {
        assertThat(catchThrowableOfType(ApiProblemException.class, () -> IfMatch.required(null)).code())
            .isEqualTo(ProblemCode.PRECONDITION_REQUIRED);
        assertThat(catchThrowableOfType(ApiProblemException.class,
            () -> IfMatch.required(IfMatch.parse(List.of("*")))).code())
            .isEqualTo(ProblemCode.PRECONDITION_REQUIRED);
        IfMatch tags = IfMatch.parse(List.of("\"3\""));
        assertThat(IfMatch.required(tags)).isSameAs(tags);
    }

    @Test
    void anyMatchingStrongTagPasses() {
        IfMatch.parse(List.of("\"3\"")).check(3, null);
        IfMatch.parse(List.of("\"2\", \"3\"")).check(3, null);
    }

    @Test
    void aStaleOrWeakTagIs412WithTheCurrentVersionTagAndHistoryLink() {
        for (String value : List.of("\"2\"", "W/\"3\"", "\"03\"")) {
            ApiProblemException stale = catchThrowableOfType(ApiProblemException.class,
                () -> IfMatch.parse(List.of(value)).check(3, "/api/v2/things/7/versions"));
            assertThat(stale).as("If-Match: %s at version 3", value).isNotNull();
            assertThat(stale.code()).isEqualTo(ProblemCode.PRECONDITION_FAILED);
            assertThat(stale.extensions()).containsEntry("currentVersion", 3L);
            assertThat(stale.headers().getFirst(HttpHeaders.ETAG)).isEqualTo("\"3\"");
            assertThat(stale.headers().getFirst(HttpHeaders.LINK))
                .isEqualTo("</api/v2/things/7/versions>; rel=\"version-history\"");
        }
        ApiProblemException withoutLink = catchThrowableOfType(ApiProblemException.class,
            () -> IfMatch.parse(List.of("\"2\"")).check(3, null));
        assertThat(withoutLink.headers().containsKey(HttpHeaders.LINK)).isFalse();
    }
}
