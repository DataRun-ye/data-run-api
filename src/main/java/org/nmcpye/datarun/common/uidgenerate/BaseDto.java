package org.nmcpye.datarun.common.uidgenerate;

import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author Hamza Assada 09/06/2025 (7amza.it@gmail.com)
 */
@Getter
@Setter
@EqualsAndHashCode
public class BaseDto implements Serializable {
    @Size(max = 26)
    private String id;
    private String code;
}

//@MappedSuperclass
//public abstract class BaseEntity {
//    @Id
//    @Column(name = "id", columnDefinition = "BINARY(16)", updatable = false, nullable = false)
//    private ULID.Value id;
//
//    public BaseEntity() {
//        this.id = new ULID().nextValue();
//    }
//
//    public ULID.Value getUid() {
//        return id;
//    }
//
//    // Optionally: equals/hashCode based on id
//}
