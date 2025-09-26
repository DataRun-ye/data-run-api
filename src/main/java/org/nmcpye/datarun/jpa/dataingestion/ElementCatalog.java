//package org.nmcpye.datarun.jpa.dataingestion;
//
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.Id;
//import jakarta.persistence.Table;
//
//import java.util.UUID;
//
//@Entity
//@Table(name = "element_catalog")
//public class ElementCatalog {
//    @Id
//    @Column(name = "element_id")
//    private UUID elementId;
//
//    @Column(name = "template_version_id")
//    private String templateVersionId;
//
//    @Column(name = "name")
//    private String name; // semantic path like 'root.patient.name'
//
//    @Column(name = "ui_name")
//    private String uiName;
//
//    @Column(name = "type")
//    private String type;
//
//    @Column(name = "semantic_type")
//    private String semanticType;
//
//    @Column(name = "sensitivity")
//    private String sensitivity;
//
//    // getters/setters omitted
//    public UUID getElementId() { return elementId; }
//    public String getType() { return type; }
//}
