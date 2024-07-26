package org.nmcpye.datarun.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.nmcpye.datarun.domain.AssignmentAsserts.*;
import static org.nmcpye.datarun.web.rest.TestUtil.createUpdateProxyForBean;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.domain.Assignment;
import org.nmcpye.datarun.domain.Team;
import org.nmcpye.datarun.repository.AssignmentRepository;
import org.nmcpye.datarun.service.AssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link AssignmentResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class AssignmentResourceIT {

    private static final String DEFAULT_UID = "AAAAAAAAAA";
    private static final String UPDATED_UID = "BBBBBBBBBB";

    private static final String DEFAULT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_CODE = "BBBBBBBBBB";

    private static final Integer DEFAULT_PHASE_NO = 1;
    private static final Integer UPDATED_PHASE_NO = 2;

    private static final Integer DEFAULT_DISTRICT_CODE = 1;
    private static final Integer UPDATED_DISTRICT_CODE = 2;

    private static final String DEFAULT_GOV = "AAAAAAAAAA";
    private static final String UPDATED_GOV = "BBBBBBBBBB";

    private static final String DEFAULT_DISTRICT = "AAAAAAAAAA";
    private static final String UPDATED_DISTRICT = "BBBBBBBBBB";

    private static final String DEFAULT_SUBDISTRICT = "AAAAAAAAAA";
    private static final String UPDATED_SUBDISTRICT = "BBBBBBBBBB";

    private static final String DEFAULT_VILLAGE = "AAAAAAAAAA";
    private static final String UPDATED_VILLAGE = "BBBBBBBBBB";

    private static final String DEFAULT_SUBVILLAGE = "AAAAAAAAAA";
    private static final String UPDATED_SUBVILLAGE = "BBBBBBBBBB";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final Integer DEFAULT_DAY_ID = 1;
    private static final Integer UPDATED_DAY_ID = 2;

    private static final Double DEFAULT_POPULATION = 1D;
    private static final Double UPDATED_POPULATION = 2D;

    private static final Integer DEFAULT_ITNS_PLANNED = 1;
    private static final Integer UPDATED_ITNS_PLANNED = 2;

    private static final Integer DEFAULT_TARGET_TYPE = 1;
    private static final Integer UPDATED_TARGET_TYPE = 2;

    private static final Double DEFAULT_LONGITUDE = 1D;
    private static final Double UPDATED_LONGITUDE = 2D;

    private static final Double DEFAULT_LATITUDE = 1D;
    private static final Double UPDATED_LATITUDE = 2D;

    private static final Instant DEFAULT_START_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_START_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/assignments";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Mock
    private AssignmentRepository assignmentRepositoryMock;

    @Mock
    private AssignmentService assignmentServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAssignmentMockMvc;

    private Assignment assignment;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Assignment createEntity(EntityManager em) {
        Assignment assignment = new Assignment()
            .uid(DEFAULT_UID)
            .code(DEFAULT_CODE)
            .phaseNo(DEFAULT_PHASE_NO)
            .districtCode(DEFAULT_DISTRICT_CODE)
            .gov(DEFAULT_GOV)
            .district(DEFAULT_DISTRICT)
            .subdistrict(DEFAULT_SUBDISTRICT)
            .village(DEFAULT_VILLAGE)
            .subvillage(DEFAULT_SUBVILLAGE)
            .name(DEFAULT_NAME)
            .dayId(DEFAULT_DAY_ID)
            .population(DEFAULT_POPULATION)
            .itnsPlanned(DEFAULT_ITNS_PLANNED)
            .targetType(DEFAULT_TARGET_TYPE)
            .longitude(DEFAULT_LONGITUDE)
            .latitude(DEFAULT_LATITUDE)
            .startDate(DEFAULT_START_DATE);
        // Add required entity
        Team team;
        if (TestUtil.findAll(em, Team.class).isEmpty()) {
            team = TeamResourceIT.createEntity(em);
            em.persist(team);
            em.flush();
        } else {
            team = TestUtil.findAll(em, Team.class).get(0);
        }
        assignment.setTeam(team);
        return assignment;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Assignment createUpdatedEntity(EntityManager em) {
        Assignment assignment = new Assignment()
            .uid(UPDATED_UID)
            .code(UPDATED_CODE)
            .phaseNo(UPDATED_PHASE_NO)
            .districtCode(UPDATED_DISTRICT_CODE)
            .gov(UPDATED_GOV)
            .district(UPDATED_DISTRICT)
            .subdistrict(UPDATED_SUBDISTRICT)
            .village(UPDATED_VILLAGE)
            .subvillage(UPDATED_SUBVILLAGE)
            .name(UPDATED_NAME)
            .dayId(UPDATED_DAY_ID)
            .population(UPDATED_POPULATION)
            .itnsPlanned(UPDATED_ITNS_PLANNED)
            .targetType(UPDATED_TARGET_TYPE)
            .longitude(UPDATED_LONGITUDE)
            .latitude(UPDATED_LATITUDE)
            .startDate(UPDATED_START_DATE);
        // Add required entity
        Team team;
        if (TestUtil.findAll(em, Team.class).isEmpty()) {
            team = TeamResourceIT.createUpdatedEntity(em);
            em.persist(team);
            em.flush();
        } else {
            team = TestUtil.findAll(em, Team.class).get(0);
        }
        assignment.setTeam(team);
        return assignment;
    }

    @BeforeEach
    public void initTest() {
        assignment = createEntity(em);
    }

    @Test
    @Transactional
    void createAssignment() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Assignment
        var returnedAssignment = om.readValue(
            restAssignmentMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(assignment)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            Assignment.class
        );

        // Validate the Assignment in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertAssignmentUpdatableFieldsEquals(returnedAssignment, getPersistedAssignment(returnedAssignment));
    }

    @Test
    @Transactional
    void createAssignmentWithExistingId() throws Exception {
        // Create the Assignment with an existing ID
        assignment.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAssignmentMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(assignment)))
            .andExpect(status().isBadRequest());

        // Validate the Assignment in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkUidIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        assignment.setUid(null);

        // Create the Assignment, which fails.

        restAssignmentMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(assignment)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllAssignments() throws Exception {
        // Initialize the database
        assignmentRepository.saveAndFlush(assignment);

        // Get all the assignmentList
        restAssignmentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(assignment.getId().intValue())))
            .andExpect(jsonPath("$.[*].uid").value(hasItem(DEFAULT_UID)))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].phaseNo").value(hasItem(DEFAULT_PHASE_NO)))
            .andExpect(jsonPath("$.[*].districtCode").value(hasItem(DEFAULT_DISTRICT_CODE)))
            .andExpect(jsonPath("$.[*].gov").value(hasItem(DEFAULT_GOV)))
            .andExpect(jsonPath("$.[*].district").value(hasItem(DEFAULT_DISTRICT)))
            .andExpect(jsonPath("$.[*].subdistrict").value(hasItem(DEFAULT_SUBDISTRICT)))
            .andExpect(jsonPath("$.[*].village").value(hasItem(DEFAULT_VILLAGE)))
            .andExpect(jsonPath("$.[*].subvillage").value(hasItem(DEFAULT_SUBVILLAGE)))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].dayId").value(hasItem(DEFAULT_DAY_ID)))
            .andExpect(jsonPath("$.[*].population").value(hasItem(DEFAULT_POPULATION.doubleValue())))
            .andExpect(jsonPath("$.[*].itnsPlanned").value(hasItem(DEFAULT_ITNS_PLANNED)))
            .andExpect(jsonPath("$.[*].targetType").value(hasItem(DEFAULT_TARGET_TYPE)))
            .andExpect(jsonPath("$.[*].longitude").value(hasItem(DEFAULT_LONGITUDE.doubleValue())))
            .andExpect(jsonPath("$.[*].latitude").value(hasItem(DEFAULT_LATITUDE.doubleValue())))
            .andExpect(jsonPath("$.[*].startDate").value(hasItem(DEFAULT_START_DATE.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAssignmentsWithEagerRelationshipsIsEnabled() throws Exception {
        when(assignmentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restAssignmentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(assignmentServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAssignmentsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(assignmentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restAssignmentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(assignmentRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getAssignment() throws Exception {
        // Initialize the database
        assignmentRepository.saveAndFlush(assignment);

        // Get the assignment
        restAssignmentMockMvc
            .perform(get(ENTITY_API_URL_ID, assignment.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(assignment.getId().intValue()))
            .andExpect(jsonPath("$.uid").value(DEFAULT_UID))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.phaseNo").value(DEFAULT_PHASE_NO))
            .andExpect(jsonPath("$.districtCode").value(DEFAULT_DISTRICT_CODE))
            .andExpect(jsonPath("$.gov").value(DEFAULT_GOV))
            .andExpect(jsonPath("$.district").value(DEFAULT_DISTRICT))
            .andExpect(jsonPath("$.subdistrict").value(DEFAULT_SUBDISTRICT))
            .andExpect(jsonPath("$.village").value(DEFAULT_VILLAGE))
            .andExpect(jsonPath("$.subvillage").value(DEFAULT_SUBVILLAGE))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.dayId").value(DEFAULT_DAY_ID))
            .andExpect(jsonPath("$.population").value(DEFAULT_POPULATION.doubleValue()))
            .andExpect(jsonPath("$.itnsPlanned").value(DEFAULT_ITNS_PLANNED))
            .andExpect(jsonPath("$.targetType").value(DEFAULT_TARGET_TYPE))
            .andExpect(jsonPath("$.longitude").value(DEFAULT_LONGITUDE.doubleValue()))
            .andExpect(jsonPath("$.latitude").value(DEFAULT_LATITUDE.doubleValue()))
            .andExpect(jsonPath("$.startDate").value(DEFAULT_START_DATE.toString()));
    }

    @Test
    @Transactional
    void getNonExistingAssignment() throws Exception {
        // Get the assignment
        restAssignmentMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingAssignment() throws Exception {
        // Initialize the database
        assignmentRepository.saveAndFlush(assignment);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the assignment
        Assignment updatedAssignment = assignmentRepository.findById(assignment.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedAssignment are not directly saved in db
        em.detach(updatedAssignment);
        updatedAssignment
            .uid(UPDATED_UID)
            .code(UPDATED_CODE)
            .phaseNo(UPDATED_PHASE_NO)
            .districtCode(UPDATED_DISTRICT_CODE)
            .gov(UPDATED_GOV)
            .district(UPDATED_DISTRICT)
            .subdistrict(UPDATED_SUBDISTRICT)
            .village(UPDATED_VILLAGE)
            .subvillage(UPDATED_SUBVILLAGE)
            .name(UPDATED_NAME)
            .dayId(UPDATED_DAY_ID)
            .population(UPDATED_POPULATION)
            .itnsPlanned(UPDATED_ITNS_PLANNED)
            .targetType(UPDATED_TARGET_TYPE)
            .longitude(UPDATED_LONGITUDE)
            .latitude(UPDATED_LATITUDE)
            .startDate(UPDATED_START_DATE);

        restAssignmentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedAssignment.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedAssignment))
            )
            .andExpect(status().isOk());

        // Validate the Assignment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedAssignmentToMatchAllProperties(updatedAssignment);
    }

    @Test
    @Transactional
    void putNonExistingAssignment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        assignment.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAssignmentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, assignment.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(assignment))
            )
            .andExpect(status().isBadRequest());

        // Validate the Assignment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchAssignment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        assignment.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAssignmentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(assignment))
            )
            .andExpect(status().isBadRequest());

        // Validate the Assignment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamAssignment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        assignment.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAssignmentMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(assignment)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Assignment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateAssignmentWithPatch() throws Exception {
        // Initialize the database
        assignmentRepository.saveAndFlush(assignment);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the assignment using partial update
        Assignment partialUpdatedAssignment = new Assignment();
        partialUpdatedAssignment.setId(assignment.getId());

        partialUpdatedAssignment
            .code(UPDATED_CODE)
            .phaseNo(UPDATED_PHASE_NO)
            .districtCode(UPDATED_DISTRICT_CODE)
            .gov(UPDATED_GOV)
            .district(UPDATED_DISTRICT)
            .subvillage(UPDATED_SUBVILLAGE)
            .name(UPDATED_NAME)
            .targetType(UPDATED_TARGET_TYPE)
            .latitude(UPDATED_LATITUDE)
            .startDate(UPDATED_START_DATE);

        restAssignmentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAssignment.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAssignment))
            )
            .andExpect(status().isOk());

        // Validate the Assignment in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAssignmentUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedAssignment, assignment),
            getPersistedAssignment(assignment)
        );
    }

    @Test
    @Transactional
    void fullUpdateAssignmentWithPatch() throws Exception {
        // Initialize the database
        assignmentRepository.saveAndFlush(assignment);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the assignment using partial update
        Assignment partialUpdatedAssignment = new Assignment();
        partialUpdatedAssignment.setId(assignment.getId());

        partialUpdatedAssignment
            .uid(UPDATED_UID)
            .code(UPDATED_CODE)
            .phaseNo(UPDATED_PHASE_NO)
            .districtCode(UPDATED_DISTRICT_CODE)
            .gov(UPDATED_GOV)
            .district(UPDATED_DISTRICT)
            .subdistrict(UPDATED_SUBDISTRICT)
            .village(UPDATED_VILLAGE)
            .subvillage(UPDATED_SUBVILLAGE)
            .name(UPDATED_NAME)
            .dayId(UPDATED_DAY_ID)
            .population(UPDATED_POPULATION)
            .itnsPlanned(UPDATED_ITNS_PLANNED)
            .targetType(UPDATED_TARGET_TYPE)
            .longitude(UPDATED_LONGITUDE)
            .latitude(UPDATED_LATITUDE)
            .startDate(UPDATED_START_DATE);

        restAssignmentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAssignment.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAssignment))
            )
            .andExpect(status().isOk());

        // Validate the Assignment in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAssignmentUpdatableFieldsEquals(partialUpdatedAssignment, getPersistedAssignment(partialUpdatedAssignment));
    }

    @Test
    @Transactional
    void patchNonExistingAssignment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        assignment.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAssignmentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, assignment.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(assignment))
            )
            .andExpect(status().isBadRequest());

        // Validate the Assignment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchAssignment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        assignment.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAssignmentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(assignment))
            )
            .andExpect(status().isBadRequest());

        // Validate the Assignment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamAssignment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        assignment.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAssignmentMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(assignment)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Assignment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteAssignment() throws Exception {
        // Initialize the database
        assignmentRepository.saveAndFlush(assignment);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the assignment
        restAssignmentMockMvc
            .perform(delete(ENTITY_API_URL_ID, assignment.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return assignmentRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected Assignment getPersistedAssignment(Assignment assignment) {
        return assignmentRepository.findById(assignment.getId()).orElseThrow();
    }

    protected void assertPersistedAssignmentToMatchAllProperties(Assignment expectedAssignment) {
        assertAssignmentAllPropertiesEquals(expectedAssignment, getPersistedAssignment(expectedAssignment));
    }

    protected void assertPersistedAssignmentToMatchUpdatableProperties(Assignment expectedAssignment) {
        assertAssignmentAllUpdatablePropertiesEquals(expectedAssignment, getPersistedAssignment(expectedAssignment));
    }
}
