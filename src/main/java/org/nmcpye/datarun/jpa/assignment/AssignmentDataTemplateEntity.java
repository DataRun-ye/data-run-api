package org.nmcpye.datarun.jpa.assignment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.common.SoftDeleteObject;
import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/// Control which `data_template` (vocabulary) a *principal* (user/team/group or a `member role`) can see/use inside
/// a specific assignment. e.g. preventing HF users from seeing `Issue` while letting MU officers see it — without hacks.
///
/// @author Hamza Assada 06/01/2026
@Entity
@Table(name = "assignment_data_template")
@EntityListeners(AuditingEntityListener.class)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AssignmentDataTemplateEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = {"defaultPartySet", "properties", "activity", "team", "forms",
        "orgUnit", "parent", "children", "ancestors", "level", "createdBy", "createdDate",
        "lastModifiedDate", "lastModifiedBy"}, allowSetters = true)
    @JsonSerialize(contentAs = SoftDeleteObject.class)
    private Assignment assignment;

    /// Precedence Logic: The AssignmentPartyBinding entity correctly allows vocabularyId to be nullable,
    /// enabling the "Global Role" vs "Specific Form Role" logic.
    /// Adaptability: We are using vocabularyId in the DB, but we use DataTemplate in our Services/Mappers
    /// to keep it consistent with our existing code.
    @ManyToOne
    @JoinColumn(name = "data_template_id", nullable = false)
    @JsonSerialize(contentAs = SoftDeleteObject.class)
    private DataTemplate dataTemplate;

    @Column(name = "principal_type", length = 64)
    private String principalType;

    @Column(name = "principal_id", length = 26)
    private String principalId;

    @Column(name="principal_role")
    private String principalRole;

    @CreatedBy
    @Column(name = "created_by", nullable = false, length = 50, updatable = false)
    protected String createdBy;

    @CreatedDate
    @Column(name = "created_date", updatable = false)
    protected Instant createdDate;

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 50)
    protected String lastModifiedBy;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    protected Instant lastModifiedDate;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AssignmentDataTemplateEntity that = (AssignmentDataTemplateEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
