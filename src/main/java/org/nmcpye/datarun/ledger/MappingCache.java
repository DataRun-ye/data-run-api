package org.nmcpye.datarun.ledger;

import org.nmcpye.datarun.ledger.model.Submission;

public interface MappingCache {
    /**
     * Resolve sku by canonical category uid or fallback to category text.
     */
    String resolveSku(String categoryUid, String categoryText);

    /**
     * Resolve and populate fromPartyId and toPartyId on Submission.
     */
    void resolvePartiesForSubmission(Submission s);
}
