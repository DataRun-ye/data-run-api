package org.nmcpye.datarun.drun.mongo.mapping.serialization;

import org.nmcpye.datarun.domain.OrgUnit;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

public class OrgUnitReference implements Serializable {
    private String uid;
    private String code;
    private String name;
    private String path;
    private String parent;
    private Map<String, String> label;

    public OrgUnitReference(OrgUnit orgUnit) {
        this.uid = orgUnit.getUid();
        this.code = orgUnit.getCode();
        this.name = orgUnit.getName();
        this.path = orgUnit.getOuPath();
        this.parent = orgUnit.getParent();
        this.label = Map.of("en", orgUnit.getName(), "ar", orgUnit.getName());
    }


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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrgUnitReference that = (OrgUnitReference) o;
        return Objects.equals(uid, that.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uid);
    }
}
