package org.nmcpye.datarun.submission.normalized;

import java.util.Objects;

/**
 * A section of one form version, located by its data path, with the attributes the feed's dictionary publishes
 * ({@code feed.form_section}).
 *
 * @param path      the data path: the stored keys from the root, joined by dots (the definition's {@code path})
 * @param repeating whether the section repeats, its stored data then being an array of rows; {@code false} when the
 *                  definition does not say
 * @param id        the section's id, derived from the form and the path ({@link DerivedIds#sectionId})
 * @param name      its name in this version, as stored; {@code null} when it has none
 * @param label     its labels by locale, as stored, in JSON text; {@code null} when it has none
 * @param order     its place among its siblings, as stored; {@code null} when the definition gives none
 */
public record SectionDefinition(String path, boolean repeating, String id, String name, String label, Integer order) {

    public SectionDefinition {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(id, "id");
    }
}
