package org.nmcpye.datarun.drun.mongo.domain;

import java.util.List;
import java.util.Objects;

public class ReferenceData {
    private String form;
    private String field;
    private List<DataFormSubmission> ReferenceSubmissions;

    public ReferenceData(String form, String field,
                         List<DataFormSubmission> referenceSubmissions) {
        this.form = form;
        this.field = field;
        ReferenceSubmissions = referenceSubmissions;
    }

    void addReferenceSubmission(DataFormSubmission submission) {
        ReferenceSubmissions.add(submission);
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public List<DataFormSubmission> getReferenceSubmissions() {
        return ReferenceSubmissions;
    }

    public void setReferenceSubmissions(List<DataFormSubmission> referenceSubmissions) {
        ReferenceSubmissions = referenceSubmissions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReferenceData that = (ReferenceData) o;
        return Objects.equals(form, that.form) && Objects.equals(field, that.field) && Objects.equals(ReferenceSubmissions, that.ReferenceSubmissions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(form, field, ReferenceSubmissions);
    }
}
