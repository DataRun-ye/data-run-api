package org.nmcpye.datarun.drun.postgres.domain;

import org.nmcpye.datarun.drun.postgres.domain.enumeration.FormPermission;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


public class TeamFormPermissions {
    private String form;

    private Set<FormPermission> permissions = new HashSet<>();

    public TeamFormPermissions() {
    }

    public TeamFormPermissions(String form, Set<FormPermission> permissions) {
        this.form = form;
        this.permissions = permissions;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public Set<FormPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<FormPermission> permissions) {
        this.permissions = permissions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TeamFormPermissions that)) return false;
        return Objects.equals(form, that.form) && Objects.equals(permissions, that.permissions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(form, permissions);
    }
}
