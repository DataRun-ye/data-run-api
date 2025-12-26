package org.nmcpye.datarun.partyaccess.entities;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "party")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Party {
    @Id
    @Column(name = "id", length = 26, updatable = false, nullable = false)
    protected String id;

    /**
     * 11-char Business Key
     */
    @Column(length = 11, unique = true, nullable = false)
    private String uid;

    /**
     * ORG_UNIT, TEAM, USER, EXTERNAL
     */
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String name;

    private String parentId;

    @Column(columnDefinition = "jsonb default '[]'::jsonb")
    @Type(JsonType.class)
    private List<String> tags;

    @Column(columnDefinition = "jsonb default '{}'::jsonb")
    @Type(JsonType.class)
    private Map<String, Object> meta;

    @Column(name = "created_date", updatable = false)
    private Instant createdDate;
}
