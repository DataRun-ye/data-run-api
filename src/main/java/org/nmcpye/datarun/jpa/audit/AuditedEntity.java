//package org.nmcpye.datarun.jpa.audit;
//
//public enum AuditedEntity {
//    PROJECT(org.nmcpye.datarun.jpa.project.Project.class, "Project"),
//    ACTIVITY(org.nmcpye.datarun.jpa.activity.Activity.class, "Activity"),
//    ORG_UNIT(org.nmcpye.datarun.jpa.orgunit.OrgUnit.class, "OrgUnit"),
//    TEAM(org.nmcpye.datarun.jpa.team.Team.class, "Team"),
//    ASSIGNMENT(org.nmcpye.datarun.jpa.assignment.Assignment.class, "Assignment"),
//    OPTION_SET(org.nmcpye.datarun.jpa.option.OptionSet.class, "OptionSet"),
//    DATA_ELEMENT(org.nmcpye.datarun.jpa.dataelement.DataTemplateElement.class, "DataTemplateElement"),
//    DATA_TEMPLATE(org.nmcpye.datarun.jpa.datatemplate.DataTemplate.class, "DataTemplate");
//
//    private final Class<?> entityClass;
//    private final String eventEntityType;
//
//    private AuditedEntity(Class<?> entityClass, String eventEntityType) {
//        this.entityClass = entityClass;
//        this.eventEntityType = eventEntityType;
//    }
//
//
//    public Class<?> getEntityClass() {
//        return entityClass;
//    }
//
//    public String getEventEntityType() {
//        return eventEntityType;
//    }
//}
