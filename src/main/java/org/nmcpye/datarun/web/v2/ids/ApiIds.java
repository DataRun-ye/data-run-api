package org.nmcpye.datarun.web.v2.ids;

import java.util.regex.Pattern;

/**
 * The v2 id rule. Every v2 id is an opaque string: the server's canonical id text for the entity, exactly as
 * stored. Today that is a 26-character ULID for most entities, a 24-hex legacy id for some forms, a one-character
 * id for org-unit levels, and UUID text for ids created later; a later migration of the database to the uuid type
 * changes nothing for clients, because presentation is the server's choice.
 * <ul>
 *   <li>Clients send an id back exactly as received. They do not parse it or assume a format or a length beyond
 *       {@link #MAX_LENGTH}. The OpenAPI document says only that: {@code type: string}, {@code maxLength: 36}, no
 *       {@code format}.</li>
 *   <li>It is never the 11-character {@code uid}, which stays a v1 alias for installed phones.</li>
 *   <li>The server compares ids exactly; it does not change their case.</li>
 * </ul>
 * {@link #isWellFormed} is the server's own guard, not part of the contract: a value that no stored id can take
 * (too long, or with a character outside letters, digits and {@code -}) names nothing, so a route answers it as it
 * answers an unknown id, and it never reaches a query or a log line.
 */
public final class ApiIds {

    /** The documented maximum length of an id. */
    public static final int MAX_LENGTH = 36;

    /** The name of the OpenAPI schema every v2 id refers to. */
    public static final String SCHEMA_NAME = "Id";

    /** How the OpenAPI document describes an id. */
    public static final String DESCRIPTION = "An opaque identifier, at most 36 characters. Send it back exactly as "
        + "received; do not parse it or assume a format.";

    private static final Pattern STORED_ID = Pattern.compile("[0-9A-Za-z-]{1," + MAX_LENGTH + "}");

    private ApiIds() {
    }

    /** Whether the value could be a stored id at all. */
    public static boolean isWellFormed(String value) {
        return value != null && STORED_ID.matcher(value).matches();
    }
}
