//package org.nmcpye.datarun.etl;
//
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.nmcpye.datarun.etl.dao.RepeatInstancesDao;
//import org.nmcpye.datarun.etl.dao.SubmissionValuesDao;
//import org.nmcpye.datarun.etl.dto.SubmissionDto;
//import org.nmcpye.datarun.etl.exception.MissingRepeatUidException;
//import org.nmcpye.datarun.etl.repository.FormTemplateRepository;
//import org.nmcpye.datarun.etl.service.EtlService;
//import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.mockito.Mockito.verifyNoInteractions;
//import static org.mockito.Mockito.when;
//
//// EtlServiceTest.java
//@ExtendWith(MockitoExtension.class)
//public class EtlServiceTest {
//
//    @Mock
//    NamedParameterJdbcTemplate jdbc; // not used directly in EtlService if DAOs mocked
//    @Mock
//    FormTemplateRepository templateRepo;
//    @Mock
//    RepeatInstancesDao repeatDao;
//    @Mock
//    SubmissionValuesDao valuesDao;
//
//    @InjectMocks
//    EtlService etlService;
//
//    @Test
//    void testRejectMissingUid() {
//        // setup template with one repeatPath "visits"
//        FormTemplate t = mockTemplateWithRepeatPath("visits", Map.of("age","ELEM-1"));
//        when(templateRepo.findByUid("FT-1")).thenReturn(t);
//
//        Map<String, Object> formData = new HashMap<>();
//        List<Map<String,Object>> repeats = new ArrayList<>();
//        repeats.add(Map.of("age", 10)); // missing _uid
//        formData.put("visits", repeats);
//
//        SubmissionDto dto = new SubmissionDto(1L, "FT-1", Instant.now(), formData);
//
//        MissingRepeatUidException ex = assertThrows(MissingRepeatUidException.class, () -> etlService.processSubmission(dto));
//        assertFalse(ex.getDetails().isEmpty());
//        verifyNoInteractions(repeatDao, valuesDao);
//    }
//
//    @Test
//    void testUpsertAndMarkSweep() {
//        // mock template, templateRepo, incoming items with UIDs A,B,
//        // existing repeatDao.findActiveRepeatUids returns [A,B,C] => should mark C deleted
//        // verify repeatDao.upsertRepeatInstance called for A,B
//        // verify submissionValuesDao.upsertSubmissionValue called expected times
//        // verify markRepeatInstancesDeleted called with [C]
//    }
//}
