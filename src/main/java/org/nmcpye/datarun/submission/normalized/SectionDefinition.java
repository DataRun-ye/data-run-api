package org.nmcpye.datarun.submission.normalized;

import java.util.Objects;

/**
 * A section of one form version, located by its data path.
 *
 * @param path      the data path: the stored keys from the root, joined by dots (the definition's {@code path})
 * @param repeating whether the section repeats, its stored data then being an array of rows
 * @param id        the section's id, derived from the form and the path ({@link DerivedIds#sectionId})
 */
public record SectionDefinition(String path, boolean repeating, String id) {

    public SectionDefinition {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(id, "id");
    }
}
