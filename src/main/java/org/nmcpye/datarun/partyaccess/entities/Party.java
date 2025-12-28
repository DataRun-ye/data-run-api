package org.nmcpye.datarun.partyaccess.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.jpa.common.TranslatableIdentifiable;

import java.util.List;
import java.util.Map;

/// @author Hamza Assada 28/12/2025
@Entity
@Table(name = "party")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Party extends TranslatableIdentifiable {
    /// 11-char Business Key
    @Column(length = 11, unique = true, nullable = false, updatable = false)
    private String uid;

    protected String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "jsonb default '[]'::jsonb")
    @Type(JsonType.class)
    private List<String> tags;

    @Type(JsonType.class)
    @Column(name = "properties_map", columnDefinition = "jsonb")
    @JsonProperty
    private Map<String, Object> properties;

    /// org_unit, team, user, external, static
    @Column(nullable = false)
    private String type;

    @Column(name="source_id", length = 26)
    private String sourceId;

    /// for parties with parents such as orgUnits
    @Column(name = "parent_id")
    private String parentId;
}
