//package org.nmcpye.datarun.mongo.domain;
//
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Size;
//import org.nmcpye.datarun.drun.postgres.domain.enumeration.FormPermission;
//import org.springframework.data.annotation.Id;
//import org.springframework.data.mongodb.core.index.CompoundIndex;
//import org.springframework.data.mongodb.core.index.CompoundIndexes;
//import org.springframework.data.mongodb.core.mapping.Document;
//import org.springframework.data.mongodb.core.mapping.Field;
//
//import java.util.Objects;
//import java.util.Set;
//
//@Document(collection = "team_form_permission")
//@CompoundIndexes(
//    @CompoundIndex(name = "team_form_permission_index", def = "{'team' : 1, 'form' : 1}", unique = true)
//)
//public class TeamFormPermission {
//    @Id
//    private String id;
//
//    @Size(max = 11)
//    @Field("team")
//    @NotNull
//    private String team;
//
//    @Size(max = 11)
//    @Field("form")
//    @NotNull
//    private String form;
//
//    @Field("permissions")
//    private Set<FormPermission> permissions;
//
//    public TeamFormPermission(String team, String form, Set<FormPermission> permissions) {
//        this.team = team;
//        this.form = form;
//        this.permissions = permissions;
//    }
//
//    public String getId() {
//        return id;
//    }
//
//    public void setId(String id) {
//        this.id = id;
//    }
//
//    public String getTeam() {
//        return team;
//    }
//
//    public void setTeam(String team) {
//        this.team = team;
//    }
//
//    public String getForm() {
//        return form;
//    }
//
//    public void setForm(String form) {
//        this.form = form;
//    }
//
//    public Set<FormPermission> getPermissions() {
//        return permissions;
//    }
//
//    public void setPermissions(Set<FormPermission> permissions) {
//        this.permissions = permissions;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof TeamFormPermission that)) return false;
//        return Objects.equals(id, that.id) && Objects.equals(team, that.team) && Objects.equals(form, that.form);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(id, team, form);
//    }
//}
