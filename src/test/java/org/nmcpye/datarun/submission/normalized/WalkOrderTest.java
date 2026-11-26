package org.nmcpye.datarun.submission.normalized;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The one walk rule: objects' keys by {@link String#compareTo}, and data paths compared key by key. */
class WalkOrderTest {

    @Test
    void pathsCompareKeyByKeyAPathBeforeThePathsUnderIt() {
        assertThat(WalkOrder.PATHS.compare("visit.headline", "visit-alt")).isNegative();
        assertThat("visit.headline".compareTo("visit-alt")).as("unlike the text as a whole").isPositive();
        assertThat(WalkOrder.PATHS.compare("a", "a.b")).isNegative();
        assertThat(WalkOrder.PATHS.compare("a.b", "a.b")).isZero();
        assertThat(WalkOrder.PATHS.compare("b", "a.z")).isPositive();
    }

    @Test
    void keysAreInCodeUnitOrderWhateverTheStoredOrder() throws Exception {
        assertThat(WalkOrder.keys(new ObjectMapper().readTree("{\"b\": 1, \"B\": 2, \"a\": 3, \"\\u00e9\": 4, \"_x\": 5}")))
            .containsExactly("B", "_x", "a", "b", "\u00e9");
    }

    /**
     * The normalizer meets values in the order {@link WalkOrder#PATHS} gives their paths: the rule it walks by and the
     * rule that compares places are one.
     */
    @Test
    void theNormalizerMeetsValuesInTheOrderThePathRuleGives() {
        NormalizedSubmission normalized = SubmissionNormalizer.normalize(new StoredSubmission(
            "01J2Q7Y0V6M3N4P5R6S7T8V9WX", StoredForms.FORM_ID, null, StoredForms.text(
                "{'visit-alt': 1, 'visit': {'headline': 2, 'Zone': 3, 'alpha': {'b': 4, 'a': 5}}, 'B': 6, '_x': 7}")),
            null);

        List<String> met = normalized.unmapped().stream().map(UnmappedValue::path).toList();
        List<String> ordered = new ArrayList<>(met);
        ordered.sort(WalkOrder.PATHS);
        assertThat(met).hasSize(7).isEqualTo(ordered)
            .startsWith("B", "_x", "visit.Zone", "visit.alpha.a", "visit.alpha.b", "visit.headline", "visit-alt");
    }
}
