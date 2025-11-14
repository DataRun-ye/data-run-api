package org.nmcpye.datarun.jpa.datatemplate;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * @author Hamza Assada
 * @since 25/09/2025
 */
@Entity
@Table(name = "canonical_element")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalElement {

    @Id
    @Column(name = "canonical_element_uid", updatable = false, nullable = false, unique = true)
    private String canonicalElementUid;

    @Column(name = "template_uid", updatable = false, nullable = false, unique = true)
    private String templateUid;

    @Column(name = "preferred_name", updatable = false, nullable = false)
    private String preferredName;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", length = 64)
    private DataType dataType;

    @Enumerated(EnumType.STRING)
    @Column(name = "semantic_type", length = 64)
    private SemanticType semanticType;

    @Type(JsonType.class)
    @Column(name = "canonical_path", length = 3000)
    private String canonicalPath;

    @Column(name = "cardinality", length = 2)
    private String cardinality;

    @Column(name = "option_set_uid", length = 11)
    private String optionSetUid;

    @Column(name = "option_set_id", length = 11)
    private String optionSetId;

    /// last updated Localized display labels, e.g. `{"en": "Child Name", "ar": "..."}`.
    @Type(JsonType.class)
    @Column(name = "display_label", columnDefinition = "jsonb default '{}'::jsonb")
    private Map<String, String> displayLabel;


    @Singular
    @Type(JsonType.class)
    @Column(name = "json_data_paths", columnDefinition = "jsonb default '[]'::jsonb")
    private List<String> jsonDataPaths;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "created_date")
    private Instant createdDate;

    @Column(name = "last_modified_date")
    private Instant lastModifiedDate;
    // convenience method to get canonicalCandidates as List<String> via ObjectMapper
}
