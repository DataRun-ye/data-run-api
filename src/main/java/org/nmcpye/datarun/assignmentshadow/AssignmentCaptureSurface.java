package org.nmcpye.datarun.assignmentshadow;

public enum AssignmentCaptureSurface {
    ASSIGNMENT_LIST("assignment_list"),
    ASSIGNMENT_FORMS("assignment_forms"),
    ORG_UNIT_SYNC("org_unit_sync"),
    REFERENCE_CATALOG("reference_catalog"),
    VERSIONED_UPLOAD("versioned_upload");

    private final String tag;

    AssignmentCaptureSurface(String tag) {
        this.tag = tag;
    }

    public String tag() {
        return tag;
    }
}
