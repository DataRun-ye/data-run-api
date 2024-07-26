package org.nmcpye.datarun.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.nmcpye.datarun.domain.ProgressStatusAsserts.*;
import static org.nmcpye.datarun.web.rest.TestUtil.createUpdateProxyForBean;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.domain.ProgressStatus;
import org.nmcpye.datarun.repository.ProgressStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link ProgressStatusResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ProgressStatusResourceIT {

    private static final String DEFAULT_UID = "AAAAAAAAAA";
    private static final String UPDATED_UID = "BBBBBBBBBB";

    private static final String DEFAULT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/progress-statuses";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ProgressStatusRepository progressStatusRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restProgressStatusMockMvc;

    private ProgressStatus progressStatus;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ProgressStatus createEntity(EntityManager em) {
        ProgressStatus progressStatus = new ProgressStatus().uid(DEFAULT_UID).code(DEFAULT_CODE).name(DEFAULT_NAME);
        return progressStatus;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ProgressStatus createUpdatedEntity(EntityManager em) {
        ProgressStatus progressStatus = new ProgressStatus().uid(UPDATED_UID).code(UPDATED_CODE).name(UPDATED_NAME);
        return progressStatus;
    }

    @BeforeEach
    public void initTest() {
        progressStatus = createEntity(em);
    }

    @Test
    @Transactional
    void createProgressStatus() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ProgressStatus
        var returnedProgressStatus = om.readValue(
            restProgressStatusMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(progressStatus)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ProgressStatus.class
        );

        // Validate the ProgressStatus in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertProgressStatusUpdatableFieldsEquals(returnedProgressStatus, getPersistedProgressStatus(returnedProgressStatus));
    }

    @Test
    @Transactional
    void createProgressStatusWithExistingId() throws Exception {
        // Create the ProgressStatus with an existing ID
        progressStatus.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restProgressStatusMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(progressStatus)))
            .andExpect(status().isBadRequest());

        // Validate the ProgressStatus in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkUidIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        progressStatus.setUid(null);

        // Create the ProgressStatus, which fails.

        restProgressStatusMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(progressStatus)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllProgressStatuses() throws Exception {
        // Initialize the database
        progressStatusRepository.saveAndFlush(progressStatus);

        // Get all the progressStatusList
        restProgressStatusMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(progressStatus.getId().intValue())))
            .andExpect(jsonPath("$.[*].uid").value(hasItem(DEFAULT_UID)))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)));
    }

    @Test
    @Transactional
    void getProgressStatus() throws Exception {
        // Initialize the database
        progressStatusRepository.saveAndFlush(progressStatus);

        // Get the progressStatus
        restProgressStatusMockMvc
            .perform(get(ENTITY_API_URL_ID, progressStatus.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(progressStatus.getId().intValue()))
            .andExpect(jsonPath("$.uid").value(DEFAULT_UID))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME));
    }

    @Test
    @Transactional
    void getNonExistingProgressStatus() throws Exception {
        // Get the progressStatus
        restProgressStatusMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingProgressStatus() throws Exception {
        // Initialize the database
        progressStatusRepository.saveAndFlush(progressStatus);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the progressStatus
        ProgressStatus updatedProgressStatus = progressStatusRepository.findById(progressStatus.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedProgressStatus are not directly saved in db
        em.detach(updatedProgressStatus);
        updatedProgressStatus.uid(UPDATED_UID).code(UPDATED_CODE).name(UPDATED_NAME);

        restProgressStatusMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedProgressStatus.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedProgressStatus))
            )
            .andExpect(status().isOk());

        // Validate the ProgressStatus in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedProgressStatusToMatchAllProperties(updatedProgressStatus);
    }

    @Test
    @Transactional
    void putNonExistingProgressStatus() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        progressStatus.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restProgressStatusMockMvc
            .perform(
                put(ENTITY_API_URL_ID, progressStatus.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(progressStatus))
            )
            .andExpect(status().isBadRequest());

        // Validate the ProgressStatus in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchProgressStatus() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        progressStatus.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProgressStatusMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(progressStatus))
            )
            .andExpect(status().isBadRequest());

        // Validate the ProgressStatus in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamProgressStatus() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        progressStatus.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProgressStatusMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(progressStatus)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ProgressStatus in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateProgressStatusWithPatch() throws Exception {
        // Initialize the database
        progressStatusRepository.saveAndFlush(progressStatus);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the progressStatus using partial update
        ProgressStatus partialUpdatedProgressStatus = new ProgressStatus();
        partialUpdatedProgressStatus.setId(progressStatus.getId());

        restProgressStatusMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedProgressStatus.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedProgressStatus))
            )
            .andExpect(status().isOk());

        // Validate the ProgressStatus in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertProgressStatusUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedProgressStatus, progressStatus),
            getPersistedProgressStatus(progressStatus)
        );
    }

    @Test
    @Transactional
    void fullUpdateProgressStatusWithPatch() throws Exception {
        // Initialize the database
        progressStatusRepository.saveAndFlush(progressStatus);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the progressStatus using partial update
        ProgressStatus partialUpdatedProgressStatus = new ProgressStatus();
        partialUpdatedProgressStatus.setId(progressStatus.getId());

        partialUpdatedProgressStatus.uid(UPDATED_UID).code(UPDATED_CODE).name(UPDATED_NAME);

        restProgressStatusMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedProgressStatus.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedProgressStatus))
            )
            .andExpect(status().isOk());

        // Validate the ProgressStatus in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertProgressStatusUpdatableFieldsEquals(partialUpdatedProgressStatus, getPersistedProgressStatus(partialUpdatedProgressStatus));
    }

    @Test
    @Transactional
    void patchNonExistingProgressStatus() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        progressStatus.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restProgressStatusMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, progressStatus.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(progressStatus))
            )
            .andExpect(status().isBadRequest());

        // Validate the ProgressStatus in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchProgressStatus() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        progressStatus.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProgressStatusMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(progressStatus))
            )
            .andExpect(status().isBadRequest());

        // Validate the ProgressStatus in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamProgressStatus() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        progressStatus.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProgressStatusMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(progressStatus)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ProgressStatus in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteProgressStatus() throws Exception {
        // Initialize the database
        progressStatusRepository.saveAndFlush(progressStatus);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the progressStatus
        restProgressStatusMockMvc
            .perform(delete(ENTITY_API_URL_ID, progressStatus.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return progressStatusRepository.count();
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

    protected ProgressStatus getPersistedProgressStatus(ProgressStatus progressStatus) {
        return progressStatusRepository.findById(progressStatus.getId()).orElseThrow();
    }

    protected void assertPersistedProgressStatusToMatchAllProperties(ProgressStatus expectedProgressStatus) {
        assertProgressStatusAllPropertiesEquals(expectedProgressStatus, getPersistedProgressStatus(expectedProgressStatus));
    }

    protected void assertPersistedProgressStatusToMatchUpdatableProperties(ProgressStatus expectedProgressStatus) {
        assertProgressStatusAllUpdatablePropertiesEquals(expectedProgressStatus, getPersistedProgressStatus(expectedProgressStatus));
    }
}
