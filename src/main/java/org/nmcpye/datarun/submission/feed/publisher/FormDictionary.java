package org.nmcpye.datarun.submission.feed.publisher;

import org.nmcpye.datarun.submission.normalized.FieldDefinition;
import org.nmcpye.datarun.submission.normalized.FormDefinition;
import org.nmcpye.datarun.submission.normalized.FormVersionDefinition;
import org.nmcpye.datarun.submission.normalized.SectionDefinition;

import java.util.ArrayList;
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
 *   <li>Attributes are as the definition stores them: a value type the server does not know is published as
 *       stored.</li>
 *   <li>The placements published are those that hold the version's values, by the normalizer's own rule
 *       ({@link FormDefinition#placements}): at a data path a version names twice, only the definition resolution
 *       picks, as for {@code AMBIGUOUS_PATH}; of one field placed twice in one section, or twice outside every
 *       section, the placement first in walk order. A section path defined twice keeps its first definition in stored
 *       order. What is left out is counted, with its versions.</li>
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

    /** Definitions a version gives that the dictionary leaves out, and the versions that give them, sorted. */
    record LeftOut(int count, List<String> versions) {

        LeftOut {
            versions = List.copyOf(versions);
        }
    }

    private final List<FieldRecord> fields;
    private final List<SectionRecord> sections;
    private final LeftOut pathsTakenByAnother;
    private final LeftOut givenTwice;

    private FormDictionary(List<FieldRecord> fields, List<SectionRecord> sections, LeftOut pathsTakenByAnother,
                           LeftOut givenTwice) {
        this.fields = List.copyOf(fields);
        this.sections = List.copyOf(sections);
        this.pathsTakenByAnother = pathsTakenByAnother;
        this.givenTwice = givenTwice;
    }

    /** The dictionary of these forms. */
    static FormDictionary of(Collection<FormDefinition> forms) {
        List<FormDefinition> byId = new ArrayList<>(forms);
        byId.sort(Comparator.comparing(FormDefinition::id));
        List<FieldRecord> fields = new ArrayList<>();
        List<SectionRecord> sections = new ArrayList<>();
        int taken = 0;
        int twice = 0;
        Set<String> withTakenPaths = new TreeSet<>();
        Set<String> withTwice = new TreeSet<>();
        for (FormDefinition form : byId) {
            List<FormVersionDefinition> oldestFirst = new ArrayList<>(form.versions());
            oldestFirst.sort(Comparator.comparingInt(FormVersionDefinition::number));
            for (FormVersionDefinition version : oldestFirst) {
                Map<String, SectionDefinition> defined = new HashMap<>();
                for (SectionDefinition section : version.sections()) {
                    if (defined.putIfAbsent(section.path(), section) != null) {
                        twice++;
                        withTwice.add(version.id());
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
                FormDefinition.Placements placements = form.placements(version);
                for (FieldDefinition field : placements.published()) {
                    SectionDefinition rows = field.sectionId() == null ? null
                        : innermost(defined, sectionPath(field), true);
                    fields.add(new FieldRecord(form.id(), version.id(), version.number(), field.id(),
                        field.sectionId(), rows == null ? null : rows.id(), field.name(), field.label(),
                        field.type(), field.optionSetId(), field.mandatory(), field.order()));
                }
                long takenHere = placements.pathTakenByAnother().stream().filter(field -> field.id() != null).count();
                if (takenHere > 0) {
                    taken += (int) takenHere;
                    withTakenPaths.add(version.id());
                }
                if (!placements.placedTwice().isEmpty()) {
                    twice += placements.placedTwice().size();
                    withTwice.add(version.id());
                }
            }
        }
        return new FormDictionary(fields, sections, new LeftOut(taken, new ArrayList<>(withTakenPaths)),
            new LeftOut(twice, new ArrayList<>(withTwice)));
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

    /**
     * Field placements left out because their data path, named twice by their version, resolves to another
     * definition.
     */
    LeftOut pathsTakenByAnother() {
        return pathsTakenByAnother;
    }

    /** Section definitions, and placements of one field in one section, that a version gives twice. */
    LeftOut givenTwice() {
        return givenTwice;
    }

    /** How many form versions the dictionary holds records of. */
    long versionCount() {
        Set<String> versions = new HashSet<>();
        sections.forEach(section -> versions.add(section.formVersionId()));
        fields.forEach(field -> versions.add(field.formVersionId()));
        return versions.size();
    }
}
