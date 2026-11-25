package org.nmcpye.datarun.submission.normalized;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * The ids a normalized submission derives, by the recipes the D feed spike published
 * ({@code .review/data-chain/2026-11-25-d-spike-phase1.md} §2 and §11; D design §4.3), so that a reader of the
 * feed can compute them too.
 *
 * <ul>
 *   <li>Every derived id is a UUID version 5 (RFC 9562 §5.5) in the namespace {@link #NAMESPACE}, itself the
 *       version 5 UUID of the URL {@code https://datarun/feed/v1}. Its text is the lower-case hyphenated form.</li>
 *   <li>A section's id: over {@code <form key>|section|<section path>}. Stored definitions carry no usable section
 *       id: 145 of 717 stored sections have none, and every other one repeats its name (measured on the staging
 *       clone, 2026-11-25).</li>
 *   <li>A row without a stored {@code _id}: over {@code <submission key>|<section path>|<parent row key or empty>|<index>},
 *       where the index is the row's 1-based position in its array.</li>
 *   <li>The keys inside a recipe are UUIDs: a ULID is read as the UUID with the same 128 bits; any other form id is
 *       the version 5 UUID over {@code data_template|<id>}; a row's key is its ULID, the version 5 UUID over
 *       {@code <submission key>|<stored _id>} for another stored id, or its derived id.</li>
 * </ul>
 *
 * <p>The spike reads every submission id as a ULID, which all 67,176 stored ones are. A submission id that is not
 * a ULID (none today) is read here as a UUID when it is one, and otherwise as the version 5 UUID over
 * {@code submission|<id>}; a derived row id is then never missing.</p>
 */
public final class DerivedIds {

    private static final UUID URL_NAMESPACE = UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8");

    /** The namespace of every derived id: the version 5 UUID of the URL {@code https://datarun/feed/v1}. */
    public static final UUID NAMESPACE = uuidV5(URL_NAMESPACE, "https://datarun/feed/v1");

    private static final String CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final int ULID_LENGTH = 26;

    private DerivedIds() {
    }

    /**
     * A section's id, derived from its form and its data path.
     *
     * @param formId      the form's stored id
     * @param sectionPath the section's data path: the stored keys from the root, joined by dots
     */
    public static String sectionId(String formId, String sectionPath) {
        return uuidV5(NAMESPACE, formKey(formId) + "|section|" + sectionPath).toString();
    }

    /** The key a recipe uses for a form: its ULID's 128 bits, or the version 5 UUID over {@code data_template|<id>}. */
    public static UUID formKey(String formId) {
        UUID ulid = ulidToUuid(formId);
        return ulid != null ? ulid : uuidV5(NAMESPACE, "data_template|" + formId);
    }

    /**
     * The key a recipe uses for a submission: its ULID's 128 bits (every stored submission id today), the UUID itself
     * when the id is one, or the version 5 UUID over {@code submission|<id>}.
     */
    public static UUID submissionKey(String submissionId) {
        UUID ulid = ulidToUuid(submissionId);
        if (ulid != null) {
            return ulid;
        }
        UUID uuid = parseUuid(submissionId);
        return uuid != null ? uuid : uuidV5(NAMESPACE, "submission|" + submissionId);
    }

    /** The key a recipe uses for a row whose stored {@code _id} is not a ULID. */
    static UUID legacyRowKey(UUID submissionKey, String storedId) {
        return uuidV5(NAMESPACE, submissionKey + "|" + storedId);
    }

    /**
     * The derived id of a row without a stored {@code _id}.
     *
     * @param submissionKey the submission's key ({@link #submissionKey(String)})
     * @param sectionPath   the repeating section's data path
     * @param parentRowKey  the enclosing row's key, or {@code null} for a row of a section outside every row
     * @param index         the row's 1-based position in its array
     */
    static UUID derivedRowKey(UUID submissionKey, String sectionPath, UUID parentRowKey, int index) {
        return uuidV5(NAMESPACE, submissionKey + "|" + sectionPath + "|"
            + (parentRowKey == null ? "" : parentRowKey.toString()) + "|" + index);
    }

    /**
     * The id of a row whose own id an earlier row of the submission already has ({@link RowKeyKind#DUPLICATE_ID}).
     * The first attempt is the row's position-derived id ({@link #derivedRowKey}); only when that is taken too (two
     * parents with one key, such as one ULID stored in two cases) the next attempts add {@code |<attempt>} to the
     * same recipe, from 2 up, until the id is free.
     *
     * @param attempt 1 for the position-derived id, then 2, 3 and so on
     */
    static UUID duplicateRowKey(UUID submissionKey, String sectionPath, UUID parentRowKey, int index, int attempt) {
        if (attempt <= 1) {
            return derivedRowKey(submissionKey, sectionPath, parentRowKey, index);
        }
        return uuidV5(NAMESPACE, submissionKey + "|" + sectionPath + "|"
            + (parentRowKey == null ? "" : parentRowKey.toString()) + "|" + index + "|" + attempt);
    }

    /** Whether the text is a ULID: 26 Crockford base-32 digits, either case, the first at most {@code 7}. */
    public static boolean isUlid(String text) {
        if (text == null || text.length() != ULID_LENGTH || text.charAt(0) > '7') {
            return false;
        }
        for (int i = 0; i < ULID_LENGTH; i++) {
            if (digit(text.charAt(i)) < 0) {
                return false;
            }
        }
        return true;
    }

    /** The UUID with the same 128 bits as the ULID, or {@code null} when the text is not a ULID. */
    public static UUID ulidToUuid(String text) {
        if (!isUlid(text)) {
            return null;
        }
        long high = 0;
        long low = 0;
        for (int i = 0; i < ULID_LENGTH; i++) {
            int d = digit(text.charAt(i));
            // shift the 128-bit (high, low) pair left by five bits and add the digit
            high = (high << 5) | (low >>> 59);
            low = (low << 5) | d;
        }
        return new UUID(high, low);
    }

    private static int digit(char c) {
        char upper = Character.toUpperCase(c);
        return upper < 128 ? CROCKFORD.indexOf(upper) : -1;
    }

    private static UUID parseUuid(String text) {
        if (text == null || text.length() != 36) {
            return null;
        }
        try {
            UUID uuid = UUID.fromString(text);
            return uuid.toString().equalsIgnoreCase(text) ? uuid : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** A version 5 (name-based, SHA-1) UUID of the name's UTF-8 bytes in the namespace. */
    static UUID uuidV5(UUID namespace, String name) {
        MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is required by every Java platform", e);
        }
        sha1.update(ByteBuffer.allocate(16)
            .putLong(namespace.getMostSignificantBits())
            .putLong(namespace.getLeastSignificantBits())
            .array());
        byte[] hash = sha1.digest(name.getBytes(StandardCharsets.UTF_8));
        hash[6] = (byte) ((hash[6] & 0x0f) | 0x50);
        hash[8] = (byte) ((hash[8] & 0x3f) | 0x80);
        ByteBuffer bytes = ByteBuffer.wrap(hash, 0, 16);
        return new UUID(bytes.getLong(), bytes.getLong());
    }
}
