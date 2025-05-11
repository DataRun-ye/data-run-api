package org.nmcpye.datarun.optionset;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.common.jpa.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.mongo.domain.DataOption;

import java.util.List;

/**
 * An OptionSet.
 */
@Entity
@Table(name = "option_set", indexes = {
    @Index(name = "idx_optionset_uid_unq", columnList = "uid", unique = true)
}, uniqueConstraints = {
    @UniqueConstraint(name = "uc_option_set_name",
        columnNames = "name"),
    @UniqueConstraint(name = "uc_option_set_code", columnNames = "code")
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OptionSet extends JpaBaseIdentifiableObject {
    @Type(JsonType.class)
    @Column(name = "options", columnDefinition = "jsonb")
    private List<DataOption> options;
}
