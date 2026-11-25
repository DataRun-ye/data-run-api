package org.nmcpye.datarun.submission.normalized;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.nmcpye.datarun.submission.normalized.StoredForms.version;

/**
 * Numbers survive from stored text to published text: read exactly, written with every digit. The stored texts are
 * written the way PostgreSQL prints {@code jsonb} (plain notation, {@code ", "} and {@code ": "}); invented values.
 */
class StoredJsonTest {

    private static final String DIGITS_401 = "1" + "0".repeat(400);

    @Test
    void aStoredNumberIsReadExactly() throws Exception {
        assertThat(StoredJson.parse("12345678901234567890.123").decimalValue())
            .isEqualByComparingTo("12345678901234567890.123");
        assertThat(StoredJson.parse("2.50").decimalValue().scale()).isEqualTo(2);
        assertThat(StoredJson.parse("1e400").decimalValue()).isEqualByComparingTo("1E+400");
        assertThat(StoredJson.parse(DIGITS_401).bigIntegerValue()).hasToString(DIGITS_401);
        assertThat(StoredJson.parse("-0.000001").decimalValue()).isEqualByComparingTo("-0.000001");
        // what a default mapper, such as Spring's or Hibernate's, makes of the same texts
        ObjectMapper lossy = new ObjectMapper();
        assertThat(lossy.readTree("12345678901234567890.123").asText()).isEqualTo("1.2345678901234567E19");
        assertThat(lossy.readTree("2.50").asText()).isEqualTo("2.5");
    }

    @Test
    void aStoredDocumentWrittenAgainKeepsEveryNumbersText() throws Exception {
        String stored = "{\"a\": 12345678901234567890.123, \"b\": [2.50, -0.000001, 0, -7], \"cc\": " + DIGITS_401
            + ", \"ddd\": {\"e\": 0.10, \"f\": \"text, with: punctuation\"}}";

        String published = StoredJson.write(StoredJson.parse(stored));

        assertThat(canonical(published)).isEqualTo(canonical(stored));
        assertThat(numbers(published)).containsExactly("12345678901234567890.123", "2.50", "-0.000001", "0", "-7",
            DIGITS_401, "0.10");
    }

    @Test
    void aNormalizedValueWrittenForTheFeedKeepsTheStoredNumbersText() {
        String form = "01HZY8X3K2ABCDEFGHJKMNPQRS";
        FormVersionDefinition v1 = version("01JV0000000000000000000001", "fvUid000001", 1)
            .field("deUidAmount1", null, "amount", "Number")
            .field("deUidBig0001", null, "big", "Integer")
            .field("deUidScale01", null, "scaled", "Number")
            .build(form);
        String stored = "{\"big\": " + DIGITS_401 + ", \"amount\": 12345678901234567890.123, \"scaled\": 2.50}";

        NormalizedSubmission normalized = SubmissionNormalizer.normalize(
            new StoredSubmission("01J2Q7Y0V6M3N4P5R6S7T8V9WX", "fvUid000001", stored), FormDefinition.of(form,
                List.of(v1)));

        assertThat(normalized.values()).extracting(v -> StoredJson.write(v.value()))
            .containsExactly("12345678901234567890.123", DIGITS_401, "2.50");
        assertThat(normalized.values()).extracting(FieldValue::issue).containsOnlyNulls();
    }

    @Test
    void textThatIsNotExactlyOneJsonValueDoesNotParse() {
        for (String notJson : List.of("{\"a\": ", "{} {}", "{\"n\": NaN}", "[Infinity]", "", "  ")) {
            assertThatThrownBy(() -> StoredJson.parse(notJson)).as(notJson)
                .isInstanceOf(JsonProcessingException.class);
        }
        assertThatThrownBy(() -> StoredJson.parse("[".repeat(1_001) + "]".repeat(1_001)))
            .isInstanceOf(JsonProcessingException.class);
    }

    @Test
    void aNumberJacksonCannotRepresentIsAParseFailureThatDoesNotQuoteTheText() {
        for (String number : OUT_OF_RANGE) {
            for (String text : List.of(number, "{\"amount\": " + number + "}")) {
                assertThatThrownBy(() -> StoredJson.parse(text)).as(text)
                    .isInstanceOf(JsonProcessingException.class)
                    .hasNoCause()
                    .satisfies(e -> assertThat(e.getMessage()).doesNotContain("99999").doesNotContain("2147483648"));
            }
        }
    }

    @Test
    void aNumberIsReadOnlyWhenItsPlainFormFitsTheLimit_whateverItsSpelling() throws Exception {
        // 10,001 characters in plain form: refused spelled with an exponent, as spelled in full
        for (String tooLong : List.of("1e10000", "1e-10000", "{\"a\": 1e10000}", "1" + "0".repeat(10_000))) {
            assertThatThrownBy(() -> StoredJson.parse(tooLong)).as(tooLong.length() > 20 ? "10,001 digits" : tooLong)
                .isInstanceOf(JsonProcessingException.class);
        }
        // just under the limit, 1,000 characters in plain form: read, and written again in full
        for (String justUnder : List.of("1e999", "-1e999", "1e-998")) {
            JsonNode read = StoredJson.parse(justUnder);
            String written = StoredJson.write(read);
            assertThat(written.replace("-", "")).as(justUnder).hasSize(1_000);
            // read again, the plain text writes the same text and holds the same value
            assertThat(StoredJson.write(StoredJson.parse(written))).as(justUnder).isEqualTo(written);
            assertThat(StoredJson.parse(written).decimalValue()).as(justUnder).isEqualByComparingTo(read.decimalValue());
        }
        assertThat(StoredJson.write(StoredJson.parse("1e999"))).isEqualTo("1" + "0".repeat(999));
        assertThat(StoredJson.plainLength(new java.math.BigDecimal("12.34"))).isEqualTo(5);
        assertThat(StoredJson.plainLength(new java.math.BigDecimal("0.0012"))).isEqualTo(6);
        assertThat(StoredJson.plainLength(new java.math.BigDecimal("123e2"))).isEqualTo(5);
    }

    /** Exponents Jackson reports as NumberFormatException, not as a JSON failure. */
    static final List<String> OUT_OF_RANGE = List.of("1e9999999999", "1e-9999999999", "1E+2147483648", "0e99999999999");

    /** The text without the whitespace outside strings: JSON's canonical spacing. */
    private static String canonical(String json) throws Exception {
        StringBuilder out = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                out.append(c);
                if (c == '\\') {
                    out.append(json.charAt(++i));
                } else if (c == '"') {
                    inString = false;
                }
            } else if (c == '"') {
                inString = true;
                out.append(c);
            } else if (!Character.isWhitespace(c)) {
                out.append(c);
            }
        }
        return out.toString();
    }

    /** The number tokens of a JSON text, in order. */
    private static List<String> numbers(String json) throws Exception {
        List<String> found = new ArrayList<>();
        collect(StoredJson.parse(json), found);
        return found;
    }

    private static void collect(JsonNode node, List<String> found) {
        if (node.isNumber()) {
            found.add(StoredJson.write(node));
        }
        node.forEach(child -> collect(child, found));
    }
}
