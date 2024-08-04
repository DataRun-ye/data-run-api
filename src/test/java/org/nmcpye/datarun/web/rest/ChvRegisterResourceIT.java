package org.nmcpye.datarun.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.nmcpye.datarun.domain.ChvRegisterAsserts.*;
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
import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.domain.ChvRegister;
import org.nmcpye.datarun.domain.Team;
import org.nmcpye.datarun.domain.enumeration.Gender;
import org.nmcpye.datarun.domain.enumeration.MDetectionType;
import org.nmcpye.datarun.domain.enumeration.MSeverity;
import org.nmcpye.datarun.domain.enumeration.MTestResult;
import org.nmcpye.datarun.domain.enumeration.MTreatment;
import org.nmcpye.datarun.domain.enumeration.SyncableStatus;
import org.nmcpye.datarun.repository.ChvRegisterRepository;
import org.nmcpye.datarun.service.ChvRegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link ChvRegisterResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ChvRegisterResourceIT {

    private static final String DEFAULT_UID = "AAAAAAAAAA";
    private static final String UPDATED_UID = "BBBBBBBBBB";

    private static final String DEFAULT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_LOCATION_NAME = "AAAAAAAAAA";
    private static final String UPDATED_LOCATION_NAME = "BBBBBBBBBB";

    private static final Integer DEFAULT_AGE = 1;
    private static final Integer UPDATED_AGE = 2;

    private static final Gender DEFAULT_GENDER = Gender.MALE;
    private static final Gender UPDATED_GENDER = Gender.FEMALE;

    private static final Instant DEFAULT_VISIT_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_VISIT_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Boolean DEFAULT_PREGNANT = false;
    private static final Boolean UPDATED_PREGNANT = true;

    private static final MTestResult DEFAULT_TEST_RESULT = MTestResult.NEGATIVE;
    private static final MTestResult UPDATED_TEST_RESULT = MTestResult.PF;

    private static final MDetectionType DEFAULT_DETECTION_TYPE = MDetectionType.REACTIVE;
    private static final MDetectionType UPDATED_DETECTION_TYPE = MDetectionType.ACTIVE;

    private static final MSeverity DEFAULT_SEVERITY = MSeverity.SIMPLE;
    private static final MSeverity UPDATED_SEVERITY = MSeverity.SEVERE;

    private static final MTreatment DEFAULT_TREATMENT = MTreatment.TREATED;
    private static final MTreatment UPDATED_TREATMENT = MTreatment.FIRST_DOSE;

    private static final Boolean DEFAULT_DELETED = false;
    private static final Boolean UPDATED_DELETED = true;

    private static final Instant DEFAULT_START_ENTRY_TIME = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_START_ENTRY_TIME = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_FINISHED_ENTRY_TIME = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_FINISHED_ENTRY_TIME = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_COMMENT = "AAAAAAAAAA";
    private static final String UPDATED_COMMENT = "BBBBBBBBBB";

    private static final SyncableStatus DEFAULT_STATUS = SyncableStatus.ACTIVE;
    private static final SyncableStatus UPDATED_STATUS = SyncableStatus.COMPLETED;

    private static final String ENTITY_API_URL = "/api/chv-registers";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ChvRegisterRepository chvRegisterRepository;

    @Mock
    private ChvRegisterRepository chvRegisterRepositoryMock;

    @Mock
    private ChvRegisterService chvRegisterServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restChvRegisterMockMvc;

    private ChvRegister chvRegister;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ChvRegister createEntity(EntityManager em) {
        ChvRegister chvRegister = new ChvRegister()
            .uid(DEFAULT_UID)
            .code(DEFAULT_CODE)
            .name(DEFAULT_NAME)
            .locationName(DEFAULT_LOCATION_NAME)
            .age(DEFAULT_AGE)
            .gender(DEFAULT_GENDER)
            .visitDate(DEFAULT_VISIT_DATE)
            .pregnant(DEFAULT_PREGNANT)
            .testResult(DEFAULT_TEST_RESULT)
            .detectionType(DEFAULT_DETECTION_TYPE)
            .severity(DEFAULT_SEVERITY)
            .treatment(DEFAULT_TREATMENT)
            .deleted(DEFAULT_DELETED)
            .startEntryTime(DEFAULT_START_ENTRY_TIME)
            .finishedEntryTime(DEFAULT_FINISHED_ENTRY_TIME)
            .comment(DEFAULT_COMMENT)
            .status(DEFAULT_STATUS);
        // Add required entity
        Activity activity;
        if (TestUtil.findAll(em, Activity.class).isEmpty()) {
            activity = ActivityResourceIT.createEntity(em);
            em.persist(activity);
            em.flush();
        } else {
            activity = TestUtil.findAll(em, Activity.class).get(0);
        }
        chvRegister.setActivity(activity);
        // Add required entity
        Team team;
        if (TestUtil.findAll(em, Team.class).isEmpty()) {
            team = TeamResourceIT.createEntity(em);
            em.persist(team);
            em.flush();
        } else {
            team = TestUtil.findAll(em, Team.class).get(0);
        }
        chvRegister.setTeam(team);
        return chvRegister;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ChvRegister createUpdatedEntity(EntityManager em) {
        ChvRegister chvRegister = new ChvRegister()
            .uid(UPDATED_UID)
            .code(UPDATED_CODE)
            .name(UPDATED_NAME)
            .locationName(UPDATED_LOCATION_NAME)
            .age(UPDATED_AGE)
            .gender(UPDATED_GENDER)
            .visitDate(UPDATED_VISIT_DATE)
            .pregnant(UPDATED_PREGNANT)
            .testResult(UPDATED_TEST_RESULT)
            .detectionType(UPDATED_DETECTION_TYPE)
            .severity(UPDATED_SEVERITY)
            .treatment(UPDATED_TREATMENT)
            .deleted(UPDATED_DELETED)
            .startEntryTime(UPDATED_START_ENTRY_TIME)
            .finishedEntryTime(UPDATED_FINISHED_ENTRY_TIME)
            .comment(UPDATED_COMMENT)
            .status(UPDATED_STATUS);
        // Add required entity
        Activity activity;
        if (TestUtil.findAll(em, Activity.class).isEmpty()) {
            activity = ActivityResourceIT.createUpdatedEntity(em);
            em.persist(activity);
            em.flush();
        } else {
            activity = TestUtil.findAll(em, Activity.class).get(0);
        }
        chvRegister.setActivity(activity);
        // Add required entity
        Team team;
        if (TestUtil.findAll(em, Team.class).isEmpty()) {
            team = TeamResourceIT.createUpdatedEntity(em);
            em.persist(team);
            em.flush();
        } else {
            team = TestUtil.findAll(em, Team.class).get(0);
        }
        chvRegister.setTeam(team);
        return chvRegister;
    }

    @BeforeEach
    public void initTest() {
        chvRegister = createEntity(em);
    }

    @Test
    @Transactional
    void createChvRegister() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ChvRegister
        var returnedChvRegister = om.readValue(
            restChvRegisterMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(chvRegister)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ChvRegister.class
        );

        // Validate the ChvRegister in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertChvRegisterUpdatableFieldsEquals(returnedChvRegister, getPersistedChvRegister(returnedChvRegister));
    }

    @Test
    @Transactional
    void createChvRegisterWithExistingId() throws Exception {
        // Create the ChvRegister with an existing ID
        chvRegister.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restChvRegisterMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(chvRegister)))
            .andExpect(status().isBadRequest());

        // Validate the ChvRegister in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkUidIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        chvRegister.setUid(null);

        // Create the ChvRegister, which fails.

        restChvRegisterMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(chvRegister)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllChvRegisters() throws Exception {
        // Initialize the database
        chvRegisterRepository.saveAndFlush(chvRegister);

        // Get all the chvRegisterList
        restChvRegisterMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(chvRegister.getId().intValue())))
            .andExpect(jsonPath("$.[*].uid").value(hasItem(DEFAULT_UID)))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].locationName").value(hasItem(DEFAULT_LOCATION_NAME)))
            .andExpect(jsonPath("$.[*].age").value(hasItem(DEFAULT_AGE)))
            .andExpect(jsonPath("$.[*].gender").value(hasItem(DEFAULT_GENDER.toString())))
            .andExpect(jsonPath("$.[*].visitDate").value(hasItem(DEFAULT_VISIT_DATE.toString())))
            .andExpect(jsonPath("$.[*].pregnant").value(hasItem(DEFAULT_PREGNANT.booleanValue())))
            .andExpect(jsonPath("$.[*].testResult").value(hasItem(DEFAULT_TEST_RESULT.toString())))
            .andExpect(jsonPath("$.[*].detectionType").value(hasItem(DEFAULT_DETECTION_TYPE.toString())))
            .andExpect(jsonPath("$.[*].severity").value(hasItem(DEFAULT_SEVERITY.toString())))
            .andExpect(jsonPath("$.[*].treatment").value(hasItem(DEFAULT_TREATMENT.toString())))
            .andExpect(jsonPath("$.[*].deleted").value(hasItem(DEFAULT_DELETED.booleanValue())))
            .andExpect(jsonPath("$.[*].startEntryTime").value(hasItem(DEFAULT_START_ENTRY_TIME.toString())))
            .andExpect(jsonPath("$.[*].finishedEntryTime").value(hasItem(DEFAULT_FINISHED_ENTRY_TIME.toString())))
            .andExpect(jsonPath("$.[*].comment").value(hasItem(DEFAULT_COMMENT)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllChvRegistersWithEagerRelationshipsIsEnabled() throws Exception {
        when(chvRegisterServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restChvRegisterMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(chvRegisterServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllChvRegistersWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(chvRegisterServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restChvRegisterMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(chvRegisterRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getChvRegister() throws Exception {
        // Initialize the database
        chvRegisterRepository.saveAndFlush(chvRegister);

        // Get the chvRegister
        restChvRegisterMockMvc
            .perform(get(ENTITY_API_URL_ID, chvRegister.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(chvRegister.getId().intValue()))
            .andExpect(jsonPath("$.uid").value(DEFAULT_UID))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.locationName").value(DEFAULT_LOCATION_NAME))
            .andExpect(jsonPath("$.age").value(DEFAULT_AGE))
            .andExpect(jsonPath("$.gender").value(DEFAULT_GENDER.toString()))
            .andExpect(jsonPath("$.visitDate").value(DEFAULT_VISIT_DATE.toString()))
            .andExpect(jsonPath("$.pregnant").value(DEFAULT_PREGNANT.booleanValue()))
            .andExpect(jsonPath("$.testResult").value(DEFAULT_TEST_RESULT.toString()))
            .andExpect(jsonPath("$.detectionType").value(DEFAULT_DETECTION_TYPE.toString()))
            .andExpect(jsonPath("$.severity").value(DEFAULT_SEVERITY.toString()))
            .andExpect(jsonPath("$.treatment").value(DEFAULT_TREATMENT.toString()))
            .andExpect(jsonPath("$.deleted").value(DEFAULT_DELETED.booleanValue()))
            .andExpect(jsonPath("$.startEntryTime").value(DEFAULT_START_ENTRY_TIME.toString()))
            .andExpect(jsonPath("$.finishedEntryTime").value(DEFAULT_FINISHED_ENTRY_TIME.toString()))
            .andExpect(jsonPath("$.comment").value(DEFAULT_COMMENT))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()));
    }

    @Test
    @Transactional
    void getNonExistingChvRegister() throws Exception {
        // Get the chvRegister
        restChvRegisterMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingChvRegister() throws Exception {
        // Initialize the database
        chvRegisterRepository.saveAndFlush(chvRegister);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the chvRegister
        ChvRegister updatedChvRegister = chvRegisterRepository.findById(chvRegister.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedChvRegister are not directly saved in db
        em.detach(updatedChvRegister);
        updatedChvRegister
            .uid(UPDATED_UID)
            .code(UPDATED_CODE)
            .name(UPDATED_NAME)
            .locationName(UPDATED_LOCATION_NAME)
            .age(UPDATED_AGE)
            .gender(UPDATED_GENDER)
            .visitDate(UPDATED_VISIT_DATE)
            .pregnant(UPDATED_PREGNANT)
            .testResult(UPDATED_TEST_RESULT)
            .detectionType(UPDATED_DETECTION_TYPE)
            .severity(UPDATED_SEVERITY)
            .treatment(UPDATED_TREATMENT)
            .deleted(UPDATED_DELETED)
            .startEntryTime(UPDATED_START_ENTRY_TIME)
            .finishedEntryTime(UPDATED_FINISHED_ENTRY_TIME)
            .comment(UPDATED_COMMENT)
            .status(UPDATED_STATUS);

        restChvRegisterMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedChvRegister.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedChvRegister))
            )
            .andExpect(status().isOk());

        // Validate the ChvRegister in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedChvRegisterToMatchAllProperties(updatedChvRegister);
    }

    @Test
    @Transactional
    void putNonExistingChvRegister() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        chvRegister.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restChvRegisterMockMvc
            .perform(
                put(ENTITY_API_URL_ID, chvRegister.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(chvRegister))
            )
            .andExpect(status().isBadRequest());

        // Validate the ChvRegister in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchChvRegister() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        chvRegister.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restChvRegisterMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(chvRegister))
            )
            .andExpect(status().isBadRequest());

        // Validate the ChvRegister in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamChvRegister() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        chvRegister.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restChvRegisterMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(chvRegister)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ChvRegister in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateChvRegisterWithPatch() throws Exception {
        // Initialize the database
        chvRegisterRepository.saveAndFlush(chvRegister);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the chvRegister using partial update
        ChvRegister partialUpdatedChvRegister = new ChvRegister();
        partialUpdatedChvRegister.setId(chvRegister.getId());

        partialUpdatedChvRegister
            .uid(UPDATED_UID)
            .locationName(UPDATED_LOCATION_NAME)
            .age(UPDATED_AGE)
            .gender(UPDATED_GENDER)
            .visitDate(UPDATED_VISIT_DATE)
            .pregnant(UPDATED_PREGNANT)
            .severity(UPDATED_SEVERITY)
            .deleted(UPDATED_DELETED)
            .startEntryTime(UPDATED_START_ENTRY_TIME)
            .finishedEntryTime(UPDATED_FINISHED_ENTRY_TIME)
            .comment(UPDATED_COMMENT);

        restChvRegisterMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedChvRegister.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedChvRegister))
            )
            .andExpect(status().isOk());

        // Validate the ChvRegister in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertChvRegisterUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedChvRegister, chvRegister),
            getPersistedChvRegister(chvRegister)
        );
    }

    @Test
    @Transactional
    void fullUpdateChvRegisterWithPatch() throws Exception {
        // Initialize the database
        chvRegisterRepository.saveAndFlush(chvRegister);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the chvRegister using partial update
        ChvRegister partialUpdatedChvRegister = new ChvRegister();
        partialUpdatedChvRegister.setId(chvRegister.getId());

        partialUpdatedChvRegister
            .uid(UPDATED_UID)
            .code(UPDATED_CODE)
            .name(UPDATED_NAME)
            .locationName(UPDATED_LOCATION_NAME)
            .age(UPDATED_AGE)
            .gender(UPDATED_GENDER)
            .visitDate(UPDATED_VISIT_DATE)
            .pregnant(UPDATED_PREGNANT)
            .testResult(UPDATED_TEST_RESULT)
            .detectionType(UPDATED_DETECTION_TYPE)
            .severity(UPDATED_SEVERITY)
            .treatment(UPDATED_TREATMENT)
            .deleted(UPDATED_DELETED)
            .startEntryTime(UPDATED_START_ENTRY_TIME)
            .finishedEntryTime(UPDATED_FINISHED_ENTRY_TIME)
            .comment(UPDATED_COMMENT)
            .status(UPDATED_STATUS);

        restChvRegisterMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedChvRegister.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedChvRegister))
            )
            .andExpect(status().isOk());

        // Validate the ChvRegister in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertChvRegisterUpdatableFieldsEquals(partialUpdatedChvRegister, getPersistedChvRegister(partialUpdatedChvRegister));
    }

    @Test
    @Transactional
    void patchNonExistingChvRegister() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        chvRegister.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restChvRegisterMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, chvRegister.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(chvRegister))
            )
            .andExpect(status().isBadRequest());

        // Validate the ChvRegister in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchChvRegister() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        chvRegister.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restChvRegisterMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(chvRegister))
            )
            .andExpect(status().isBadRequest());

        // Validate the ChvRegister in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamChvRegister() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        chvRegister.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restChvRegisterMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(chvRegister)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ChvRegister in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteChvRegister() throws Exception {
        // Initialize the database
        chvRegisterRepository.saveAndFlush(chvRegister);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the chvRegister
        restChvRegisterMockMvc
            .perform(delete(ENTITY_API_URL_ID, chvRegister.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return chvRegisterRepository.count();
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

    protected ChvRegister getPersistedChvRegister(ChvRegister chvRegister) {
        return chvRegisterRepository.findById(chvRegister.getId()).orElseThrow();
    }

    protected void assertPersistedChvRegisterToMatchAllProperties(ChvRegister expectedChvRegister) {
        assertChvRegisterAllPropertiesEquals(expectedChvRegister, getPersistedChvRegister(expectedChvRegister));
    }

    protected void assertPersistedChvRegisterToMatchUpdatableProperties(ChvRegister expectedChvRegister) {
        assertChvRegisterAllUpdatablePropertiesEquals(expectedChvRegister, getPersistedChvRegister(expectedChvRegister));
    }
}
