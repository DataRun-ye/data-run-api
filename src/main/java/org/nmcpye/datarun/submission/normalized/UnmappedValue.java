package org.nmcpye.datarun.submission.normalized;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/**
 * A stored value that no field of the form can key: kept whole, never dropped.
 *
 * @param path   the data path the value is stored at, from the submission's root
 * @param value  the stored value, whole, a node of the result's own parsed tree; never JSON null
 * @param reason why no field keys it
 */
public record UnmappedValue(String path, JsonNode value, UnmappedReason reason) {

    public UnmappedValue {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(reason, "reason");
    }
}
