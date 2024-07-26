package org.nmcpye.datarun.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.nmcpye.datarun.domain.WarehouseTransactionAsserts.*;
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
import org.nmcpye.datarun.domain.Warehouse;
import org.nmcpye.datarun.domain.WarehouseTransaction;
import org.nmcpye.datarun.domain.enumeration.SyncableStatus;
import org.nmcpye.datarun.repository.WarehouseTransactionRepository;
import org.nmcpye.datarun.service.WarehouseTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link WarehouseTransactionResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class WarehouseTransactionResourceIT {

    private static final String DEFAULT_UID = "AAAAAAAAAA";
    private static final String UPDATED_UID = "BBBBBBBBBB";

    private static final String DEFAULT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_IMOV_UID = "AAAAAAAAAA";
    private static final String UPDATED_IMOV_UID = "BBBBBBBBBB";

    private static final Instant DEFAULT_TRANSACTION_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_TRANSACTION_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Integer DEFAULT_PHASE_NO = 1;
    private static final Integer UPDATED_PHASE_NO = 2;

    private static final String DEFAULT_ENTRY_TYPE = "AAAAAAAAAA";
    private static final String UPDATED_ENTRY_TYPE = "BBBBBBBBBB";

    private static final Integer DEFAULT_QUANTITY = 0;
    private static final Integer UPDATED_QUANTITY = 1;

    private static final String DEFAULT_NOTES = "AAAAAAAAAA";
    private static final String UPDATED_NOTES = "BBBBBBBBBB";

    private static final String DEFAULT_PERSON_NAME = "AAAAAAAAAA";
    private static final String UPDATED_PERSON_NAME = "BBBBBBBBBB";

    private static final Integer DEFAULT_WORK_DAY_ID = 1;
    private static final Integer UPDATED_WORK_DAY_ID = 2;

    private static final Instant DEFAULT_SUBMISSION_TIME = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_SUBMISSION_TIME = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Long DEFAULT_SUBMISSION_ID = 1L;
    private static final Long UPDATED_SUBMISSION_ID = 2L;

    private static final Boolean DEFAULT_DELETED = false;
    private static final Boolean UPDATED_DELETED = true;

    private static final String DEFAULT_SUBMISSION_UUID = "AAAAAAAAAA";
    private static final String UPDATED_SUBMISSION_UUID = "BBBBBBBBBB";

    private static final Instant DEFAULT_START_ENTRY_TIME = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_START_ENTRY_TIME = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_FINISHED_ENTRY_TIME = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_FINISHED_ENTRY_TIME = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final SyncableStatus DEFAULT_STATUS = SyncableStatus.ACTIVE;
    private static final SyncableStatus UPDATED_STATUS = SyncableStatus.COMPLETED;

    private static final String ENTITY_API_URL = "/api/warehouse-transactions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private WarehouseTransactionRepository warehouseTransactionRepository;

    @Mock
    private WarehouseTransactionRepository warehouseTransactionRepositoryMock;

    @Mock
    private WarehouseTransactionService warehouseTransactionServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restWarehouseTransactionMockMvc;

    private WarehouseTransaction warehouseTransaction;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static WarehouseTransaction createEntity(EntityManager em) {
        WarehouseTransaction warehouseTransaction = new WarehouseTransaction()
            .uid(DEFAULT_UID)
            .code(DEFAULT_CODE)
            .name(DEFAULT_NAME)
            .imovUid(DEFAULT_IMOV_UID)
            .transactionDate(DEFAULT_TRANSACTION_DATE)
            .phaseNo(DEFAULT_PHASE_NO)
            .entryType(DEFAULT_ENTRY_TYPE)
            .quantity(DEFAULT_QUANTITY)
            .notes(DEFAULT_NOTES)
            .personName(DEFAULT_PERSON_NAME)
            .workDayId(DEFAULT_WORK_DAY_ID)
            .submissionTime(DEFAULT_SUBMISSION_TIME)
            .submissionId(DEFAULT_SUBMISSION_ID)
            .deleted(DEFAULT_DELETED)
            .submissionUuid(DEFAULT_SUBMISSION_UUID)
            .startEntryTime(DEFAULT_START_ENTRY_TIME)
            .finishedEntryTime(DEFAULT_FINISHED_ENTRY_TIME)
            .status(DEFAULT_STATUS);
        // Add required entity
        Warehouse warehouse;
        if (TestUtil.findAll(em, Warehouse.class).isEmpty()) {
            warehouse = WarehouseResourceIT.createEntity(em);
            em.persist(warehouse);
            em.flush();
        } else {
            warehouse = TestUtil.findAll(em, Warehouse.class).get(0);
        }
        warehouseTransaction.setWarehouse(warehouse);
        return warehouseTransaction;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static WarehouseTransaction createUpdatedEntity(EntityManager em) {
        WarehouseTransaction warehouseTransaction = new WarehouseTransaction()
            .uid(UPDATED_UID)
            .code(UPDATED_CODE)
            .name(UPDATED_NAME)
            .imovUid(UPDATED_IMOV_UID)
            .transactionDate(UPDATED_TRANSACTION_DATE)
            .phaseNo(UPDATED_PHASE_NO)
            .entryType(UPDATED_ENTRY_TYPE)
            .quantity(UPDATED_QUANTITY)
            .notes(UPDATED_NOTES)
            .personName(UPDATED_PERSON_NAME)
            .workDayId(UPDATED_WORK_DAY_ID)
            .submissionTime(UPDATED_SUBMISSION_TIME)
            .submissionId(UPDATED_SUBMISSION_ID)
            .deleted(UPDATED_DELETED)
            .submissionUuid(UPDATED_SUBMISSION_UUID)
            .startEntryTime(UPDATED_START_ENTRY_TIME)
            .finishedEntryTime(UPDATED_FINISHED_ENTRY_TIME)
            .status(UPDATED_STATUS);
        // Add required entity
        Warehouse warehouse;
        if (TestUtil.findAll(em, Warehouse.class).isEmpty()) {
            warehouse = WarehouseResourceIT.createUpdatedEntity(em);
            em.persist(warehouse);
            em.flush();
        } else {
            warehouse = TestUtil.findAll(em, Warehouse.class).get(0);
        }
        warehouseTransaction.setWarehouse(warehouse);
        return warehouseTransaction;
    }

    @BeforeEach
    public void initTest() {
        warehouseTransaction = createEntity(em);
    }

    @Test
    @Transactional
    void createWarehouseTransaction() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the WarehouseTransaction
        var returnedWarehouseTransaction = om.readValue(
            restWarehouseTransactionMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(warehouseTransaction)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            WarehouseTransaction.class
        );

        // Validate the WarehouseTransaction in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertWarehouseTransactionUpdatableFieldsEquals(
            returnedWarehouseTransaction,
            getPersistedWarehouseTransaction(returnedWarehouseTransaction)
        );
    }

    @Test
    @Transactional
    void createWarehouseTransactionWithExistingId() throws Exception {
        // Create the WarehouseTransaction with an existing ID
        warehouseTransaction.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restWarehouseTransactionMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(warehouseTransaction)))
            .andExpect(status().isBadRequest());

        // Validate the WarehouseTransaction in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkUidIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        warehouseTransaction.setUid(null);

        // Create the WarehouseTransaction, which fails.

        restWarehouseTransactionMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(warehouseTransaction)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllWarehouseTransactions() throws Exception {
        // Initialize the database
        warehouseTransactionRepository.saveAndFlush(warehouseTransaction);

        // Get all the warehouseTransactionList
        restWarehouseTransactionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(warehouseTransaction.getId().intValue())))
            .andExpect(jsonPath("$.[*].uid").value(hasItem(DEFAULT_UID)))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].imovUid").value(hasItem(DEFAULT_IMOV_UID)))
            .andExpect(jsonPath("$.[*].transactionDate").value(hasItem(DEFAULT_TRANSACTION_DATE.toString())))
            .andExpect(jsonPath("$.[*].phaseNo").value(hasItem(DEFAULT_PHASE_NO)))
            .andExpect(jsonPath("$.[*].entryType").value(hasItem(DEFAULT_ENTRY_TYPE)))
            .andExpect(jsonPath("$.[*].quantity").value(hasItem(DEFAULT_QUANTITY)))
            .andExpect(jsonPath("$.[*].notes").value(hasItem(DEFAULT_NOTES)))
            .andExpect(jsonPath("$.[*].personName").value(hasItem(DEFAULT_PERSON_NAME)))
            .andExpect(jsonPath("$.[*].workDayId").value(hasItem(DEFAULT_WORK_DAY_ID)))
            .andExpect(jsonPath("$.[*].submissionTime").value(hasItem(DEFAULT_SUBMISSION_TIME.toString())))
            .andExpect(jsonPath("$.[*].submissionId").value(hasItem(DEFAULT_SUBMISSION_ID.intValue())))
            .andExpect(jsonPath("$.[*].deleted").value(hasItem(DEFAULT_DELETED.booleanValue())))
            .andExpect(jsonPath("$.[*].submissionUuid").value(hasItem(DEFAULT_SUBMISSION_UUID)))
            .andExpect(jsonPath("$.[*].startEntryTime").value(hasItem(DEFAULT_START_ENTRY_TIME.toString())))
            .andExpect(jsonPath("$.[*].finishedEntryTime").value(hasItem(DEFAULT_FINISHED_ENTRY_TIME.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllWarehouseTransactionsWithEagerRelationshipsIsEnabled() throws Exception {
        when(warehouseTransactionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restWarehouseTransactionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(warehouseTransactionServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllWarehouseTransactionsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(warehouseTransactionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restWarehouseTransactionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(warehouseTransactionRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getWarehouseTransaction() throws Exception {
        // Initialize the database
        warehouseTransactionRepository.saveAndFlush(warehouseTransaction);

        // Get the warehouseTransaction
        restWarehouseTransactionMockMvc
            .perform(get(ENTITY_API_URL_ID, warehouseTransaction.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(warehouseTransaction.getId().intValue()))
            .andExpect(jsonPath("$.uid").value(DEFAULT_UID))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.imovUid").value(DEFAULT_IMOV_UID))
            .andExpect(jsonPath("$.transactionDate").value(DEFAULT_TRANSACTION_DATE.toString()))
            .andExpect(jsonPath("$.phaseNo").value(DEFAULT_PHASE_NO))
            .andExpect(jsonPath("$.entryType").value(DEFAULT_ENTRY_TYPE))
            .andExpect(jsonPath("$.quantity").value(DEFAULT_QUANTITY))
            .andExpect(jsonPath("$.notes").value(DEFAULT_NOTES))
            .andExpect(jsonPath("$.personName").value(DEFAULT_PERSON_NAME))
            .andExpect(jsonPath("$.workDayId").value(DEFAULT_WORK_DAY_ID))
            .andExpect(jsonPath("$.submissionTime").value(DEFAULT_SUBMISSION_TIME.toString()))
            .andExpect(jsonPath("$.submissionId").value(DEFAULT_SUBMISSION_ID.intValue()))
            .andExpect(jsonPath("$.deleted").value(DEFAULT_DELETED.booleanValue()))
            .andExpect(jsonPath("$.submissionUuid").value(DEFAULT_SUBMISSION_UUID))
            .andExpect(jsonPath("$.startEntryTime").value(DEFAULT_START_ENTRY_TIME.toString()))
            .andExpect(jsonPath("$.finishedEntryTime").value(DEFAULT_FINISHED_ENTRY_TIME.toString()))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()));
    }

    @Test
    @Transactional
    void getNonExistingWarehouseTransaction() throws Exception {
        // Get the warehouseTransaction
        restWarehouseTransactionMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingWarehouseTransaction() throws Exception {
        // Initialize the database
        warehouseTransactionRepository.saveAndFlush(warehouseTransaction);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the warehouseTransaction
        WarehouseTransaction updatedWarehouseTransaction = warehouseTransactionRepository
            .findById(warehouseTransaction.getId())
            .orElseThrow();
        // Disconnect from session so that the updates on updatedWarehouseTransaction are not directly saved in db
        em.detach(updatedWarehouseTransaction);
        updatedWarehouseTransaction
            .uid(UPDATED_UID)
            .code(UPDATED_CODE)
            .name(UPDATED_NAME)
            .imovUid(UPDATED_IMOV_UID)
            .transactionDate(UPDATED_TRANSACTION_DATE)
            .phaseNo(UPDATED_PHASE_NO)
            .entryType(UPDATED_ENTRY_TYPE)
            .quantity(UPDATED_QUANTITY)
            .notes(UPDATED_NOTES)
            .personName(UPDATED_PERSON_NAME)
            .workDayId(UPDATED_WORK_DAY_ID)
            .submissionTime(UPDATED_SUBMISSION_TIME)
            .submissionId(UPDATED_SUBMISSION_ID)
            .deleted(UPDATED_DELETED)
            .submissionUuid(UPDATED_SUBMISSION_UUID)
            .startEntryTime(UPDATED_START_ENTRY_TIME)
            .finishedEntryTime(UPDATED_FINISHED_ENTRY_TIME)
            .status(UPDATED_STATUS);

        restWarehouseTransactionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedWarehouseTransaction.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedWarehouseTransaction))
            )
            .andExpect(status().isOk());

        // Validate the WarehouseTransaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedWarehouseTransactionToMatchAllProperties(updatedWarehouseTransaction);
    }

    @Test
    @Transactional
    void putNonExistingWarehouseTransaction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        warehouseTransaction.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restWarehouseTransactionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, warehouseTransaction.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(warehouseTransaction))
            )
            .andExpect(status().isBadRequest());

        // Validate the WarehouseTransaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchWarehouseTransaction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        warehouseTransaction.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restWarehouseTransactionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(warehouseTransaction))
            )
            .andExpect(status().isBadRequest());

        // Validate the WarehouseTransaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamWarehouseTransaction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        warehouseTransaction.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restWarehouseTransactionMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(warehouseTransaction)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the WarehouseTransaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateWarehouseTransactionWithPatch() throws Exception {
        // Initialize the database
        warehouseTransactionRepository.saveAndFlush(warehouseTransaction);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the warehouseTransaction using partial update
        WarehouseTransaction partialUpdatedWarehouseTransaction = new WarehouseTransaction();
        partialUpdatedWarehouseTransaction.setId(warehouseTransaction.getId());

        partialUpdatedWarehouseTransaction
            .uid(UPDATED_UID)
            .code(UPDATED_CODE)
            .name(UPDATED_NAME)
            .phaseNo(UPDATED_PHASE_NO)
            .notes(UPDATED_NOTES)
            .personName(UPDATED_PERSON_NAME)
            .submissionTime(UPDATED_SUBMISSION_TIME)
            .finishedEntryTime(UPDATED_FINISHED_ENTRY_TIME);

        restWarehouseTransactionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedWarehouseTransaction.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedWarehouseTransaction))
            )
            .andExpect(status().isOk());

        // Validate the WarehouseTransaction in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertWarehouseTransactionUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedWarehouseTransaction, warehouseTransaction),
            getPersistedWarehouseTransaction(warehouseTransaction)
        );
    }

    @Test
    @Transactional
    void fullUpdateWarehouseTransactionWithPatch() throws Exception {
        // Initialize the database
        warehouseTransactionRepository.saveAndFlush(warehouseTransaction);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the warehouseTransaction using partial update
        WarehouseTransaction partialUpdatedWarehouseTransaction = new WarehouseTransaction();
        partialUpdatedWarehouseTransaction.setId(warehouseTransaction.getId());

        partialUpdatedWarehouseTransaction
            .uid(UPDATED_UID)
            .code(UPDATED_CODE)
            .name(UPDATED_NAME)
            .imovUid(UPDATED_IMOV_UID)
            .transactionDate(UPDATED_TRANSACTION_DATE)
            .phaseNo(UPDATED_PHASE_NO)
            .entryType(UPDATED_ENTRY_TYPE)
            .quantity(UPDATED_QUANTITY)
            .notes(UPDATED_NOTES)
            .personName(UPDATED_PERSON_NAME)
            .workDayId(UPDATED_WORK_DAY_ID)
            .submissionTime(UPDATED_SUBMISSION_TIME)
            .submissionId(UPDATED_SUBMISSION_ID)
            .deleted(UPDATED_DELETED)
            .submissionUuid(UPDATED_SUBMISSION_UUID)
            .startEntryTime(UPDATED_START_ENTRY_TIME)
            .finishedEntryTime(UPDATED_FINISHED_ENTRY_TIME)
            .status(UPDATED_STATUS);

        restWarehouseTransactionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedWarehouseTransaction.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedWarehouseTransaction))
            )
            .andExpect(status().isOk());

        // Validate the WarehouseTransaction in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertWarehouseTransactionUpdatableFieldsEquals(
            partialUpdatedWarehouseTransaction,
            getPersistedWarehouseTransaction(partialUpdatedWarehouseTransaction)
        );
    }

    @Test
    @Transactional
    void patchNonExistingWarehouseTransaction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        warehouseTransaction.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restWarehouseTransactionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, warehouseTransaction.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(warehouseTransaction))
            )
            .andExpect(status().isBadRequest());

        // Validate the WarehouseTransaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchWarehouseTransaction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        warehouseTransaction.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restWarehouseTransactionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(warehouseTransaction))
            )
            .andExpect(status().isBadRequest());

        // Validate the WarehouseTransaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamWarehouseTransaction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        warehouseTransaction.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restWarehouseTransactionMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(warehouseTransaction)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the WarehouseTransaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteWarehouseTransaction() throws Exception {
        // Initialize the database
        warehouseTransactionRepository.saveAndFlush(warehouseTransaction);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the warehouseTransaction
        restWarehouseTransactionMockMvc
            .perform(delete(ENTITY_API_URL_ID, warehouseTransaction.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return warehouseTransactionRepository.count();
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

    protected WarehouseTransaction getPersistedWarehouseTransaction(WarehouseTransaction warehouseTransaction) {
        return warehouseTransactionRepository.findById(warehouseTransaction.getId()).orElseThrow();
    }

    protected void assertPersistedWarehouseTransactionToMatchAllProperties(WarehouseTransaction expectedWarehouseTransaction) {
        assertWarehouseTransactionAllPropertiesEquals(
            expectedWarehouseTransaction,
            getPersistedWarehouseTransaction(expectedWarehouseTransaction)
        );
    }

    protected void assertPersistedWarehouseTransactionToMatchUpdatableProperties(WarehouseTransaction expectedWarehouseTransaction) {
        assertWarehouseTransactionAllUpdatablePropertiesEquals(
            expectedWarehouseTransaction,
            getPersistedWarehouseTransaction(expectedWarehouseTransaction)
        );
    }
}
