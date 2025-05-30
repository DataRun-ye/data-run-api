package org.nmcpye.datarun.optionset;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
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
@Table(name = "option_set", uniqueConstraints = {
    @UniqueConstraint(name = "uc_option_set_uid", columnNames = "uid"),
    @UniqueConstraint(name = "uc_option_set_name", columnNames = "name"),
    @UniqueConstraint(name = "uc_option_set_code", columnNames = "code")
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OptionSet extends JpaBaseIdentifiableObject {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    protected Long id;

    @Size(max = 11)
    @Column(name = "uid", length = 11, nullable = false)
    protected String uid;

    /**
     * The unique code for this object.
     */
    @Column(name = "code")
    protected String code;

    /**
     * The name of this object. Required and unique.
     */
    @Column(name = "name", nullable = false)
    protected String name;

    @Type(JsonType.class)
    @Column(name = "options", columnDefinition = "jsonb")
    private List<DataOption> options;

//    @OneToMany(cascade = CascadeType.ALL)
//    @JoinColumn(name = "option_set_id")
//    @OrderColumn(name = "sort_order")
//    @ListIndexBase(1)
//    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
//    private List<Option> optionSetOptions = new ArrayList<>();
}
