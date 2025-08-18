//package org.nmcpye.datarun.jpa.etl;
//
//import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
//import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
//import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
//import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
//import org.nmcpye.datarun.jpa.etl.model.TemplateElementMap;
//
//import java.util.List;
//import java.util.Map;
//
///**
// * Small factory/builder helpers to create template DTOs & submission payloads cleanly.
// *
// * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
// */
//public final class TestBuilders {
//
//    public static FormDataElementConf textField(String id, String name, String path) {
//        FormDataElementConf f = new FormDataElementConf();
//        f.setId(id);
//        f.setName(name);
//        f.setPath(path);
//        // default type text
//        // f.setType(ValueType.Text); // import if needed
//        return f;
//    }
//
//    public static FormDataElementConf multiSelectField(String id, String name, String path, String optionSetId) {
//        FormDataElementConf f = new FormDataElementConf();
//        f.setId(id);
//        f.setName(name);
//        f.setPath(path);
//        f.setType(ValueType.SelectMulti);
//        f.setOptionSet(optionSetId);
//        return f;
//    }
//
//    public static FormSectionConf repeatSection(String name, String path, boolean repeatable, String repeatCategoryElement) {
//        FormSectionConf s = new FormSectionConf();
//        s.setName(name);
//        s.setPath(path);
//        s.setRepeatable(repeatable);
//        s.setRepeatCategoryElement(repeatCategoryElement);
//        return s;
//    }
//
//    public static TemplateElementMap template(String templateId, List<FormDataElementConf> fields, List<FormSectionConf> sections) {
//        DataTemplateInstanceDto dto = new DataTemplateInstanceDto(
//            templateId, "v1", 1, "templName", "desc", false, Map.of(), fields, sections);
//        return new TemplateElementMap(dto);
//    }
//
//    // builder for TestDataFormSubmission (if not using your exact subclass)
//    public static EtlIntegrationScenariosIT.TestDataFormSubmission submission(String instanceId, String templateId, Map<String, Object> data) {
//        return new EtlIntegrationScenariosIT.TestDataFormSubmission(instanceId, templateId, "v1", "assign-1", data, java.time.Instant.now());
//    }
//}
