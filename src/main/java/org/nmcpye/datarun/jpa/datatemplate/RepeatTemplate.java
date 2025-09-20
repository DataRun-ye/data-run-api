package org.nmcpye.datarun.jpa.datatemplate;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;

import java.util.Set;

/**
 * @author Hamza Assada
 * @since 20/09/2025
 */
@Entity
@Table(name = "repeat_template", indexes = {
    @Index(name = "idx_repeat_template_semantics", columnList = "semantics"),
    @Index(name = "idx_repeat_template_category", columnList = "category_element_uid"),
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString(onlyExplicitlyIncluded = true)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class RepeatTemplate extends JpaBaseIdentifiableObject {
    /// Default projection mode (derived from repeat_template.semantics)
    ///
    /// `EVENT` → append or upsert by natural_key (payload._id by default).
    ///
    /// `ENTITY` → upsert with entity-resolution later.
    ///
    /// `RELATIONSHIP` → append into relationship table with explicit FK columns.
    ///
    /// `ATTRIBUTE` → consider storing as jsonb extra + a flattened column set optional.
    public enum Semantics {
        EVENT,
        ENTITY,
        ATTRIBUTE,
        RELATIONSHIP,
        COLLECTION
    }

    @Size(max = 11)
    @Column(name = "uid", length = 11, updatable = false, unique = true)
    protected String uid;

    @Column(name = "code")
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    /// `default_role` (EVENT | ENTITY | RELATIONSHIP | ATTRIBUTE | COLLECTION) — *default hint*
    @Enumerated(EnumType.STRING)
    @Column(name = "semantics", nullable = false, length = 32, columnDefinition = "default 'EVENT'")
    private Semantics semantics = Semantics.EVENT;

    @Column(name = "category_element_uid", length = 26)
    private String categoryElementUid;

    /// (jsonb array of element_uids), is crucial for dedupe & idempotency.
    /// if not present fallback to payload._id.
    /// If you need cross-submission merging (e.g., order line mapped to same product across submissions),
    /// handle in entity-resolution.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "natural_keys", columnDefinition = "jsonb default '[]'::jsonb")
    private Set<String> naturalKeys;
}
