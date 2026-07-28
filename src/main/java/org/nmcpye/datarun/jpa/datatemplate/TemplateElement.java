package org.nmcpye.datarun.jpa.datatemplate;

import lombok.*;

import java.util.Map;

/// In-memory projection input built from a published template version.
///
/// This is not a persisted entity or a runtime form authority. It exists only
/// while deriving the canonical metadata consumed by ETL and exports.
///
/// @author Hamza
/// @since 18/08/2025
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class TemplateElement {
    /// uid of the [DataTemplate] containing this configuration.
    @ToString.Include
    private String templateUid;

    /// Structural field or repeat name.
    private String name;

    /// Option set uid
    /// [#SelectMulti] field.
    private String optionSetUid;

    private String optionSetId;

    /// Path built with element names (ends with name). Used during normalization.
    ///
    /// Used in normalization.
    /// For [FormSectionConf], the id is its name.
    /// For [FormDataElementConf], the id is linked to [DataElement] uid.
    private String jsonDataPath;

    /// The Canonical path. that canonically represent a grain of data
    /// name of element if submission-level
    /// the full `repeat_path` (e.g., `root.householdinfo.children`) mixes two different concepts:
    /// 1.  **Structural Grouping:** The `householdinfo` part is just a visual grouping on the form. An admin could rename it to `household_details` tomorrow, and it would mean the exact same thing to the user.
    /// 2.  **Data Grain:** The `children` part, because it is `repeatable: true`, defines a fundamental change in the data's structure. It means "one or more children related to one parent submission." This is the true canonical grain.
    private String canonicalPath;

    /// full path to nearest repeatable ancestor (or null), dot delimited. no array [*]
    private String parentRepeatJsonDataPath;

    /// Localized display labels, e.g. `{"en": "Child Name", "ar": "..."}`.
    private Map<String, String> displayLabel;

    //-----------------------
    // Canonical and metadata
    //-----------------------
    private DataType dataType;

    private SemanticType semanticType;

}
