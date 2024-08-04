package org.nmcpye.datarun.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.nmcpye.datarun.domain.ChvSessionAsserts.*;
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
import org.nmcpye.datarun.domain.ChvSession;
import org.nmcpye.datarun.domain.Team;
import org.nmcpye.datarun.domain.enumeration.MSessionSubject;
import org.nmcpye.datarun.domain.enumeration.SyncableStatus;
import org.nmcpye.datarun.repository.ChvSessionRepository;
import org.nmcpye.datarun.service.ChvSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link ChvSessionResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ChvSessionResourceIT {

    private static final String DEFAULT_UID = "AAAAAAAAAA";
    private static final String UPDATED_UID = "BBBBBBBBBB";

    private static final String DEFAULT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final Instant DEFAULT_SESSION_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_SESSION_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final MSessionSubject DEFAULT_SUBJECT = MSessionSubject.ITNs;
    private static final MSessionSubject UPDATED_SUBJECT = MSessionSubject.BreadingSite;

    private static final Integer DEFAULT_SESSIONS = 1;
    private static final Integer UPDATED_SESSIONS = 2;

    private static final Integer DEFAULT_PEOPLE = 1;
    private static final Integer UPDATED_PEOPLE = 2;

    private static final String DEFAULT_COMMENT = "AAAAAAAAAA";
    private static final String UPDATED_COMMENT = "BBBBBBBBBB";

    private static final Boolean DEFAULT_DELETED = false;
    private static final Boolean UPDATED_DELETED = true;

    private static final Instant DEFAULT_START_ENTRY_TIME = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_START_ENTRY_TIME = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_FINISHED_ENTRY_TIME = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_FINISHED_ENTRY_TIME = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final SyncableStatus DEFAULT_STATUS = SyncableStatus.ACTIVE;
    private static final SyncableStatus UPDATED_STATUS = SyncableStatus.COMPLETED;

    private static final String ENTITY_API_URL = "/api/chv-sessions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ChvSessionRepository chvSessionRepository;

    @Mock
    private ChvSessionRepository chvSessionRepositoryMock;

    @Mock
    private ChvSessionService chvSessionServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restChvSessionMockMvc;

    private ChvSession chvSession;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ChvSession createEntity(EntityManager em) {
        ChvSession chvSession = new ChvSession()
            .uid(DEFAULT_UID)
            .code(DEFAULT_CODE)
            .name(DEFAULT_NAME)
            .sessionDate(DEFAULT_SESSION_DATE)
            .subject(DEFAULT_SUBJECT)
            .sessions(DEFAULT_SESSIONS)
            .peopleItns(DEFAULT_PEOPLE)
            .comment(DEFAULT_COMMENT)
            .deleted(DEFAULT_DELETED)
            .startEntryTime(DEFAULT_START_ENTRY_TIME)
            .finishedEntryTime(DEFAULT_FINISHED_ENTRY_TIME)
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
        chvSession.setTeam(team);
        // Add required entity
        Activity activity;
        if (TestUtil.findAll(em, Activity.class).isEmpty()) {
            activity = ActivityResourceIT.createEntity(em);
            em.persist(activity);
            em.flush();
        } else {
            activity = TestUtil.findAll(em, Activity.class).get(0);
        }
        chvSession.setActivity(activity);
        return chvSession;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ChvSession createUpdatedEntity(EntityManager em) {
        ChvSession chvSession = new ChvSession()
            .uid(UPDATED_UID)
            .code(UPDATED_CODE)
            .name(UPDATED_NAME)
            .sessionDate(UPDATED_SESSION_DATE)
            .subject(UPDATED_SUBJECT)
            .sessions(UPDATED_SESSIONS)
            .peopleItns(UPDATED_PEOPLE)
            .comment(UPDATED_COMMENT)
            .deleted(UPDATED_DELETED)
            .startEntryTime(UPDATED_START_ENTRY_TIME)
            .finishedEntryTime(UPDATED_FINISHED_ENTRY_TIME)
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
        chvSession.setTeam(team);
        // Add required entity
        Activity activity;
        if (TestUtil.findAll(em, Activity.class).isEmpty()) {
            activity = ActivityResourceIT.createUpdatedEntity(em);
            em.persist(activity);
            em.flush();
        } else {
            activity = TestUtil.findAll(em, Activity.class).get(0);
        }
        chvSession.setActivity(activity);
        return chvSession;
    }

    @BeforeEach
    public void initTest() {
        chvSession = createEntity(em);
    }

    @Test
    @Transactional
    void createChvSession() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ChvSession
        var returnedChvSession = om.readValue(
            restChvSessionMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(chvSession)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ChvSession.class
        );

        // Validate the ChvSession in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertChvSessionUpdatableFieldsEquals(returnedChvSession, getPersistedChvSession(returnedChvSession));
    }

    @Test
    @Transactional
    void createChvSessionWithExistingId() throws Exception {
        // Create the ChvSession with an existing ID
        chvSession.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restChvSessionMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(chvSession)))
            .andExpect(status().isBadRequest());

        // Validate the ChvSession in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkUidIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        chvSession.setUid(null);

        // Create the ChvSession, which fails.

        restChvSessionMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(chvSession)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        chvSession.setStatus(null);

        // Create the ChvSession, which fails.

        restChvSessionMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(chvSession)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllChvSessions() throws Exception {
        // Initialize the database
        chvSessionRepository.saveAndFlush(chvSession);

        // Get all the chvSessionList
        restChvSessionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(chvSession.getId().intValue())))
            .andExpect(jsonPath("$.[*].uid").value(hasItem(DEFAULT_UID)))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].sessionDate").value(hasItem(DEFAULT_SESSION_DATE.toString())))
            .andExpect(jsonPath("$.[*].subject").value(hasItem(DEFAULT_SUBJECT.toString())))
            .andExpect(jsonPath("$.[*].sessions").value(hasItem(DEFAULT_SESSIONS)))
            .andExpect(jsonPath("$.[*].people").value(hasItem(DEFAULT_PEOPLE)))
            .andExpect(jsonPath("$.[*].comment").value(hasItem(DEFAULT_COMMENT)))
            .andExpect(jsonPath("$.[*].deleted").value(hasItem(DEFAULT_DELETED.booleanValue())))
            .andExpect(jsonPath("$.[*].startEntryTime").value(hasItem(DEFAULT_START_ENTRY_TIME.toString())))
            .andExpect(jsonPath("$.[*].finishedEntryTime").value(hasItem(DEFAULT_FINISHED_ENTRY_TIME.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllChvSessionsWithEagerRelationshipsIsEnabled() throws Exception {
        when(chvSessionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restChvSessionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(chvSessionServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllChvSessionsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(chvSessionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restChvSessionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(chvSessionRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getChvSession() throws Exception {
        // Initialize the database
        chvSessionRepository.saveAndFlush(chvSession);

        // Get the chvSession
        restChvSessionMockMvc
            .perform(get(ENTITY_API_URL_ID, chvSession.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(chvSession.getId().intValue()))
            .andExpect(jsonPath("$.uid").value(DEFAULT_UID))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.sessionDate").value(DEFAULT_SESSION_DATE.toString()))
            .andExpect(jsonPath("$.subject").value(DEFAULT_SUBJECT.toString()))
            .andExpect(jsonPath("$.sessions").value(DEFAULT_SESSIONS))
            .andExpect(jsonPath("$.people").value(DEFAULT_PEOPLE))
            .andExpect(jsonPath("$.comment").value(DEFAULT_COMMENT))
            .andExpect(jsonPath("$.deleted").value(DEFAULT_DELETED.booleanValue()))
            .andExpect(jsonPath("$.startEntryTime").value(DEFAULT_START_ENTRY_TIME.toString()))
            .andExpect(jsonPath("$.finishedEntryTime").value(DEFAULT_FINISHED_ENTRY_TIME.toString()))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()));
    }

    @Test
    @Transactional
    void getNonExistingChvSession() throws Exception {
        // Get the chvSession
        restChvSessionMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingChvSession() throws Exception {
        // Initialize the database
        chvSessionRepository.saveAndFlush(chvSession);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the chvSession
        ChvSession updatedChvSession = chvSessionRepository.findById(chvSession.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedChvSession are not directly saved in db
        em.detach(updatedChvSession);
        updatedChvSession
            .uid(UPDATED_UID)
            .code(UPDATED_CODE)
            .name(UPDATED_NAME)
            .sessionDate(UPDATED_SESSION_DATE)
            .subject(UPDATED_SUBJECT)
            .sessions(UPDATED_SESSIONS)
            .peopleItns(UPDATED_PEOPLE)
            .comment(UPDATED_COMMENT)
            .deleted(UPDATED_DELETED)
            .startEntryTime(UPDATED_START_ENTRY_TIME)
            .finishedEntryTime(UPDATED_FINISHED_ENTRY_TIME)
            .status(UPDATED_STATUS);

        restChvSessionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedChvSession.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedChvSession))
            )
            .andExpect(status().isOk());

        // Validate the ChvSession in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedChvSessionToMatchAllProperties(updatedChvSession);
    }

    @Test
    @Transactional
    void putNonExistingChvSession() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        chvSession.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restChvSessionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, chvSession.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(chvSession))
            )
            .andExpect(status().isBadRequest());

        // Validate the ChvSession in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchChvSession() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        chvSession.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restChvSessionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(chvSession))
            )
            .andExpect(status().isBadRequest());

        // Validate the ChvSession in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamChvSession() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        chvSession.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restChvSessionMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(chvSession)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ChvSession in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateChvSessionWithPatch() throws Exception {
        // Initialize the database
        chvSessionRepository.saveAndFlush(chvSession);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the chvSession using partial update
        ChvSession partialUpdatedChvSession = new ChvSession();
        partialUpdatedChvSession.setId(chvSession.getId());

        partialUpdatedChvSession
            .name(UPDATED_NAME)
            .sessionDate(UPDATED_SESSION_DATE)
            .subject(UPDATED_SUBJECT)
            .sessions(UPDATED_SESSIONS)
            .deleted(UPDATED_DELETED)
            .startEntryTime(UPDATED_START_ENTRY_TIME)
            .finishedEntryTime(UPDATED_FINISHED_ENTRY_TIME);

        restChvSessionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedChvSession.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedChvSession))
            )
            .andExpect(status().isOk());

        // Validate the ChvSession in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertChvSessionUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedChvSession, chvSession),
            getPersistedChvSession(chvSession)
        );
    }

    @Test
    @Transactional
    void fullUpdateChvSessionWithPatch() throws Exception {
        // Initialize the database
        chvSessionRepository.saveAndFlush(chvSession);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the chvSession using partial update
        ChvSession partialUpdatedChvSession = new ChvSession();
        partialUpdatedChvSession.setId(chvSession.getId());

        partialUpdatedChvSession
            .uid(UPDATED_UID)
            .code(UPDATED_CODE)
            .name(UPDATED_NAME)
            .sessionDate(UPDATED_SESSION_DATE)
            .subject(UPDATED_SUBJECT)
            .sessions(UPDATED_SESSIONS)
            .peopleItns(UPDATED_PEOPLE)
            .comment(UPDATED_COMMENT)
            .deleted(UPDATED_DELETED)
            .startEntryTime(UPDATED_START_ENTRY_TIME)
            .finishedEntryTime(UPDATED_FINISHED_ENTRY_TIME)
            .status(UPDATED_STATUS);

        restChvSessionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedChvSession.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedChvSession))
            )
            .andExpect(status().isOk());

        // Validate the ChvSession in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertChvSessionUpdatableFieldsEquals(partialUpdatedChvSession, getPersistedChvSession(partialUpdatedChvSession));
    }

    @Test
    @Transactional
    void patchNonExistingChvSession() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        chvSession.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restChvSessionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, chvSession.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(chvSession))
            )
            .andExpect(status().isBadRequest());

        // Validate the ChvSession in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchChvSession() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        chvSession.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restChvSessionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(chvSession))
            )
            .andExpect(status().isBadRequest());

        // Validate the ChvSession in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamChvSession() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        chvSession.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restChvSessionMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(chvSession)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ChvSession in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteChvSession() throws Exception {
        // Initialize the database
        chvSessionRepository.saveAndFlush(chvSession);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the chvSession
        restChvSessionMockMvc
            .perform(delete(ENTITY_API_URL_ID, chvSession.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return chvSessionRepository.count();
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

    protected ChvSession getPersistedChvSession(ChvSession chvSession) {
        return chvSessionRepository.findById(chvSession.getId()).orElseThrow();
    }

    protected void assertPersistedChvSessionToMatchAllProperties(ChvSession expectedChvSession) {
        assertChvSessionAllPropertiesEquals(expectedChvSession, getPersistedChvSession(expectedChvSession));
    }

    protected void assertPersistedChvSessionToMatchUpdatableProperties(ChvSession expectedChvSession) {
        assertChvSessionAllUpdatablePropertiesEquals(expectedChvSession, getPersistedChvSession(expectedChvSession));
    }
}
