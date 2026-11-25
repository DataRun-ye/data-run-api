package org.nmcpye.datarun.submission.normalized;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Stored documents as JSON text, read and written with every number exact.
 *
 * <p>The mappers Spring and Hibernate configure read decimals as binary doubles: a stored
 * {@code 12345678901234567890.123} becomes {@code 1.2345678901234567E19}, and {@code 2.50} becomes {@code 2.5}. This
 * class reads a decimal as a {@link java.math.BigDecimal} with its scale, an integer as an int, a long or a
 * {@link java.math.BigInteger} as its size needs, and never converts either to a binary float; it writes numbers in
 * plain notation with every digit, as PostgreSQL prints a {@code jsonb} number. So a number read from stored text and
 * written again keeps its text.</p>
 *
 * <p>Text that is not exactly one JSON value fails to parse: a syntax error, trailing content, a token JSON does not
 * have ({@code NaN}, {@code Infinity}), or nesting deeper than Jackson's limit of 1,000.</p>
 */
public final class StoredJson {

    private static final ObjectMapper EXACT = JsonMapper.builder()
        .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        .configure(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES, false)
        .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
        .build();

    private StoredJson() {
    }

    /**
     * Reads stored JSON text, such as {@code form_data::text}, with exact numbers.
     *
     * @throws JsonProcessingException when the text is not exactly one JSON value
     */
    public static JsonNode parse(String jsonText) throws JsonProcessingException {
        JsonNode node = EXACT.readTree(jsonText);
        if (node == null || node.isMissingNode()) {
            throw new JsonProcessingException("no JSON value in the text") {
            };
        }
        return node;
    }

    /** A value as JSON text, every number in plain notation with all its digits. */
    public static String write(JsonNode value) {
        try {
            return EXACT.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            // a tree parsed by this class always writes; only a node built in code could refuse
            throw new IllegalArgumentException("a JSON tree that cannot be written", e);
        }
    }
}
