package org.nmcpye.datarun.jpa.datasubmission.conflict;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A phone copy: the whole document a phone sends again for a submission the server already holds, as received,
 * before any id is minted.
 *
 * @param route       the route it came by
 * @param uploadItem  the upload item as its route read it, in its route's shape: what a conflict keeps. That is every
 *                    field the route knows, its decimals as the request's binary doubles; a field the route ignores,
 *                    such as a phone's sync bookkeeping, is not kept
 * @param canonical   what the comparison reads of it against a stored state: the document as received, its deleted
 *                    flag, the id of the assignment the upload resolved, and its status
 * @param rowsLackIds any of its rows lacks an id, found by the upload before any id is minted (apps 5.2 and 5.3)
 */
public record PhoneCopy(UploadRoute route, JsonNode uploadItem, ComparedState canonical, boolean rowsLackIds) {
}
