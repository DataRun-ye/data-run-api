package org.nmcpye.datarun.jpa.reference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.datasubmission.validation.DomainValidationException;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReferenceValueExtractorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ReferenceValueExtractor extractor;
    private DataTemplateInstanceDto template;

    @BeforeEach
    void setUp() {
        extractor = new ReferenceValueExtractor();
        template = mock(DataTemplateInstanceDto.class);
        when(template.getFields()).thenReturn(List.of(
            field("topId", "topReference", "topId", ValueType.Reference),
            field("rowId", "rowReference", "rows.rowId", ValueType.Reference),
            field(
                "nestedId",
                "nestedReference",
                "rows.nested.nestedId",
                ValueType.Reference),
            field("textId", "otherText", "rows.textId", ValueType.Text)));
    }

    @Test
    void extractsOrdinaryRepeatAndNestedRepeatValuesFromTemplatePaths()
        throws Exception {
        var formData = objectMapper.readTree("""
            {
              "topReference": "a1234567890",
              "rows": [
                {
                  "rowReference": "b1234567890",
                  "otherText": "not-a-reference",
                  "nested": [
                    {"nestedReference": "c1234567890"},
                    {"nestedReference": null}
                  ]
                },
                {
                  "rowReference": null,
                  "nested": [
                    {"nestedReference": "d1234567890"}
                  ]
                }
              ]
            }
            """);

        assertEquals(
            List.of(
                new ReferenceValueOccurrence("topId", "a1234567890"),
                new ReferenceValueOccurrence(
                    "rows.rowId",
                    "b1234567890"),
                new ReferenceValueOccurrence(
                    "rows.nested.nestedId",
                    "c1234567890"),
                new ReferenceValueOccurrence(
                    "rows.nested.nestedId",
                    "d1234567890")),
            extractor.extract("submission1", template, formData));
    }

    @Test
    void ignoresMissingNullAndBlankValues() throws Exception {
        var formData = objectMapper.readTree("""
            {
              "topReference": " ",
              "rows": [
                {"rowReference": null},
                {}
              ]
            }
            """);

        assertEquals(
            List.of(),
            extractor.extract("submission1", template, formData));
    }

    @Test
    void rejectsObjectNumberAndArrayReferenceValues() throws Exception {
        for (String value : List.of(
            "{}",
            "42",
            "[\"a1234567890\"]")) {
            var formData = objectMapper.readTree(
                "{\"topReference\":" + value + "}");
            assertThrows(
                DomainValidationException.class,
                () -> extractor.extract(
                    "submission1",
                    template,
                    formData));
        }
    }

    private FormDataElementConf field(
        String id,
        String name,
        String path,
        ValueType type) {
        FormDataElementConf field = new FormDataElementConf();
        field.setId(id);
        field.setName(name);
        field.setPath(path);
        field.setType(type);
        return field;
    }
}
