package org.nmcpye.datarun.drun.mongo.domain;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * A DataOption.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataOption implements Serializable {

    private static final long serialVersionUID = 1L;

//    // formUid_listName_optionUid
//    @Size(max = 11)
//    @NotNull
//    @Field("uid")
//    private String uid;

//    private String form;

    @NotNull
    private String listName;

    @NotNull
    private String name;

//    @Size(max = 2000)
//    private String description;

    private Integer order;

//    public String getForm() {
//        return form;
//    }
//
//    public void setForm(String form) {
//        this.form = form;
//    }

    public Map<String, String> getLabel() {
        if (this.label == null) {
            return Map.of("en", this.name);
        }
        return label;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = Objects.requireNonNullElseGet(order, () -> 0);
    }

//    public String getUid() {
//        return this.uid;
//    }

//    public DataOption uid(String uid) {
//        this.setUid(uid);
//        return this;
//    }

//    public void setUid(String uid) {
//        this.uid = uid;
//    }

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    public String getName() {
        return this.name;
    }

    public DataOption name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    private Map<String, String> label;

    public void setLabel(Map<String, String> label) {
        this.label = Objects.requireNonNullElseGet(label, () -> Map.of("en", this.name));
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DataOption)) {
            return false;
        }
        return false;
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DataOption{" +
//            ", uid='" + getUid() + "'" +
            ", name='" + getName() + "'" +
//            ", description='" + getDescription() + "'" +
            "}";
    }
}
