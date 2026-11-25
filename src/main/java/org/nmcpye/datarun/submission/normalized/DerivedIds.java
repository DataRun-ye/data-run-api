package org.nmcpye.datarun.submission.normalized;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * The ids a normalized submission derives when nothing stored carries one. The recipe is published with the feed
 * contract, so a reader of the feed can compute every derived id from the stored ids it already holds.
 *
 * <p><b>The recipe.</b> A derived id is the text of a UUID version 5 (RFC 9562 §5.5: SHA-1 over the namespace's 16
 * bytes and the name's UTF-8 bytes), lower case and hyphenated, in the namespace {@link #NAMESPACE}: the version 5
 * UUID of the URL {@code https://datarun/feed/v1} ({@code 4b1aa021-0abd-532b-88b8-f71a9078093d}). Its name joins
 * stored id texts and paths with {@code |}:</p>
 * <ul>
 *   <li>a section: {@code <form id>|section|<section path>};</li>
 *   <li>a row without a stored {@code _id} (or with an empty one):
 *       {@code <submission id>|<section path>|<parent row id, or nothing>|<index>};</li>
 *   <li>a row whose id an earlier row of the submission already has: the same name, and when that id is taken too,
 *       the same name followed by {@code |<attempt>}, from 2 up, until the id is free.</li>
 * </ul>
 * <p>Every id in a name is the stored id text, exactly as stored, never parsed: a ULID, a form's 24-hex id, a 16-character
 * id of an older app, or any other. The parent row id is that row's own id: its stored {@code _id}, or its derived id.
 * A section path is the stored keys from the root to the section, joined by dots; the index is the row's 1-based
 * position in its array.</p>
 *
 * <p>Sections take derived ids because stored definitions carry no usable one: a stored section has no {@code id}, or
 * one that repeats its name. A renamed section therefore gets a new id.</p>
 */
public final class DerivedIds {

    private static final UUID URL_NAMESPACE = UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8");

    /** The namespace of every derived id: the version 5 UUID of the URL {@code https://datarun/feed/v1}. */
    public static final UUID NAMESPACE = uuidV5(URL_NAMESPACE, "https://datarun/feed/v1");

    private DerivedIds() {
    }

    /**
     * A section's id: over {@code <form id>|section|<section path>}.
     *
     * @param formId      the form's stored id text
     * @param sectionPath the section's data path: the stored keys from the root, joined by dots
     */
    public static String sectionId(String formId, String sectionPath) {
        return uuidV5(NAMESPACE, formId + "|section|" + sectionPath).toString();
    }

    /**
     * The derived id of a row without a stored {@code _id}: over
     * {@code <submission id>|<section path>|<parent row id, or nothing>|<index>}.
     *
     * @param submissionId the submission's stored id text
     * @param sectionPath  the repeating section's data path
     * @param parentRowId  the id of the row it is nested in, or {@code null} for a row of a section outside every row
     * @param index        the row's 1-based position in its array
     */
    public static String derivedRowId(String submissionId, String sectionPath, String parentRowId, int index) {
        return uuidV5(NAMESPACE, rowName(submissionId, sectionPath, parentRowId, index)).toString();
    }

    /**
     * The id of a row whose own id an earlier row of the submission already has ({@link NormalizationFlag#DUPLICATE_ID}).
     * The first attempt is the row's derived id ({@link #derivedRowId}); the next ones append {@code |<attempt>} to
     * the same name, from 2 up.
     *
     * @param attempt 1 for the derived id, then 2, 3 and so on
     */
    public static String duplicateRowId(String submissionId, String sectionPath, String parentRowId, int index,
                                        int attempt) {
        String name = rowName(submissionId, sectionPath, parentRowId, index);
        return uuidV5(NAMESPACE, attempt <= 1 ? name : name + "|" + attempt).toString();
    }

    private static String rowName(String submissionId, String sectionPath, String parentRowId, int index) {
        return submissionId + "|" + sectionPath + "|" + (parentRowId == null ? "" : parentRowId) + "|" + index;
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
