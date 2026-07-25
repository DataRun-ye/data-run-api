package org.nmcpye.datarun.web.rest.v1.datasubmission;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.web.rest.v1.datasubmission.dto.DataSubmissionUploadV1Dto;
import org.nmcpye.datarun.web.rest.v1.datasubmission.mapper.DataSubmissionUploadV1Mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class DataSubmissionUploadV1MapperTest {

    @Test
    void currentMobilePayloadMapsWithoutPersistingTransportOnlyFields()
        throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        DataSubmissionUploadV1Dto request = objectMapper.readValue("""
            {
              "uid": "a1234567890",
              "form": "f1234567890",
              "formVersion": "v1234567890",
              "assignment": "s1234567890",
              "deleted": false,
              "lastSyncMassage": "ignored mobile state",
              "lastSyncDate": "2026-07-25T00:00:00Z",
              "startEntryTime": "2026-07-25T01:00:00Z",
              "finishedEntryTime": "2026-07-25T02:00:00Z",
              "formData": {"referenceField": "r1234567890"},
              "referenceDefinitions": [
                {
                  "uid": "r1234567890",
                  "name": "Ahmed Ali Saleh Hassan"
                }
              ]
            }
            """, DataSubmissionUploadV1Dto.class);

        var entity = new DataSubmissionUploadV1Mapper()
            .toEntity(request);

        assertEquals("a1234567890", entity.getUid());
        assertEquals("f1234567890", entity.getForm());
        assertEquals("v1234567890", entity.getFormVersion());
        assertEquals("s1234567890", entity.getAssignment());
        assertFalse(entity.getDeleted());
        assertEquals(
            "r1234567890",
            entity.getFormData().get("referenceField").asText());
        assertEquals(
            "r1234567890",
            request.getReferenceDefinitions().get(0).getUid());
    }

    @Test
    void absentDeletedFlagKeepsEntityDefault() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        DataSubmissionUploadV1Dto request = objectMapper.readValue(
            "{\"uid\":\"a1234567890\"}",
            DataSubmissionUploadV1Dto.class);

        var entity = new DataSubmissionUploadV1Mapper()
            .toEntity(request);

        assertFalse(entity.getDeleted());
        assertNull(request.getReferenceDefinitions());
    }
}
