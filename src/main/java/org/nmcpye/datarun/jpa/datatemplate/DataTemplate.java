package org.nmcpye.datarun.jpa.datatemplate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.jpa.common.JpaSoftDeleteObject;

import java.time.Instant;

/**
 * @author Hamza Assada 27/05/2025 (7amza.it@gmail.com)
 */
@Entity
@Table(name = "data_template")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataTemplate extends JpaSoftDeleteObject {
    @Size(max = 11)
    @Column(name = "uid", length = 11, updatable = false, unique = true)
    protected String uid;

    /**
     * The unique code for this object.
     */
    @Column(name = "code", unique = true)
    protected String code;

    /**
     * The name of this object. Required and unique.
     */
    @Column(name = "name", nullable = false, unique = true)
    protected String name;

    @NotNull
    @Column(name = "version_uid", nullable = false, unique = true)
    private String versionUid;

    @NotNull
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber = 0;

    @Column(name = "deleted")
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "description", length = 2000)
    private String description;

    public DataTemplate pumpVersion() {
        this.setVersionNumber(versionNumber + 1);
        return this;
    }

    public DataTemplate versionNumber(Integer currentVersion) {
        setVersionNumber(currentVersion);
        return this;
    }

    public DataTemplate id(String id) {
        setId(id);
        return this;
    }

    public DataTemplate uid(String uid) {
        setUid(uid);
        return this;
    }

    public DataTemplate versionUid(String versionUid) {
        this.setVersionUid(versionUid);
        return this;
    }
}
