package org.nmcpye.datarun.service.custom.dto;

import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link org.nmcpye.datarun.domain.WarehouseItem} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class WarehouseItemDTO implements Serializable {

    @Size(max = 11)
    private String uid;

    private String code;

    private String name;

    private String description;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WarehouseItemDTO)) {
            return false;
        }

        WarehouseItemDTO warehouseItemDTO = (WarehouseItemDTO) o;
        if (this.uid == null) {
            return false;
        }
        return Objects.equals(this.uid, warehouseItemDTO.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uid);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "WarehouseItemDTO{" +
            "uid=" + getUid() +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", description='" + getDescription() + "'" +
            "}";
    }
}
