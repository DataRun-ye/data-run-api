package org.nmcpye.datarun.partyaccess.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableObject;

@Entity
@Table(name = "assignment_party_binding",
    indexes = {@Index(name = "idx_apb_assignment_vocab_role",
        columnList = "assignment_id,vocabulary_id,role_name")})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentPartyBinding extends JpaIdentifiableObject {
    @Column(length = 11, unique = true, nullable = false, updatable = false)
    private String uid;

    /**
     * role Name
     */
    @Column(name = "name")
    private String name;

    @Column(name = "assignment_id", length = 26, nullable = false)
    private String assignmentId;


    /**
     * Precedence Logic: The AssignmentPartyBinding entity correctly allows vocabularyId to be nullable,
     * enabling the "Global Role" vs "Specific Form Role" logic.
     * Adaptability: We are using vocabularyId in the DB but we use DataTemplate in our Services/Mappers
     * to keep it consistent with our existing code.
     */
    @Column(name = "vocabulary_id")
    private String vocabularyId; // dataTemplate id (nullable)

    @Column(name = "party_set_id", length = 26, nullable = false)
    private String partySetId;

    @JsonIgnore
    @Override
    public String getCode() {
        return null;
    }
}
