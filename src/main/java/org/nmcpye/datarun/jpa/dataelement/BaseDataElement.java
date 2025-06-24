package org.nmcpye.datarun.jpa.dataelement;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;

/**
 * A DataElement, a configuration definition of a data element
 * A Data Element provide the basic configuration properties for any Field,
 * and can be extended with other special configuration properties
 * Elements that extend this [DimensionalElement, EntityAttributeType]
 *
 * @author Hamza Assada 08/02/2024 <7amza.it@gmail.com>
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("common-java:DuplicatedBlocks")
public class BaseDataElement extends JpaBaseIdentifiableObject {
    @Size(max = 11)
    @Column(name = "uid", length = 11, updatable = false, unique = true)
    protected String uid;
    /**
     * The code for this Element.
     * unique, but not required
     */
    @Column(name = "code", unique = true)
    protected String code;

    /**
     * The name of this data element.
     * Required and unique with no spaces.
     */
    @Column(name = "name", nullable = false, unique = true)
    protected String name;

    @Column(name = "short_name", length = 50)
    protected String shortName;

    @Size(max = 2000)
    @Column(name = "description")
    protected String description;
}
