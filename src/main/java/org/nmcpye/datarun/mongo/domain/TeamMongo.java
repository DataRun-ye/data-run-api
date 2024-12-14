//package org.nmcpye.datarun.drun.mongo.domain;
//
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Size;
//import org.nmcpye.datarun.domain.AbstractAuditingEntity;
//import org.springframework.data.annotation.Id;
//import org.springframework.data.mongodb.core.mapping.DBRef;
//import org.springframework.data.mongodb.core.mapping.Document;
//import org.springframework.data.mongodb.core.mapping.Field;
//
//import java.io.Serializable;
//import java.util.*;
//
//
///**
// * A Assignment.
// */
//@Document(collection = "team")
//@SuppressWarnings("common-java:DuplicatedBlocks")
//public class TeamMongo
//    extends AbstractAuditingEntity<String> implements Serializable {
//
//    private static final String PATH_SEP = ",";
//
//    private static final long serialVersionUID = 1L;
//
//    @Id
//    private String id;
//
//    @NotNull
//    @Size(max = 11)
//    @Field("uid")
//    private String uid;
//
//    @NotNull
//    private String code;
//
//    private String name;
//
//    private Boolean disabled;
//
//    @Field("delete_client_data")
//    private Boolean deleteClientData;
//
//    private String activity;
//
//    private String path;
//
//    @DBRef
//    @Field("team_users")
//    @JsonIgnoreProperties(value = {"team"}, allowSetters = true)
//    private final Set<TeamUser> teamUsers = new HashSet<>();
//
//    private Map<String, Object> properties = new HashMap<String, Object>();
//
//    @Override
//    public String getId() {
//        return id;
//    }
//
//    @Override
//    public void setId(String id) {
//        this.id = id;
//    }
//
//    @Override
//    public String getUid() {
//        return uid;
//    }
//
//    @Override
//    public void setUid(String uid) {
//        this.uid = uid;
//    }
//
//    @Override
//    public String getCode() {
//        return code;
//    }
//
//    @Override
//    public void setCode(String code) {
//        this.code = code;
//    }
//
//    @Override
//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public Boolean getDisabled() {
//        return disabled;
//    }
//
//    public void setDisabled(Boolean disabled) {
//        this.disabled = disabled;
//    }
//
//    public Boolean getDeleteClientData() {
//        return deleteClientData;
//    }
//
//    public void setDeleteClientData(Boolean deleteClientData) {
//        this.deleteClientData = deleteClientData;
//    }
//
//    public String getActivity() {
//        return activity;
//    }
//
//    public void setActivity(String activity) {
//        this.activity = activity;
//    }
//
//    public Map<String, Object> getProperties() {
//        return properties;
//    }
//
//    public void setProperties(Map<String, Object> properties) {
//        this.properties = properties;
//    }
//
//    public String getPath() {
//        return path;
//    }
//
//    public void setPath(String path) {
//        this.path = path;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof TeamMongo teamMongo)) return false;
//        return Objects.equals(getUid(), teamMongo.getUid()) && Objects.equals(getCode(), teamMongo.getCode()) && Objects.equals(getName(), teamMongo.getName()) && Objects.equals(getDisabled(), teamMongo.getDisabled()) && Objects.equals(getDeleteClientData(), teamMongo.getDeleteClientData()) && Objects.equals(getActivity(), teamMongo.getActivity()) && Objects.equals(getPath(), teamMongo.getPath()) && Objects.equals(teamUsers, teamMongo.teamUsers) && Objects.equals(getProperties(), teamMongo.getProperties());
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(super.hashCode(), getUid(), getCode(), getName(), getDisabled(), getDeleteClientData(), getActivity(), getPath(), teamUsers, getProperties());
//    }
//}
