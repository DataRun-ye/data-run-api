package org.nmcpye.datarun.submission.normalized;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

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
 * <p><b>One rule for numbers, whatever their spelling.</b> A number is read only when its plain form (no sign, no
 * exponent) fits Jackson's limit of 1,000 characters for a number, which already refuses longer digit strings.
 * PostgreSQL always prints a {@code jsonb} number in plain form, so {@code 1e10000} fails exactly as its
 * 10,001-digit plain spelling does. Every number read therefore writes again in plain form.</p>
 *
 * <p><b>A key given twice in one object keeps its last value</b>, as {@code jsonb} keeps it on input: any text is read
 * as PostgreSQL would store it. {@code {"s": {"n": 1}, "s": {"i": 2}}} reads as {@code {"s": {"i": 2}}}.</p>
 *
 * <p>Text that is not exactly one JSON value fails to parse: a syntax error, trailing content, a token JSON does not
 * have ({@code NaN}, {@code Infinity}), a number Jackson cannot represent (an exponent out of range), or nesting
 * deeper than Jackson's limit of 1,000. Every such failure is a {@link JsonProcessingException} with a fixed message
 * naming only the kind of failure: Jackson's own messages can quote the text.</p>
 */
public final class StoredJson {

    private static final ObjectMapper EXACT = JsonMapper.builder()
        .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        .configure(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES, false)
        .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
        .build();

    /** The longest number Jackson reads, in characters: the limit plain spellings already meet. */
    private static final int MAX_NUMBER_LENGTH = EXACT.getFactory().streamReadConstraints().getMaxNumberLength();

    private StoredJson() {
    }

    /**
     * Reads stored JSON text, such as {@code form_data::text}, with exact numbers.
     *
     * @param jsonText the text; {@code null} is a programming error
     * @throws JsonProcessingException when the text is not exactly one JSON value, whatever Jackson raised
     */
    public static JsonNode parse(String jsonText) throws JsonProcessingException {
        Objects.requireNonNull(jsonText, "jsonText");
        JsonNode node;
        try {
            node = EXACT.readTree(jsonText);
        } catch (JsonProcessingException | RuntimeException e) {
            // Jackson reports some numbers it cannot read (an exponent out of range) as a NumberFormatException
            throw unreadable(e.getClass().getSimpleName());
        }
        if (node == null || node.isMissingNode()) {
            throw unreadable("no JSON value");
        }
        checkNumbers(node);
        return node;
    }

    /** Refuses a number whose plain form is longer than the reader's limit; walks without recursion. */
    private static void checkNumbers(JsonNode root) throws JsonProcessingException {
        Deque<JsonNode> pending = new ArrayDeque<>();
        pending.push(root);
        while (!pending.isEmpty()) {
            JsonNode node = pending.pop();
            if (node.isBigDecimal() && plainLength(node.decimalValue()) > MAX_NUMBER_LENGTH) {
                throw unreadable("a number longer than " + MAX_NUMBER_LENGTH + " characters in plain form");
            }
            node.forEach(pending::push);
        }
    }

    /** The length of the number's plain form, without its sign. */
    static long plainLength(BigDecimal number) {
        long precision = number.precision();
        long scale = number.scale();
        if (scale <= 0) {
            return precision - scale;                 // the digits, then -scale zeros
        }
        return scale < precision ? precision + 1     // a point among the digits
            : scale + 2;                              // "0.", zeros, then the digits
    }

    /** A failure that names its kind, never the text or Jackson's message, and keeps no cause that might. */
    private static JsonProcessingException unreadable(String kind) {
        return new JsonProcessingException("stored text is not readable JSON: " + kind) {
        };
    }

    /**
     * A value as JSON text, every number in plain notation with all its digits. A tree read by {@link #parse} always
     * writes: its numbers fit 1,000 characters in plain form, well inside the plain writer's limit (an exponent of
     * 9,999), and its nesting fits the writer's limit as it fit the reader's.
     *
     * @throws IllegalArgumentException only for a node built in code that the writer refuses
     */
    public static String write(JsonNode value) {
        try {
            return EXACT.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("a JSON tree that cannot be written", e);
        }
    }
}
