package org.nmcpye.datarun.jpa.assignment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.party.dto.CombineMode;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/// @author Hamza Assada 28/12/2025
@Entity
@Table(name = "assignment_party_binding",
    indexes = {@Index(name = "idx_apb_assignment_vocab_role",
        columnList = "assignment_id,vocabulary_id,role_name")})
@EntityListeners(AuditingEntityListener.class)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentPartyBindingOld {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(length = 11, unique = true, nullable = false, updatable = false)
    private String uid;

    /// role Name
    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "assignment_id", length = 26, nullable = false)
    private String assignmentId;


    /// Precedence Logic: The AssignmentPartyBinding entity correctly allows vocabularyId to be nullable,
    /// enabling the "Global Role" vs "Specific Form Role" logic.
    /// Adaptability: We are using vocabularyId in the DB, but we use DataTemplate in our Services/Mappers
    /// to keep it consistent with our existing code.
    @Column(name = "vocabulary_id")
    private String vocabularyId; // dataTemplate id (nullable)

    @Column(name = "party_set_id", nullable = false)
    private UUID partySetId;

    @Column(name = "principal_type", length = 64)
    private String principalType;

    @Column(name = "principal_id", length = 26)
    private String principalId;

    @Column(name = "combine_mode", length = 32)
    private CombineMode combineMode;

    @CreatedBy
    @Column(name = "created_by", nullable = false, length = 50, updatable = false)
    protected String createdBy;

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 50)
    protected String lastModifiedBy;

    @CreatedDate
    @Column(name = "created_date", updatable = false)
    protected Instant createdDate;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    protected Instant lastModifiedDate;
}
