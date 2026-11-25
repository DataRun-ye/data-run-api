package org.nmcpye.datarun.submission.normalized;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Every version of one form, and for each version the resolution of every data path any version defines.
 *
 * <p><b>Resolution.</b> A stored value at a data path, in a submission pinned to version V,
 * resolves to one definition at that path, chosen among all the form's versions in this order:</p>
 * <ol>
 *   <li>a definition that can hold the value's shape (a field holds anything; a plain section holds an object; a
 *       repeating section holds rows; an empty array fits every definition) before one that cannot;</li>
 *   <li>V's own definition before another version's;</li>
 *   <li>a newer version before an older one;</li>
 *   <li>a field before a section.</li>
 * </ol>
 * <p>A path no version defines has no resolution. Example: a section plain in versions 1-9 and repeating in 14-15
 * holds an object in submissions pinned to 13 and 15; those objects resolve through version 9.</p>
 *
 * <p>A submission whose pinned version is not among the form's stored versions (a deleted version) resolves by the
 * same order without step 2: through the form's other versions, newest first.</p>
 *
 * <p><b>Ambiguous paths.</b> A data path that names different fields in different versions (a path reused for another
 * data element), or a field in one version and a section in another, or that one version defines twice, still
 * resolves by the order above (twice in one version: the first in stored order). Such paths are listed in
 * {@link #ambiguousPaths()}, and every stored value or container at one is flagged
 * {@link NormalizationFlag#AMBIGUOUS_PATH}. A section that is plain in one version and repeating in another is not
 * ambiguous: the stored shape decides.</p>
 *
 * <p>Immutable and safe to share between threads; built once per form and reused.</p>
 */
public final class FormDefinition {

    private final String id;
    private final List<FormVersionDefinition> versions;
    private final Map<String, FormVersionDefinition> versionsByUid;
    private final Map<String, Map<String, Resolution[]>> resolutionsByVersionUid;
    private final Map<String, Resolution[]> resolutionsWithoutPin;
    private final Set<String> ambiguousPaths;
    private final Map<String, Map<String, List<String>>> dataPathsByField;

    /** The definition used when no definition of a form is stored: no versions, so no path resolves. */
    static final FormDefinition NO_VERSIONS = new FormDefinition("", List.of());

    private FormDefinition(String id, List<FormVersionDefinition> versions) {
        this.id = Objects.requireNonNull(id, "id");
        List<FormVersionDefinition> newestFirst = new ArrayList<>(versions);
        newestFirst.sort(Comparator.comparingInt(FormVersionDefinition::number).reversed());
        this.versions = List.copyOf(newestFirst);
        Map<String, FormVersionDefinition> byUid = new HashMap<>();
        Set<Integer> numbers = new HashSet<>();
        for (FormVersionDefinition version : this.versions) {
            if (byUid.put(version.uid(), version) != null || !numbers.add(version.number())) {
                throw new IllegalArgumentException("form " + id + " has two versions with uid " + version.uid()
                    + " or number " + version.number());
            }
        }
        this.versionsByUid = Map.copyOf(byUid);
        Map<String, List<Candidate>> candidates = candidatesByPath(this.versions);
        Map<String, Map<String, Resolution[]>> byVersion = new HashMap<>();
        for (FormVersionDefinition pinned : this.versions) {
            byVersion.put(pinned.uid(), resolveAll(candidates, pinned));
        }
        this.resolutionsByVersionUid = Map.copyOf(byVersion);
        this.resolutionsWithoutPin = resolveAll(candidates, null);
        this.ambiguousPaths = ambiguous(candidates);
        this.dataPathsByField = dataPathsByField(this.versions);
    }

    /**
     * A form and its versions.
     *
     * @param id       the form's stored id
     * @param versions every version of the form, in any order
     */
    public static FormDefinition of(String id, List<FormVersionDefinition> versions) {
        return new FormDefinition(id, versions);
    }

    /** The form's stored id. */
    public String id() {
        return id;
    }

    /** The form's versions, newest first. */
    public List<FormVersionDefinition> versions() {
        return versions;
    }

    /**
     * The data paths that name different fields, or a field and a section, across the form's versions, or that one
     * version defines twice; values at them resolve as usual and are flagged {@link NormalizationFlag#AMBIGUOUS_PATH}.
     */
    public Set<String> ambiguousPaths() {
        return ambiguousPaths;
    }

    boolean isAmbiguous(String path) {
        return ambiguousPaths.contains(path);
    }

    /**
     * Where each version of the form stores a field's values: the field's data paths, by version id, newest version
     * first, and within a version in stored order (a version that places the field in two sections gives two paths).
     * A version that does not place the field has no entry; a field no version places gives an empty map. The reverse
     * of resolution, for readers that select or write by field id: search predicates, an edit's stored place.
     *
     * @param fieldId the field id: the stored id of a data element
     */
    public Map<String, List<String>> dataPaths(String fieldId) {
        return fieldId == null ? Map.of() : dataPathsByField.getOrDefault(fieldId, Map.of());
    }

    /** The version a stored submission names by its uid. */
    public Optional<FormVersionDefinition> versionByUid(String uid) {
        return Optional.ofNullable(uid == null ? null : versionsByUid.get(uid));
    }

    /**
     * The definition that resolves a stored value at a data path, in a submission pinned to a version of this form.
     *
     * @param pinned the pinned version, or {@code null} when it is not among the form's versions
     * @return {@code null} when no version of the form defines the path
     */
    Resolution resolve(FormVersionDefinition pinned, String path, StoredShape shape) {
        Map<String, Resolution[]> resolutions = pinned == null ? resolutionsWithoutPin
            : resolutionsByVersionUid.getOrDefault(pinned.uid(), resolutionsWithoutPin);
        Resolution[] byShape = resolutions.get(path);
        return byShape == null ? null : byShape[shape.ordinal()];
    }

    /** Every definition of each path, newest version first and, within a version, a field before a section. */
    private static Map<String, List<Candidate>> candidatesByPath(List<FormVersionDefinition> newestFirst) {
        Map<String, List<Candidate>> candidatesByPath = new HashMap<>();
        for (FormVersionDefinition version : newestFirst) {
            for (FieldDefinition field : version.fields()) {
                candidatesByPath.computeIfAbsent(field.path(), p -> new ArrayList<>())
                    .add(new Candidate(version, field, null));
            }
        }
        for (FormVersionDefinition version : newestFirst) {
            for (SectionDefinition section : version.sections()) {
                candidatesByPath.computeIfAbsent(section.path(), p -> new ArrayList<>())
                    .add(new Candidate(version, null, section));
            }
        }
        Comparator<Candidate> order = Comparator
            .comparingInt((Candidate c) -> c.version().number()).reversed()
            .thenComparing(c -> c.field() == null);
        candidatesByPath.values().forEach(list -> list.sort(order));
        return candidatesByPath;
    }

    /** Each field id's data paths, by version id, newest version first; fields without an id are never placed. */
    private static Map<String, Map<String, List<String>>> dataPathsByField(List<FormVersionDefinition> newestFirst) {
        Map<String, Map<String, List<String>>> building = new HashMap<>();
        for (FormVersionDefinition version : newestFirst) {
            for (FieldDefinition field : version.fields()) {
                if (field.id() != null) {
                    building.computeIfAbsent(field.id(), id -> new LinkedHashMap<>())
                        .computeIfAbsent(version.id(), id -> new ArrayList<>())
                        .add(field.path());
                }
            }
        }
        Map<String, Map<String, List<String>>> frozen = new HashMap<>();
        building.forEach((fieldId, byVersion) -> {
            Map<String, List<String>> versions = new LinkedHashMap<>();
            byVersion.forEach((versionId, paths) -> versions.put(versionId, List.copyOf(paths)));
            frozen.put(fieldId, Collections.unmodifiableMap(versions));
        });
        return Map.copyOf(frozen);
    }

    private static Set<String> ambiguous(Map<String, List<Candidate>> candidatesByPath) {
        Set<String> ambiguous = new HashSet<>();
        for (Map.Entry<String, List<Candidate>> entry : candidatesByPath.entrySet()) {
            Set<String> identities = new HashSet<>();
            Set<FormVersionDefinition> versions = new HashSet<>();
            boolean twiceInAVersion = false;
            for (Candidate candidate : entry.getValue()) {
                identities.add(candidate.field() == null ? "section" : "field " + candidate.field().id());
                twiceInAVersion |= !versions.add(candidate.version());
            }
            if (identities.size() > 1 || twiceInAVersion) {
                ambiguous.add(entry.getKey());
            }
        }
        return Set.copyOf(ambiguous);
    }

    /** Each path's resolution for each shape, with this version pinned ({@code null}: none). */
    private static Map<String, Resolution[]> resolveAll(Map<String, List<Candidate>> candidatesByPath,
                                                        FormVersionDefinition pinned) {
        StoredShape[] shapes = StoredShape.values();
        Map<String, Resolution[]> byPath = new HashMap<>(candidatesByPath.size() * 2);
        for (Map.Entry<String, List<Candidate>> entry : candidatesByPath.entrySet()) {
            Resolution[] byShape = new Resolution[shapes.length];
            for (StoredShape shape : shapes) {
                byShape[shape.ordinal()] = choose(entry.getValue(), pinned, shape);
            }
            byPath.put(entry.getKey(), byShape);
        }
        return Map.copyOf(byPath);
    }

    private static Resolution choose(List<Candidate> newestFirst, FormVersionDefinition pinned, StoredShape shape) {
        Candidate best = null;
        int bestRank = -1;
        for (Candidate candidate : newestFirst) {
            boolean holds = shape.heldBy(candidate.field() != null,
                candidate.section() != null && candidate.section().repeating());
            int rank = (holds ? 2 : 0) + (candidate.version() == pinned ? 1 : 0);
            // the list is newest first, so the first candidate of the highest rank is the newest of that rank
            if (rank > bestRank) {
                best = candidate;
                bestRank = rank;
            }
        }
        return best == null ? null
            : new Resolution(best.field(), best.section(), best.version(), best.version() == pinned);
    }

    private record Candidate(FormVersionDefinition version, FieldDefinition field, SectionDefinition section) {
    }

    /**
     * The definition chosen for a path and a shape: a field or a section, and the version it came from.
     */
    record Resolution(
        FieldDefinition field,
        SectionDefinition section,
        FormVersionDefinition version,
        boolean fromPinnedVersion) {

        boolean isField() {
            return field != null;
        }

        boolean isRepeatingSection() {
            return section != null && section.repeating();
        }

        boolean isPlainSection() {
            return section != null && !section.repeating();
        }
    }
}
