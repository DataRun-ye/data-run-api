package org.nmcpye.datarun.jpa.reference;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableObject;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;

@Entity
@Table(
    name = "reference_entry",
    indexes = {
        @Index(name = "idx_reference_entry_org_unit_uid", columnList = "org_unit_id, uid"),
    })
@Getter
@Setter
public class ReferenceEntry extends JpaIdentifiableObject {

    @Size(min = 11, max = 11)
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9]{10}$")
    @Column(name = "uid", length = 11, nullable = false, updatable = false, unique = true)
    protected String uid;

    @NotBlank
    @Size(max = 255)
    @Column(name = "display_name", length = 255, nullable = false)
    private String displayName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_unit_id", nullable = false, updatable = false)
    @JsonIgnoreProperties(
        value = {"parent", "children", "assignments"},
        allowSetters = true)
    private OrgUnit orgUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "first_registered_activity_id", updatable = false)
    @JsonIgnoreProperties(value = {"assignments", "teams"}, allowSetters = true)
    private Activity firstRegisteredActivity;

    @JsonIgnore
    @Override
    public String getCode() {
        return null;
    }

    @JsonIgnore
    @Override
    public String getName() {
        return displayName;
    }
}
