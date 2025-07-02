package org.nmcpye.datarun.jpa.option;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;

import java.util.HashSet;
import java.util.Set;

/**
 * option groups are created and managed within a specific option set. When
 * you create an option group, you must select the option set it belongs
 * to, and you cannot change this association later. This means option
 * groups are not unique across the entire system, but are unique within the
 * context of their assigned option set. The main purpose is to group and
 * classify options within a particular option set, allowing for easier management
 * and filtering of large sets of options. Option group sets also operate within
 * the context of a single option set and cannot span multiple option sets Manage
 * option sets.
 *
 * @author Hamza Assada 30/06/2025 (7amza.it@gmail.com)
 */
@Entity
@Table(name = "option_group", uniqueConstraints = {
        @UniqueConstraint(name = "uc_group_option_set_name", columnNames = {"name", "option_set_id"}),
        @UniqueConstraint(name = "uc_group_option_set_code", columnNames = {"code", "option_set_id"}),
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OptionGroup extends
        JpaBaseIdentifiableObject {
    /**
     * The unique code for this object.
     */
    @Column(name = "code")
    private String code;

    /**
     * The name of this object. Required and unique.
     */
    @Column(name = "name", nullable = false)
    private String name;

    @Size(max = 50)
    @Column(name = "shortname", length = 50)
    private String shortName;

    @Column(name = "description")
    private String description;

    /**
     * when set you cannot change this association later
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_set_id", updatable = false, nullable = false)
    private OptionSet optionSet;

    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinTable(
            name = "option_group__options",
            joinColumns = @JoinColumn(name = "option_group_id"),
            inverseJoinColumns = @JoinColumn(name = "option_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Option> options = new HashSet<>();

    @JsonProperty("options")
    @JsonSerialize(contentAs = JpaBaseIdentifiableObject.class)
    public Set<Option> getOptions() {
        return options;
    }

    @JsonProperty("optionSet")
    @JsonSerialize(as = JpaBaseIdentifiableObject.class)
    public OptionSet getOptionSet() {
        return optionSet;
    }

    public void updateOptions(Set<Option> updates) {
        for (Option option : new HashSet<>(options)) {
            if (!updates.contains(option)) {
                removeOption(option);
            }
        }

        for (Option option : updates) {
            addOption(option);
        }
    }

    public void addOption(Option option) {
        options.add(option);
    }

    public void removeOption(Option option) {
        options.remove(option);
    }

    @Override
    protected void setUid(String uid) {

    }

    @JsonIgnore
    @Override
    public String getUid() {
        return null;
    }
}
