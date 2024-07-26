package org.nmcpye.datarun.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.nmcpye.datarun.domain.ChvSupplyAsserts.*;
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
import org.nmcpye.datarun.domain.ChvSupply;
import org.nmcpye.datarun.domain.Team;
import org.nmcpye.datarun.domain.enumeration.DrugItemType;
import org.nmcpye.datarun.domain.enumeration.SyncableStatus;
import org.nmcpye.datarun.repository.ChvSupplyRepository;
import org.nmcpye.datarun.service.ChvSupplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link ChvSupplyResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ChvSupplyResourceIT {

    private static final String DEFAULT_UID = "AAAAAAAAAA";
    private static final String UPDATED_UID = "BBBBBBBBBB";

    private static final String DEFAULT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final DrugItemType DEFAULT_ITEM = DrugItemType.MRDT;
    private static final DrugItemType UPDATED_ITEM = DrugItemType.MDrugs;

    private static final Integer DEFAULT_PREVIOUS_BALANCE = 1;
    private static final Integer UPDATED_PREVIOUS_BALANCE = 2;

    private static final Integer DEFAULT_NEW_SUPPLY = 1;
    private static final Integer UPDATED_NEW_SUPPLY = 2;

    private static final Integer DEFAULT_CONSUMED = 1;
    private static final Integer UPDATED_CONSUMED = 2;

    private static final Integer DEFAULT_LOST_CORRUPT = 1;
    private static final Integer UPDATED_LOST_CORRUPT = 2;

    private static final Integer DEFAULT_REMAINING = 1;
    private static final Integer UPDATED_REMAINING = 2;

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

    private static final String ENTITY_API_URL = "/api/chv-supplies";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ChvSupplyRepository chvSupplyRepository;

    @Mock
    private ChvSupplyRepository chvSupplyRepositoryMock;

    @Mock
    private ChvSupplyService chvSupplyServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restChvSupplyMockMvc;

    private ChvSupply chvSupply;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ChvSupply createEntity(EntityManager em) {
        ChvSupply chvSupply = new ChvSupply()
            .uid(DEFAULT_UID)
            .code(DEFAULT_CODE)
            .name(DEFAULT_NAME)
            .item(DEFAULT_ITEM)
            .previousBalance(DEFAULT_PREVIOUS_BALANCE)
            .newSupply(DEFAULT_NEW_SUPPLY)
            .consumed(DEFAULT_CONSUMED)
            .lostCorrupt(DEFAULT_LOST_CORRUPT)
            .remaining(DEFAULT_REMAINING)
            .comment(DEFAULT_COMMENT)
            .deleted(DEFAULT_DELETED)
            .startEntryTime(DEFAULT_START_ENTRY_TIME)
            .finishedEntryTime(DEFAULT_FINISHED_ENTRY_TIME)
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
        chvSupply.setActivity(activity);
        // Add required entity
        Team team;
        if (TestUtil.findAll(em, Team.class).isEmpty()) {
            team = TeamResourceIT.createEntity(em);
            em.persist(team);
            em.flush();
        } else {
            team = TestUtil.findAll(em, Team.class).get(0);
        }
        chvSupply.setTeam(team);
        return chvSupply;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ChvSupply createUpdatedEntity(EntityManager em) {
        ChvSupply chvSupply = new ChvSupply()
            .uid(UPDATED_UID)
            .code(UPDATED_CODE)
            .name(UPDATED_NAME)
            .item(UPDATED_ITEM)
            .previousBalance(UPDATED_PREVIOUS_BALANCE)
            .newSupply(UPDATED_NEW_SUPPLY)
            .consumed(UPDATED_CONSUMED)
            .lostCorrupt(UPDATED_LOST_CORRUPT)
            .remaining(UPDATED_REMAINING)
            .comment(UPDATED_COMMENT)
            .deleted(UPDATED_DELETED)
            .startEntryTime(UPDATED_START_ENTRY_TIME)
            .finishedEntryTime(UPDATED_FINISHED_ENTRY_TIME)
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
        chvSupply.setActivity(activity);
        // Add required entity
        Team team;
        if (TestUtil.findAll(em, Team.class).isEmpty()) {
            team = TeamResourceIT.createUpdatedEntity(em);
            em.persist(team);
            em.flush();
        } else {
            team = TestUtil.findAll(em, Team.class).get(0);
        }
        chvSupply.setTeam(team);
        return chvSupply;
    }

    @BeforeEach
    public void initTest() {
        chvSupply = createEntity(em);
    }

    @Test
    @Transactional
    void createChvSupply() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ChvSupply
        var returnedChvSupply = om.readValue(
            restChvSupplyMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(chvSupply)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ChvSupply.class
        );

        // Validate the ChvSupply in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertChvSupplyUpdatableFieldsEquals(returnedChvSupply, getPersistedChvSupply(returnedChvSupply));
    }

    @Test
    @Transactional
    void createChvSupplyWithExistingId() throws Exception {
        // Create the ChvSupply with an existing ID
        chvSupply.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restChvSupplyMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(chvSupply)))
            .andExpect(status().isBadRequest());

        // Validate the ChvSupply in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkUidIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        chvSupply.setUid(null);

        // Create the ChvSupply, which fails.

        restChvSupplyMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(chvSupply)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        chvSupply.setStatus(null);

        // Create the ChvSupply, which fails.

        restChvSupplyMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(chvSupply)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllChvSupplies() throws Exception {
        // Initialize the database
        chvSupplyRepository.saveAndFlush(chvSupply);

        // Get all the chvSupplyList
        restChvSupplyMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(chvSupply.getId().intValue())))
            .andExpect(jsonPath("$.[*].uid").value(hasItem(DEFAULT_UID)))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].item").value(hasItem(DEFAULT_ITEM.toString())))
            .andExpect(jsonPath("$.[*].previousBalance").value(hasItem(DEFAULT_PREVIOUS_BALANCE)))
            .andExpect(jsonPath("$.[*].newSupply").value(hasItem(DEFAULT_NEW_SUPPLY)))
            .andExpect(jsonPath("$.[*].consumed").value(hasItem(DEFAULT_CONSUMED)))
            .andExpect(jsonPath("$.[*].lostCorrupt").value(hasItem(DEFAULT_LOST_CORRUPT)))
            .andExpect(jsonPath("$.[*].remaining").value(hasItem(DEFAULT_REMAINING)))
            .andExpect(jsonPath("$.[*].comment").value(hasItem(DEFAULT_COMMENT)))
            .andExpect(jsonPath("$.[*].deleted").value(hasItem(DEFAULT_DELETED.booleanValue())))
            .andExpect(jsonPath("$.[*].startEntryTime").value(hasItem(DEFAULT_START_ENTRY_TIME.toString())))
            .andExpect(jsonPath("$.[*].finishedEntryTime").value(hasItem(DEFAULT_FINISHED_ENTRY_TIME.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllChvSuppliesWithEagerRelationshipsIsEnabled() throws Exception {
        when(chvSupplyServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restChvSupplyMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(chvSupplyServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllChvSuppliesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(chvSupplyServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restChvSupplyMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(chvSupplyRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getChvSupply() throws Exception {
        // Initialize the database
        chvSupplyRepository.saveAndFlush(chvSupply);

        // Get the chvSupply
        restChvSupplyMockMvc
            .perform(get(ENTITY_API_URL_ID, chvSupply.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(chvSupply.getId().intValue()))
            .andExpect(jsonPath("$.uid").value(DEFAULT_UID))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.item").value(DEFAULT_ITEM.toString()))
            .andExpect(jsonPath("$.previousBalance").value(DEFAULT_PREVIOUS_BALANCE))
            .andExpect(jsonPath("$.newSupply").value(DEFAULT_NEW_SUPPLY))
            .andExpect(jsonPath("$.consumed").value(DEFAULT_CONSUMED))
            .andExpect(jsonPath("$.lostCorrupt").value(DEFAULT_LOST_CORRUPT))
            .andExpect(jsonPath("$.remaining").value(DEFAULT_REMAINING))
            .andExpect(jsonPath("$.comment").value(DEFAULT_COMMENT))
            .andExpect(jsonPath("$.deleted").value(DEFAULT_DELETED.booleanValue()))
            .andExpect(jsonPath("$.startEntryTime").value(DEFAULT_START_ENTRY_TIME.toString()))
            .andExpect(jsonPath("$.finishedEntryTime").value(DEFAULT_FINISHED_ENTRY_TIME.toString()))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()));
    }

    @Test
    @Transactional
    void getNonExistingChvSupply() throws Exception {
        // Get the chvSupply
        restChvSupplyMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingChvSupply() throws Exception {
        // Initialize the database
        chvSupplyRepository.saveAndFlush(chvSupply);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the chvSupply
        ChvSupply updatedChvSupply = chvSupplyRepository.findById(chvSupply.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedChvSupply are not directly saved in db
        em.detach(updatedChvSupply);
        updatedChvSupply
            .uid(UPDATED_UID)
            .code(UPDATED_CODE)
            .name(UPDATED_NAME)
            .item(UPDATED_ITEM)
            .previousBalance(UPDATED_PREVIOUS_BALANCE)
            .newSupply(UPDATED_NEW_SUPPLY)
            .consumed(UPDATED_CONSUMED)
            .lostCorrupt(UPDATED_LOST_CORRUPT)
            .remaining(UPDATED_REMAINING)
            .comment(UPDATED_COMMENT)
            .deleted(UPDATED_DELETED)
            .startEntryTime(UPDATED_START_ENTRY_TIME)
            .finishedEntryTime(UPDATED_FINISHED_ENTRY_TIME)
            .status(UPDATED_STATUS);

        restChvSupplyMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedChvSupply.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedChvSupply))
            )
            .andExpect(status().isOk());

        // Validate the ChvSupply in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedChvSupplyToMatchAllProperties(updatedChvSupply);
    }

    @Test
    @Transactional
    void putNonExistingChvSupply() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        chvSupply.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restChvSupplyMockMvc
            .perform(
                put(ENTITY_API_URL_ID, chvSupply.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(chvSupply))
            )
            .andExpect(status().isBadRequest());

        // Validate the ChvSupply in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchChvSupply() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        chvSupply.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restChvSupplyMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(chvSupply))
            )
            .andExpect(status().isBadRequest());

        // Validate the ChvSupply in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamChvSupply() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        chvSupply.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restChvSupplyMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(chvSupply)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ChvSupply in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateChvSupplyWithPatch() throws Exception {
        // Initialize the database
        chvSupplyRepository.saveAndFlush(chvSupply);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the chvSupply using partial update
        ChvSupply partialUpdatedChvSupply = new ChvSupply();
        partialUpdatedChvSupply.setId(chvSupply.getId());

        partialUpdatedChvSupply
            .uid(UPDATED_UID)
            .code(UPDATED_CODE)
            .item(UPDATED_ITEM)
            .previousBalance(UPDATED_PREVIOUS_BALANCE)
            .newSupply(UPDATED_NEW_SUPPLY)
            .lostCorrupt(UPDATED_LOST_CORRUPT)
            .remaining(UPDATED_REMAINING)
            .deleted(UPDATED_DELETED)
            .status(UPDATED_STATUS);

        restChvSupplyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedChvSupply.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedChvSupply))
            )
            .andExpect(status().isOk());

        // Validate the ChvSupply in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertChvSupplyUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedChvSupply, chvSupply),
            getPersistedChvSupply(chvSupply)
        );
    }

    @Test
    @Transactional
    void fullUpdateChvSupplyWithPatch() throws Exception {
        // Initialize the database
        chvSupplyRepository.saveAndFlush(chvSupply);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the chvSupply using partial update
        ChvSupply partialUpdatedChvSupply = new ChvSupply();
        partialUpdatedChvSupply.setId(chvSupply.getId());

        partialUpdatedChvSupply
            .uid(UPDATED_UID)
            .code(UPDATED_CODE)
            .name(UPDATED_NAME)
            .item(UPDATED_ITEM)
            .previousBalance(UPDATED_PREVIOUS_BALANCE)
            .newSupply(UPDATED_NEW_SUPPLY)
            .consumed(UPDATED_CONSUMED)
            .lostCorrupt(UPDATED_LOST_CORRUPT)
            .remaining(UPDATED_REMAINING)
            .comment(UPDATED_COMMENT)
            .deleted(UPDATED_DELETED)
            .startEntryTime(UPDATED_START_ENTRY_TIME)
            .finishedEntryTime(UPDATED_FINISHED_ENTRY_TIME)
            .status(UPDATED_STATUS);

        restChvSupplyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedChvSupply.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedChvSupply))
            )
            .andExpect(status().isOk());

        // Validate the ChvSupply in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertChvSupplyUpdatableFieldsEquals(partialUpdatedChvSupply, getPersistedChvSupply(partialUpdatedChvSupply));
    }

    @Test
    @Transactional
    void patchNonExistingChvSupply() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        chvSupply.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restChvSupplyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, chvSupply.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(chvSupply))
            )
            .andExpect(status().isBadRequest());

        // Validate the ChvSupply in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchChvSupply() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        chvSupply.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restChvSupplyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(chvSupply))
            )
            .andExpect(status().isBadRequest());

        // Validate the ChvSupply in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamChvSupply() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        chvSupply.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restChvSupplyMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(chvSupply)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ChvSupply in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteChvSupply() throws Exception {
        // Initialize the database
        chvSupplyRepository.saveAndFlush(chvSupply);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the chvSupply
        restChvSupplyMockMvc
            .perform(delete(ENTITY_API_URL_ID, chvSupply.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return chvSupplyRepository.count();
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

    protected ChvSupply getPersistedChvSupply(ChvSupply chvSupply) {
        return chvSupplyRepository.findById(chvSupply.getId()).orElseThrow();
    }

    protected void assertPersistedChvSupplyToMatchAllProperties(ChvSupply expectedChvSupply) {
        assertChvSupplyAllPropertiesEquals(expectedChvSupply, getPersistedChvSupply(expectedChvSupply));
    }

    protected void assertPersistedChvSupplyToMatchUpdatableProperties(ChvSupply expectedChvSupply) {
        assertChvSupplyAllUpdatablePropertiesEquals(expectedChvSupply, getPersistedChvSupply(expectedChvSupply));
    }
}
