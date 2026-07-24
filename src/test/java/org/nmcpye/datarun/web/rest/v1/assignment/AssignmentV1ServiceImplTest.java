package org.nmcpye.datarun.web.rest.v1.assignment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.datatemplateprocessor.ReferenceTemplateCapabilityService;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.service.AssignmentService;
import org.nmcpye.datarun.web.query.QueryRequest;
import org.nmcpye.datarun.web.rest.v1.assignment.dto.AssignmentFormV1Dto;
import org.nmcpye.datarun.web.rest.v1.assignment.dto.AssignmentWithAccessV1Dto;
import org.nmcpye.datarun.web.rest.v1.assignment.mapper.AssignmentV1Mapper;
import org.nmcpye.datarun.web.rest.v1.assignment.mapper.AssignmentWithAccessV1Mapper;
import org.nmcpye.datarun.web.rest.v1.assignment.service.AssignmentV1ServiceImpl;
import org.springframework.data.domain.PageImpl;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AssignmentV1ServiceImplTest {

    private AssignmentService assignmentService;
    private AssignmentWithAccessV1Mapper accessMapper;
    private ReferenceTemplateCapabilityService capabilityService;
    private AssignmentV1ServiceImpl service;
    private QueryRequest queryRequest;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        assignmentService = mock(AssignmentService.class);
        AssignmentV1Mapper assignmentMapper = mock(AssignmentV1Mapper.class);
        accessMapper = mock(AssignmentWithAccessV1Mapper.class);
        capabilityService = mock(ReferenceTemplateCapabilityService.class);
        service = new AssignmentV1ServiceImpl(
                assignmentService,
                assignmentMapper,
                accessMapper,
                capabilityService);

        queryRequest = new QueryRequest();
        assignment = new Assignment();
        assignment.setUid("assignment1");
        assignment.setForms(new HashSet<>(Set.of("ordinary01", "reference01")));
        when(assignmentService.findAllByUser(queryRequest, null))
                .thenReturn(new PageImpl<>(java.util.List.of(assignment)));
    }

    @Test
    void oldClientKeepsOrdinaryFormsAndFiltersReferenceForms() {
        when(capabilityService.findReferenceTemplateUids(Set.of("ordinary01", "reference01")))
                .thenReturn(Set.of("reference01"));
        when(accessMapper.toDto(assignment)).thenReturn(dtoWithBothForms());

        var response = service.getAllWithAccess(queryRequest, null, 0);

        assertEquals(
                Set.of("ordinary01"),
                response.getItems().get(0).getAccessibleForms().stream()
                        .map(AssignmentFormV1Dto::getForm)
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void referenceVersionOneKeepsReferenceForms() {
        when(accessMapper.toDto(assignment)).thenReturn(dtoWithBothForms());

        var response = service.getAllWithAccess(queryRequest, null, 1);

        assertEquals(2, response.getItems().get(0).getAccessibleForms().size());
        verifyNoInteractions(capabilityService);
    }

    private AssignmentWithAccessV1Dto dtoWithBothForms() {
        AssignmentWithAccessV1Dto dto = new AssignmentWithAccessV1Dto();
        dto.setAccessibleForms(new HashSet<>(Set.of(
                AssignmentFormV1Dto.builder().form("ordinary01").build(),
                AssignmentFormV1Dto.builder().form("reference01").build())));
        return dto;
    }
}
