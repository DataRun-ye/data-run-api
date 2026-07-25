package org.nmcpye.datarun.datatemplateprocessor;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.datatemplateprocessor.postprocessors.AbstractFormElementHandler;
import org.nmcpye.datarun.jpa.dataelement.DataElement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BoundedReferenceTemplateProcessingTest {

    @Test
    void referenceFieldDoesNotRequireLegacyMetadataSchema() {
        DataElement source = new DataElement();
        source.setUid("r1234567890");
        source.setName("referenceField");
        source.setValueType(ValueType.Reference);

        FormDataElementConf field = new FormDataElementConf();
        field.setId(source.getUid());
        field.setType(ValueType.Reference);

        FormDataElementConf processed = assertDoesNotThrow(
            () -> AbstractFormElementHandler.processElement(field, source));

        assertEquals(ValueType.Reference, processed.getType());
        assertNull(processed.getResourceMetadataSchema());
    }
}
