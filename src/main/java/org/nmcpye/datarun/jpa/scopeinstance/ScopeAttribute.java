package org.nmcpye.datarun.jpa.scopeinstance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Stores key-value pairs for dynamic less commonly used scope dimensions,
 * Linked to either a FlowScope or a StageScope.
 *
 * @author Hamza Assada 14/06/2025 <7amza.it@gmail.com>
 */
@Entity
@Table(name = "scope_attribute_value")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ScopeAttribute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * relationship with FlowScope (nullable, as it's either FlowScope or StageScope).
     */
    @ManyToOne
    @JoinColumn(name = "scope_id")
    @JsonIgnoreProperties(value = {"attributes", "coreElements"}, allowSetters = true)
    private WorkflowContext scope;

    @ManyToOne
    @JoinColumn(name = "scope_element_id")
    private ScopeElementDefinition scopeElement;

    /**
     * Value of the dynamic attribute (stored as TEXT).
     */
    @Column(name = "attribute_value", nullable = false, columnDefinition = "TEXT")
    private String value;
}
