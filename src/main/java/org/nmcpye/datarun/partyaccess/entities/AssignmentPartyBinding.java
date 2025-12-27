package org.nmcpye.datarun.partyaccess.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.time.Instant;

@Entity
@Table(name = "assignment_party_binding")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
public class AssignmentPartyBinding {
    @Id
    @Column(name = "id", length = 26, updatable = false, nullable = false)
    protected String id;

    @Column(name = "assignment_id")
    private String assignmentId;

    /**
     * Precedence Logic: The AssignmentPartyBinding entity correctly allows vocabularyId to be nullable,
     * enabling the "Global Role" vs "Specific Form Role" logic.
     * Adaptability: We are using vocabularyId in the DB but we use DataTemplate in our Services/Mappers
     * to keep it consistent with our existing code.
     */
    @Column(name = "vocabulary_id")
    private String vocabularyId; // Linked to DataTemplate

    @Column(name = "role_name")
    private String roleName;

    @Column(name = "party_set_id")
    private String partySetId;

    @Column(name = "created_date", updatable = false, nullable = false)
    private Instant createdDate;

    @Column(name = "last_modified_date", nullable = false)
    private Instant lastModifiedDate;
}
