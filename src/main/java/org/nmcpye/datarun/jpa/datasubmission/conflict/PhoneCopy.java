package org.nmcpye.datarun.jpa.datasubmission.conflict;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A phone copy: the whole document a phone sends again for a submission the server already holds, as received,
 * before any id is minted.
 *
 * @param route      the route it came by
 * @param uploadItem the upload item exactly as received, in its route's shape: what a conflict keeps
 * @param canonical  what the comparison reads of it against a stored state: the document as received, its deleted
 *                   flag, and the assignment and form version the upload resolved
 */
public record PhoneCopy(UploadRoute route, JsonNode uploadItem, ComparedState canonical) {

    /** What the comparison reads of it against another phone copy: the upload item's own parts. */
    ComparedState asReceived() {
        return ComparedState.ofUploadItem(uploadItem);
    }
}
