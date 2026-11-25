package org.nmcpye.datarun.web.v2.idempotency;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * One form for JSON documents that mean the same, so equal requests hash equal: object members sorted by name
 * (UTF-16 code units, as RFC 8785 sorts), no whitespace, numbers by exact value ({@code 2}, {@code 2.0},
 * {@code 2e0} and {@code 20e-1} are one number; decimals are never rounded through a double), strings escaped
 * one way. Array order is kept. A member named twice counts once, with its last value, as the request's parser
 * reads it.
 */
public final class CanonicalJson {

    private static final ObjectMapper EXACT = JsonMapper.builder()
        .nodeFactory(JsonNodeFactory.withExactBigDecimals(true))
        .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
        .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
        .build();

    private CanonicalJson() {
    }

    /**
     * @throws IOException when the bytes are not one JSON document
     */
    public static byte[] canonicalize(byte[] json) throws IOException {
        JsonNode root = EXACT.readTree(json);
        if (root == null || root.isMissingNode()) {
            throw new IOException("No JSON document");
        }
        StringBuilder out = new StringBuilder(json.length);
        write(root, out);
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void write(JsonNode node, StringBuilder out) {
        if (node.isObject()) {
            List<String> names = new ArrayList<>();
            node.fieldNames().forEachRemaining(names::add);
            names.sort(null);
            out.append('{');
            for (int i = 0; i < names.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                string(names.get(i), out);
                out.append(':');
                write(node.get(names.get(i)), out);
            }
            out.append('}');
        } else if (node.isArray()) {
            out.append('[');
            Iterator<JsonNode> elements = node.elements();
            boolean first = true;
            while (elements.hasNext()) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                write(elements.next(), out);
            }
            out.append(']');
        } else if (node.isNumber()) {
            out.append(number(node));
        } else if (node.isTextual()) {
            string(node.textValue(), out);
        } else if (node.isBoolean()) {
            out.append(node.booleanValue());
        } else if (node.isNull()) {
            out.append("null");
        } else {
            throw new IllegalArgumentException("Not a JSON value: " + node.getNodeType());
        }
    }

    /** The exact value with no trailing zeros: one text per number ({@code 1E+3} for 1000, {@code 0} for -0.0). */
    static String number(JsonNode node) {
        BigDecimal value = node.isIntegralNumber() ? new BigDecimal(node.bigIntegerValue()) : node.decimalValue();
        return value.signum() == 0 ? "0" : value.stripTrailingZeros().toString();
    }

    private static void string(String value, StringBuilder out) {
        out.append('"').append(JsonStringEncoder.getInstance().quoteAsString(value)).append('"');
    }
}
