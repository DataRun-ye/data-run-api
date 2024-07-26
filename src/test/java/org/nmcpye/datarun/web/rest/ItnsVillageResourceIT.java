package org.nmcpye.datarun.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.nmcpye.datarun.domain.ItnsVillageAsserts.*;
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
import org.nmcpye.datarun.domain.Assignment;
import org.nmcpye.datarun.domain.ItnsVillage;
import org.nmcpye.datarun.domain.Team;
import org.nmcpye.datarun.domain.enumeration.SettlementEnum;
import org.nmcpye.datarun.domain.enumeration.SurveyTypeEnum;
import org.nmcpye.datarun.domain.enumeration.SyncableStatus;
import org.nmcpye.datarun.repository.ItnsVillageRepository;
import org.nmcpye.datarun.service.ItnsVillageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link ItnsVillageResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ItnsVillageResourceIT {

    private static final String DEFAULT_UID = "AAAAAAAAAA";
    private static final String UPDATED_UID = "BBBBBBBBBB";

    private static final String DEFAULT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_SUBMISSION_UUID = "AAAAAAAAAA";
    private static final String UPDATED_SUBMISSION_UUID = "BBBBBBBBBB";

    private static final Long DEFAULT_SUBMISSION_ID = 1L;
    private static final Long UPDATED_SUBMISSION_ID = 2L;

    private static final Instant DEFAULT_WORK_DAY_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_WORK_DAY_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final SurveyTypeEnum DEFAULT_SURVEYTYPE = SurveyTypeEnum.ACTUALREPORT;
    private static final SurveyTypeEnum UPDATED_SURVEYTYPE = SurveyTypeEnum.TESTREPORT;

    private static final String DEFAULT_OTHER_REASON_COMMENT = "AAAAAAAAAA";
    private static final String UPDATED_OTHER_REASON_COMMENT = "BBBBBBBBBB";

    private static final String DEFAULT_REASON_NOTCOMPLETE = "AAAAAAAAAA";
    private static final String UPDATED_REASON_NOTCOMPLETE = "BBBBBBBBBB";

    private static final SettlementEnum DEFAULT_SETTLEMENT = SettlementEnum.RESIDENTS;
    private static final SettlementEnum UPDATED_SETTLEMENT = SettlementEnum.IDPSCAMP;

    private static final String DEFAULT_SETTLEMENT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_SETTLEMENT_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_TL_COMMENET = "AAAAAAAAAA";
    private static final String UPDATED_TL_COMMENET = "BBBBBBBBBB";

    private static final Integer DEFAULT_TIME_SPENT_HOURS = 1;
    private static final Integer UPDATED_TIME_SPENT_HOURS = 2;

    private static final Integer DEFAULT_TIME_SPENT_MINUTES = 1;
    private static final Integer UPDATED_TIME_SPENT_MINUTES = 2;

    private static final String DEFAULT_DIFFICULTIES = "AAAAAAAAAA";
    private static final String UPDATED_DIFFICULTIES = "BBBBBBBBBB";

    private static final String DEFAULT_LOCATION_CAPTURED = "AAAAAAAAAA";
    private static final String UPDATED_LOCATION_CAPTURED = "BBBBBBBBBB";

    private static final Instant DEFAULT_LOCATION_CAPTURE_TIME = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_LOCATION_CAPTURE_TIME = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_HO_PROOF = "AAAAAAAAAA";
    private static final String UPDATED_HO_PROOF = "BBBBBBBBBB";

    private static final String DEFAULT_HO_PROOF_URL = "AAAAAAAAAA";
    private static final String UPDATED_HO_PROOF_URL = "BBBBBBBBBB";

    private static final Instant DEFAULT_SUBMISSION_TIME = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_SUBMISSION_TIME = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_UNTARGETING_OTHER_SPECIFY = "AAAAAAAAAA";
    private static final String UPDATED_UNTARGETING_OTHER_SPECIFY = "BBBBBBBBBB";

    private static final String DEFAULT_OTHER_VILLAGE_NAME = "AAAAAAAAAA";
    private static final String UPDATED_OTHER_VILLAGE_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_OTHER_VILLAGE_CODE = "AAAAAAAAAA";
    private static final String UPDATED_OTHER_VILLAGE_CODE = "BBBBBBBBBB";

    private static final Long DEFAULT_OTHER_TEAM_NO = 1L;
    private static final Long UPDATED_OTHER_TEAM_NO = 2L;

    private static final Instant DEFAULT_START_ENTRY_TIME = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_START_ENTRY_TIME = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_FINISHED_ENTRY_TIME = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_FINISHED_ENTRY_TIME = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Boolean DEFAULT_DELETED = false;
    private static final Boolean UPDATED_DELETED = true;

    private static final SyncableStatus DEFAULT_STATUS = SyncableStatus.ACTIVE;
    private static final SyncableStatus UPDATED_STATUS = SyncableStatus.COMPLETED;

    private static final String ENTITY_API_URL = "/api/itns-villages";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ItnsVillageRepository itnsVillageRepository;

    @Mock
    private ItnsVillageRepository itnsVillageRepositoryMock;

    @Mock
    private ItnsVillageService itnsVillageServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restItnsVillageMockMvc;

    private ItnsVillage itnsVillage;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ItnsVillage createEntity(EntityManager em) {
        ItnsVillage itnsVillage = new ItnsVillage()
            .uid(DEFAULT_UID)
            .code(DEFAULT_CODE)
            .name(DEFAULT_NAME)
            .submissionUuid(DEFAULT_SUBMISSION_UUID)
            .submissionId(DEFAULT_SUBMISSION_ID)
            .workDayDate(DEFAULT_WORK_DAY_DATE)
            .surveytype(DEFAULT_SURVEYTYPE)
            .otherReasonComment(DEFAULT_OTHER_REASON_COMMENT)
            .reasonNotcomplete(DEFAULT_REASON_NOTCOMPLETE)
            .settlement(DEFAULT_SETTLEMENT)
            .settlementName(DEFAULT_SETTLEMENT_NAME)
            .tlCommenet(DEFAULT_TL_COMMENET)
            .timeSpentHours(DEFAULT_TIME_SPENT_HOURS)
            .timeSpentMinutes(DEFAULT_TIME_SPENT_MINUTES)
            .difficulties(DEFAULT_DIFFICULTIES)
            .locationCaptured(DEFAULT_LOCATION_CAPTURED)
            .locationCaptureTime(DEFAULT_LOCATION_CAPTURE_TIME)
            .hoProof(DEFAULT_HO_PROOF)
            .hoProofUrl(DEFAULT_HO_PROOF_URL)
            .submissionTime(DEFAULT_SUBMISSION_TIME)
            .untargetingOtherSpecify(DEFAULT_UNTARGETING_OTHER_SPECIFY)
            .otherVillageName(DEFAULT_OTHER_VILLAGE_NAME)
            .otherVillageCode(DEFAULT_OTHER_VILLAGE_CODE)
            .otherTeamNo(DEFAULT_OTHER_TEAM_NO)
            .startEntryTime(DEFAULT_START_ENTRY_TIME)
            .finishedEntryTime(DEFAULT_FINISHED_ENTRY_TIME)
            .deleted(DEFAULT_DELETED)
            .status(DEFAULT_STATUS);
        // Add required entity
        Team team;
        if (TestUtil.findAll(em, Team.class).isEmpty()) {
            team = TeamResourceIT.createEntity(em);
            em.persist(team);
            em.flush();
        } else {
            team = TestUtil.findAll(em, Team.class).get(0);
        }
        itnsVillage.setTeam(team);
        // Add required entity
        Assignment assignment;
        if (TestUtil.findAll(em, Assignment.class).isEmpty()) {
            assignment = AssignmentResourceIT.createEntity(em);
            em.persist(assignment);
            em.flush();
        } else {
            assignment = TestUtil.findAll(em, Assignment.class).get(0);
        }
        itnsVillage.setAssignment(assignment);
        // Add required entity
        Activity activity;
        if (TestUtil.findAll(em, Activity.class).isEmpty()) {
            activity = ActivityResourceIT.createEntity(em);
            em.persist(activity);
            em.flush();
        } else {
            activity = TestUtil.findAll(em, Activity.class).get(0);
        }
        itnsVillage.setActivity(activity);
        return itnsVillage;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ItnsVillage createUpdatedEntity(EntityManager em) {
        ItnsVillage itnsVillage = new ItnsVillage()
            .uid(UPDATED_UID)
            .code(UPDATED_CODE)
            .name(UPDATED_NAME)
            .submissionUuid(UPDATED_SUBMISSION_UUID)
            .submissionId(UPDATED_SUBMISSION_ID)
            .workDayDate(UPDATED_WORK_DAY_DATE)
            .surveytype(UPDATED_SURVEYTYPE)
            .otherReasonComment(UPDATED_OTHER_REASON_COMMENT)
            .reasonNotcomplete(UPDATED_REASON_NOTCOMPLETE)
            .settlement(UPDATED_SETTLEMENT)
            .settlementName(UPDATED_SETTLEMENT_NAME)
            .tlCommenet(UPDATED_TL_COMMENET)
            .timeSpentHours(UPDATED_TIME_SPENT_HOURS)
            .timeSpentMinutes(UPDATED_TIME_SPENT_MINUTES)
            .difficulties(UPDATED_DIFFICULTIES)
            .locationCaptured(UPDATED_LOCATION_CAPTURED)
            .locationCaptureTime(UPDATED_LOCATION_CAPTURE_TIME)
            .hoProof(UPDATED_HO_PROOF)
            .hoProofUrl(UPDATED_HO_PROOF_URL)
            .submissionTime(UPDATED_SUBMISSION_TIME)
            .untargetingOtherSpecify(UPDATED_UNTARGETING_OTHER_SPECIFY)
            .otherVillageName(UPDATED_OTHER_VILLAGE_NAME)
            .otherVillageCode(UPDATED_OTHER_VILLAGE_CODE)
            .otherTeamNo(UPDATED_OTHER_TEAM_NO)
            .startEntryTime(UPDATED_START_ENTRY_TIME)
            .finishedEntryTime(UPDATED_FINISHED_ENTRY_TIME)
            .deleted(UPDATED_DELETED)
            .status(UPDATED_STATUS);
        // Add required entity
        Team team;
        if (TestUtil.findAll(em, Team.class).isEmpty()) {
            team = TeamResourceIT.createUpdatedEntity(em);
            em.persist(team);
            em.flush();
        } else {
            team = TestUtil.findAll(em, Team.class).get(0);
        }
        itnsVillage.setTeam(team);
        // Add required entity
        Assignment assignment;
        if (TestUtil.findAll(em, Assignment.class).isEmpty()) {
            assignment = AssignmentResourceIT.createUpdatedEntity(em);
            em.persist(assignment);
            em.flush();
        } else {
            assignment = TestUtil.findAll(em, Assignment.class).get(0);
        }
        itnsVillage.setAssignment(assignment);
        // Add required entity
        Activity activity;
        if (TestUtil.findAll(em, Activity.class).isEmpty()) {
            activity = ActivityResourceIT.createUpdatedEntity(em);
            em.persist(activity);
            em.flush();
        } else {
            activity = TestUtil.findAll(em, Activity.class).get(0);
        }
        itnsVillage.setActivity(activity);
        return itnsVillage;
    }

    @BeforeEach
    public void initTest() {
        itnsVillage = createEntity(em);
    }

    @Test
    @Transactional
    void createItnsVillage() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ItnsVillage
        var returnedItnsVillage = om.readValue(
            restItnsVillageMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(itnsVillage)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ItnsVillage.class
        );

        // Validate the ItnsVillage in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertItnsVillageUpdatableFieldsEquals(returnedItnsVillage, getPersistedItnsVillage(returnedItnsVillage));
    }

    @Test
    @Transactional
    void createItnsVillageWithExistingId() throws Exception {
        // Create the ItnsVillage with an existing ID
        itnsVillage.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restItnsVillageMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(itnsVillage)))
            .andExpect(status().isBadRequest());

        // Validate the ItnsVillage in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkUidIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        itnsVillage.setUid(null);

        // Create the ItnsVillage, which fails.

        restItnsVillageMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(itnsVillage)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllItnsVillages() throws Exception {
        // Initialize the database
        itnsVillageRepository.saveAndFlush(itnsVillage);

        // Get all the itnsVillageList
        restItnsVillageMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(itnsVillage.getId().intValue())))
            .andExpect(jsonPath("$.[*].uid").value(hasItem(DEFAULT_UID)))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].submissionUuid").value(hasItem(DEFAULT_SUBMISSION_UUID)))
            .andExpect(jsonPath("$.[*].submissionId").value(hasItem(DEFAULT_SUBMISSION_ID.intValue())))
            .andExpect(jsonPath("$.[*].workDayDate").value(hasItem(DEFAULT_WORK_DAY_DATE.toString())))
            .andExpect(jsonPath("$.[*].surveytype").value(hasItem(DEFAULT_SURVEYTYPE.toString())))
            .andExpect(jsonPath("$.[*].otherReasonComment").value(hasItem(DEFAULT_OTHER_REASON_COMMENT)))
            .andExpect(jsonPath("$.[*].reasonNotcomplete").value(hasItem(DEFAULT_REASON_NOTCOMPLETE)))
            .andExpect(jsonPath("$.[*].settlement").value(hasItem(DEFAULT_SETTLEMENT.toString())))
            .andExpect(jsonPath("$.[*].settlementName").value(hasItem(DEFAULT_SETTLEMENT_NAME)))
            .andExpect(jsonPath("$.[*].tlCommenet").value(hasItem(DEFAULT_TL_COMMENET)))
            .andExpect(jsonPath("$.[*].timeSpentHours").value(hasItem(DEFAULT_TIME_SPENT_HOURS)))
            .andExpect(jsonPath("$.[*].timeSpentMinutes").value(hasItem(DEFAULT_TIME_SPENT_MINUTES)))
            .andExpect(jsonPath("$.[*].difficulties").value(hasItem(DEFAULT_DIFFICULTIES)))
            .andExpect(jsonPath("$.[*].locationCaptured").value(hasItem(DEFAULT_LOCATION_CAPTURED)))
            .andExpect(jsonPath("$.[*].locationCaptureTime").value(hasItem(DEFAULT_LOCATION_CAPTURE_TIME.toString())))
            .andExpect(jsonPath("$.[*].hoProof").value(hasItem(DEFAULT_HO_PROOF)))
            .andExpect(jsonPath("$.[*].hoProofUrl").value(hasItem(DEFAULT_HO_PROOF_URL)))
            .andExpect(jsonPath("$.[*].submissionTime").value(hasItem(DEFAULT_SUBMISSION_TIME.toString())))
            .andExpect(jsonPath("$.[*].untargetingOtherSpecify").value(hasItem(DEFAULT_UNTARGETING_OTHER_SPECIFY)))
            .andExpect(jsonPath("$.[*].otherVillageName").value(hasItem(DEFAULT_OTHER_VILLAGE_NAME)))
            .andExpect(jsonPath("$.[*].otherVillageCode").value(hasItem(DEFAULT_OTHER_VILLAGE_CODE)))
            .andExpect(jsonPath("$.[*].otherTeamNo").value(hasItem(DEFAULT_OTHER_TEAM_NO.intValue())))
            .andExpect(jsonPath("$.[*].startEntryTime").value(hasItem(DEFAULT_START_ENTRY_TIME.toString())))
            .andExpect(jsonPath("$.[*].finishedEntryTime").value(hasItem(DEFAULT_FINISHED_ENTRY_TIME.toString())))
            .andExpect(jsonPath("$.[*].deleted").value(hasItem(DEFAULT_DELETED.booleanValue())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllItnsVillagesWithEagerRelationshipsIsEnabled() throws Exception {
        when(itnsVillageServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restItnsVillageMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(itnsVillageServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllItnsVillagesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(itnsVillageServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restItnsVillageMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(itnsVillageRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getItnsVillage() throws Exception {
        // Initialize the database
        itnsVillageRepository.saveAndFlush(itnsVillage);

        // Get the itnsVillage
        restItnsVillageMockMvc
            .perform(get(ENTITY_API_URL_ID, itnsVillage.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(itnsVillage.getId().intValue()))
            .andExpect(jsonPath("$.uid").value(DEFAULT_UID))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.submissionUuid").value(DEFAULT_SUBMISSION_UUID))
            .andExpect(jsonPath("$.submissionId").value(DEFAULT_SUBMISSION_ID.intValue()))
            .andExpect(jsonPath("$.workDayDate").value(DEFAULT_WORK_DAY_DATE.toString()))
            .andExpect(jsonPath("$.surveytype").value(DEFAULT_SURVEYTYPE.toString()))
            .andExpect(jsonPath("$.otherReasonComment").value(DEFAULT_OTHER_REASON_COMMENT))
            .andExpect(jsonPath("$.reasonNotcomplete").value(DEFAULT_REASON_NOTCOMPLETE))
            .andExpect(jsonPath("$.settlement").value(DEFAULT_SETTLEMENT.toString()))
            .andExpect(jsonPath("$.settlementName").value(DEFAULT_SETTLEMENT_NAME))
            .andExpect(jsonPath("$.tlCommenet").value(DEFAULT_TL_COMMENET))
            .andExpect(jsonPath("$.timeSpentHours").value(DEFAULT_TIME_SPENT_HOURS))
            .andExpect(jsonPath("$.timeSpentMinutes").value(DEFAULT_TIME_SPENT_MINUTES))
            .andExpect(jsonPath("$.difficulties").value(DEFAULT_DIFFICULTIES))
            .andExpect(jsonPath("$.locationCaptured").value(DEFAULT_LOCATION_CAPTURED))
            .andExpect(jsonPath("$.locationCaptureTime").value(DEFAULT_LOCATION_CAPTURE_TIME.toString()))
            .andExpect(jsonPath("$.hoProof").value(DEFAULT_HO_PROOF))
            .andExpect(jsonPath("$.hoProofUrl").value(DEFAULT_HO_PROOF_URL))
            .andExpect(jsonPath("$.submissionTime").value(DEFAULT_SUBMISSION_TIME.toString()))
            .andExpect(jsonPath("$.untargetingOtherSpecify").value(DEFAULT_UNTARGETING_OTHER_SPECIFY))
            .andExpect(jsonPath("$.otherVillageName").value(DEFAULT_OTHER_VILLAGE_NAME))
            .andExpect(jsonPath("$.otherVillageCode").value(DEFAULT_OTHER_VILLAGE_CODE))
            .andExpect(jsonPath("$.otherTeamNo").value(DEFAULT_OTHER_TEAM_NO.intValue()))
            .andExpect(jsonPath("$.startEntryTime").value(DEFAULT_START_ENTRY_TIME.toString()))
            .andExpect(jsonPath("$.finishedEntryTime").value(DEFAULT_FINISHED_ENTRY_TIME.toString()))
            .andExpect(jsonPath("$.deleted").value(DEFAULT_DELETED.booleanValue()))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()));
    }

    @Test
    @Transactional
    void getNonExistingItnsVillage() throws Exception {
        // Get the itnsVillage
        restItnsVillageMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingItnsVillage() throws Exception {
        // Initialize the database
        itnsVillageRepository.saveAndFlush(itnsVillage);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the itnsVillage
        ItnsVillage updatedItnsVillage = itnsVillageRepository.findById(itnsVillage.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedItnsVillage are not directly saved in db
        em.detach(updatedItnsVillage);
        updatedItnsVillage
            .uid(UPDATED_UID)
            .code(UPDATED_CODE)
            .name(UPDATED_NAME)
            .submissionUuid(UPDATED_SUBMISSION_UUID)
            .submissionId(UPDATED_SUBMISSION_ID)
            .workDayDate(UPDATED_WORK_DAY_DATE)
            .surveytype(UPDATED_SURVEYTYPE)
            .otherReasonComment(UPDATED_OTHER_REASON_COMMENT)
            .reasonNotcomplete(UPDATED_REASON_NOTCOMPLETE)
            .settlement(UPDATED_SETTLEMENT)
            .settlementName(UPDATED_SETTLEMENT_NAME)
            .tlCommenet(UPDATED_TL_COMMENET)
            .timeSpentHours(UPDATED_TIME_SPENT_HOURS)
            .timeSpentMinutes(UPDATED_TIME_SPENT_MINUTES)
            .difficulties(UPDATED_DIFFICULTIES)
            .locationCaptured(UPDATED_LOCATION_CAPTURED)
            .locationCaptureTime(UPDATED_LOCATION_CAPTURE_TIME)
            .hoProof(UPDATED_HO_PROOF)
            .hoProofUrl(UPDATED_HO_PROOF_URL)
            .submissionTime(UPDATED_SUBMISSION_TIME)
            .untargetingOtherSpecify(UPDATED_UNTARGETING_OTHER_SPECIFY)
            .otherVillageName(UPDATED_OTHER_VILLAGE_NAME)
            .otherVillageCode(UPDATED_OTHER_VILLAGE_CODE)
            .otherTeamNo(UPDATED_OTHER_TEAM_NO)
            .startEntryTime(UPDATED_START_ENTRY_TIME)
            .finishedEntryTime(UPDATED_FINISHED_ENTRY_TIME)
            .deleted(UPDATED_DELETED)
            .status(UPDATED_STATUS);

        restItnsVillageMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedItnsVillage.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedItnsVillage))
            )
            .andExpect(status().isOk());

        // Validate the ItnsVillage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedItnsVillageToMatchAllProperties(updatedItnsVillage);
    }

    @Test
    @Transactional
    void putNonExistingItnsVillage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        itnsVillage.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restItnsVillageMockMvc
            .perform(
                put(ENTITY_API_URL_ID, itnsVillage.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(itnsVillage))
            )
            .andExpect(status().isBadRequest());

        // Validate the ItnsVillage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchItnsVillage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        itnsVillage.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restItnsVillageMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(itnsVillage))
            )
            .andExpect(status().isBadRequest());

        // Validate the ItnsVillage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamItnsVillage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        itnsVillage.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restItnsVillageMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(itnsVillage)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ItnsVillage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateItnsVillageWithPatch() throws Exception {
        // Initialize the database
        itnsVillageRepository.saveAndFlush(itnsVillage);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the itnsVillage using partial update
        ItnsVillage partialUpdatedItnsVillage = new ItnsVillage();
        partialUpdatedItnsVillage.setId(itnsVillage.getId());

        partialUpdatedItnsVillage
            .uid(UPDATED_UID)
            .workDayDate(UPDATED_WORK_DAY_DATE)
            .surveytype(UPDATED_SURVEYTYPE)
            .otherReasonComment(UPDATED_OTHER_REASON_COMMENT)
            .reasonNotcomplete(UPDATED_REASON_NOTCOMPLETE)
            .settlement(UPDATED_SETTLEMENT)
            .tlCommenet(UPDATED_TL_COMMENET)
            .timeSpentMinutes(UPDATED_TIME_SPENT_MINUTES)
            .locationCaptureTime(UPDATED_LOCATION_CAPTURE_TIME)
            .hoProof(UPDATED_HO_PROOF)
            .hoProofUrl(UPDATED_HO_PROOF_URL)
            .submissionTime(UPDATED_SUBMISSION_TIME)
            .untargetingOtherSpecify(UPDATED_UNTARGETING_OTHER_SPECIFY)
            .otherVillageCode(UPDATED_OTHER_VILLAGE_CODE);

        restItnsVillageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedItnsVillage.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedItnsVillage))
            )
            .andExpect(status().isOk());

        // Validate the ItnsVillage in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertItnsVillageUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedItnsVillage, itnsVillage),
            getPersistedItnsVillage(itnsVillage)
        );
    }

    @Test
    @Transactional
    void fullUpdateItnsVillageWithPatch() throws Exception {
        // Initialize the database
        itnsVillageRepository.saveAndFlush(itnsVillage);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the itnsVillage using partial update
        ItnsVillage partialUpdatedItnsVillage = new ItnsVillage();
        partialUpdatedItnsVillage.setId(itnsVillage.getId());

        partialUpdatedItnsVillage
            .uid(UPDATED_UID)
            .code(UPDATED_CODE)
            .name(UPDATED_NAME)
            .submissionUuid(UPDATED_SUBMISSION_UUID)
            .submissionId(UPDATED_SUBMISSION_ID)
            .workDayDate(UPDATED_WORK_DAY_DATE)
            .surveytype(UPDATED_SURVEYTYPE)
            .otherReasonComment(UPDATED_OTHER_REASON_COMMENT)
            .reasonNotcomplete(UPDATED_REASON_NOTCOMPLETE)
            .settlement(UPDATED_SETTLEMENT)
            .settlementName(UPDATED_SETTLEMENT_NAME)
            .tlCommenet(UPDATED_TL_COMMENET)
            .timeSpentHours(UPDATED_TIME_SPENT_HOURS)
            .timeSpentMinutes(UPDATED_TIME_SPENT_MINUTES)
            .difficulties(UPDATED_DIFFICULTIES)
            .locationCaptured(UPDATED_LOCATION_CAPTURED)
            .locationCaptureTime(UPDATED_LOCATION_CAPTURE_TIME)
            .hoProof(UPDATED_HO_PROOF)
            .hoProofUrl(UPDATED_HO_PROOF_URL)
            .submissionTime(UPDATED_SUBMISSION_TIME)
            .untargetingOtherSpecify(UPDATED_UNTARGETING_OTHER_SPECIFY)
            .otherVillageName(UPDATED_OTHER_VILLAGE_NAME)
            .otherVillageCode(UPDATED_OTHER_VILLAGE_CODE)
            .otherTeamNo(UPDATED_OTHER_TEAM_NO)
            .startEntryTime(UPDATED_START_ENTRY_TIME)
            .finishedEntryTime(UPDATED_FINISHED_ENTRY_TIME)
            .deleted(UPDATED_DELETED)
            .status(UPDATED_STATUS);

        restItnsVillageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedItnsVillage.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedItnsVillage))
            )
            .andExpect(status().isOk());

        // Validate the ItnsVillage in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertItnsVillageUpdatableFieldsEquals(partialUpdatedItnsVillage, getPersistedItnsVillage(partialUpdatedItnsVillage));
    }

    @Test
    @Transactional
    void patchNonExistingItnsVillage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        itnsVillage.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restItnsVillageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, itnsVillage.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(itnsVillage))
            )
            .andExpect(status().isBadRequest());

        // Validate the ItnsVillage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchItnsVillage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        itnsVillage.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restItnsVillageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(itnsVillage))
            )
            .andExpect(status().isBadRequest());

        // Validate the ItnsVillage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamItnsVillage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        itnsVillage.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restItnsVillageMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(itnsVillage)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ItnsVillage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteItnsVillage() throws Exception {
        // Initialize the database
        itnsVillageRepository.saveAndFlush(itnsVillage);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the itnsVillage
        restItnsVillageMockMvc
            .perform(delete(ENTITY_API_URL_ID, itnsVillage.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return itnsVillageRepository.count();
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

    protected ItnsVillage getPersistedItnsVillage(ItnsVillage itnsVillage) {
        return itnsVillageRepository.findById(itnsVillage.getId()).orElseThrow();
    }

    protected void assertPersistedItnsVillageToMatchAllProperties(ItnsVillage expectedItnsVillage) {
        assertItnsVillageAllPropertiesEquals(expectedItnsVillage, getPersistedItnsVillage(expectedItnsVillage));
    }

    protected void assertPersistedItnsVillageToMatchUpdatableProperties(ItnsVillage expectedItnsVillage) {
        assertItnsVillageAllUpdatablePropertiesEquals(expectedItnsVillage, getPersistedItnsVillage(expectedItnsVillage));
    }
}
