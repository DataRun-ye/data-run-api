package org.nmcpye.datarun.submission.feed.publisher;

import org.nmcpye.datarun.submission.normalized.FieldDefinition;
import org.nmcpye.datarun.submission.normalized.FormDefinition;
import org.nmcpye.datarun.submission.normalized.FormVersionDefinition;
import org.nmcpye.datarun.submission.normalized.SectionDefinition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The feed's dictionary as records of its two streams, built from the stored form definitions: one
 * {@code feed.form_section} record per section of every form version, and one {@code feed.form_field} record per field
 * placement (contract 6). Pure: it reads its arguments and nothing else.
 *
 * <ul>
 *   <li>Only fields the submission normalizer resolves are published: a field whose data element no longer exists
 *       has no record, nor a definition without a data element or a name, which the definitions already leave out.
 *       Their values are published as unmapped values.</li>
 *   <li>A field's repeating section is the innermost repeating section of its version that holds it: the rows that
 *       hold its values. A section's parent is the innermost section of its version that holds it.</li>
 *   <li>The keys hold whatever the definitions say: a version that defines one section path twice, or places one
 *       field twice in one section (or twice outside every section), keeps the first in stored order; the others are
 *       counted in {@link #skipped()}, with their versions.</li>
 * </ul>
 * <p>Records come in a fixed order: by form id, version number, then stored order.</p>
 */
final class FormDictionary {

    /** One record of {@code feed.form_field}, named as its columns. */
    record FieldRecord(
        String formId,
        String formVersionId,
        int formVersionNumber,
        String fieldId,
        String sectionId,
        String repeatSectionId,
        String name,
        String label,
        String valueType,
        String optionSetId,
        Boolean mandatory,
        Integer sortOrder) {
    }

    /** One record of {@code feed.form_section}, named as its columns. */
    record SectionRecord(
        String formId,
        String formVersionId,
        int formVersionNumber,
        String sectionId,
        String parentSectionId,
        String name,
        String label,
        boolean repeatable,
        Integer sortOrder) {
    }

    private final List<FieldRecord> fields;
    private final List<SectionRecord> sections;
    private final int skipped;
    private final List<String> versionsWithDuplicates;

    private FormDictionary(List<FieldRecord> fields, List<SectionRecord> sections, int skipped,
                           Collection<String> versionsWithDuplicates) {
        this.fields = List.copyOf(fields);
        this.sections = List.copyOf(sections);
        this.skipped = skipped;
        this.versionsWithDuplicates = List.copyOf(versionsWithDuplicates);
    }

    /** The dictionary of these forms. */
    static FormDictionary of(Collection<FormDefinition> forms) {
        List<FormDefinition> byId = new ArrayList<>(forms);
        byId.sort(Comparator.comparing(FormDefinition::id));
        List<FieldRecord> fields = new ArrayList<>();
        List<SectionRecord> sections = new ArrayList<>();
        int skipped = 0;
        Set<String> withDuplicates = new TreeSet<>();
        for (FormDefinition form : byId) {
            List<FormVersionDefinition> oldestFirst = new ArrayList<>(form.versions());
            oldestFirst.sort(Comparator.comparingInt(FormVersionDefinition::number));
            for (FormVersionDefinition version : oldestFirst) {
                int before = skipped;
                Map<String, SectionDefinition> defined = new HashMap<>();
                for (SectionDefinition section : version.sections()) {
                    if (defined.putIfAbsent(section.path(), section) != null) {
                        skipped++;
                    }
                }
                Set<String> published = new HashSet<>();
                for (SectionDefinition section : version.sections()) {
                    if (published.add(section.path())) {
                        SectionDefinition parent = innermost(defined, parentPath(section.path()), false);
                        sections.add(new SectionRecord(form.id(), version.id(), version.number(), section.id(),
                            parent == null ? null : parent.id(), section.name(), section.label(), section.repeating(),
                            section.order()));
                    }
                }
                Set<List<String>> placed = new HashSet<>();
                for (FieldDefinition field : version.fields()) {
                    if (field.id() == null) {
                        continue;
                    }
                    if (!placed.add(Arrays.asList(field.sectionId(), field.id()))) {
                        skipped++;
                        continue;
                    }
                    SectionDefinition rows = field.sectionId() == null ? null
                        : innermost(defined, sectionPath(field), true);
                    fields.add(new FieldRecord(form.id(), version.id(), version.number(), field.id(),
                        field.sectionId(), rows == null ? null : rows.id(), field.name(), field.label(),
                        field.valueType() == null ? null : field.valueType().name(), field.optionSetId(),
                        field.mandatory(), field.order()));
                }
                if (skipped > before) {
                    withDuplicates.add(version.id());
                }
            }
        }
        return new FormDictionary(fields, sections, skipped, withDuplicates);
    }

    /**
     * The innermost section at this path or above it, of those the version defines; only repeating ones when asked.
     *
     * @param path a data path, or {@code null} for none
     */
    private static SectionDefinition innermost(Map<String, SectionDefinition> byPath, String path, boolean repeating) {
        for (String at = path; at != null; at = parentPath(at)) {
            SectionDefinition section = byPath.get(at);
            if (section != null && (!repeating || section.repeating())) {
                return section;
            }
        }
        return null;
    }

    /** The path without its last key, or {@code null} for a path of one key. */
    private static String parentPath(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? null : path.substring(0, dot);
    }

    /** The data path of the section holding a field: its data path without its name. */
    private static String sectionPath(FieldDefinition field) {
        return field.path().substring(0, field.path().length() - field.name().length() - 1);
    }

    List<FieldRecord> fields() {
        return fields;
    }

    List<SectionRecord> sections() {
        return sections;
    }

    /** How many section definitions and field placements a version gave twice, and were left out. */
    int skipped() {
        return skipped;
    }

    /** The ids of the versions that gave something twice, sorted. */
    List<String> versionsWithDuplicates() {
        return versionsWithDuplicates;
    }

    /** How many form versions the dictionary holds records of. */
    long versionCount() {
        Set<String> versions = new HashSet<>();
        sections.forEach(section -> versions.add(section.formVersionId()));
        fields.forEach(field -> versions.add(field.formVersionId()));
        return versions.size();
    }
}
