package org.nmcpye.datarun.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.nmcpye.datarun.domain.ReviewTeamAsserts.*;
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
import org.nmcpye.datarun.domain.ReviewTeam;
import org.nmcpye.datarun.repository.ReviewTeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link ReviewTeamResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ReviewTeamResourceIT {

    private static final String DEFAULT_UID = "AAAAAAAAAA";
    private static final String UPDATED_UID = "BBBBBBBBBB";

    private static final String DEFAULT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_USER = "AAAAAAAAAA";
    private static final String UPDATED_USER = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/review-teams";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ReviewTeamRepository reviewTeamRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restReviewTeamMockMvc;

    private ReviewTeam reviewTeam;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ReviewTeam createEntity(EntityManager em) {
        ReviewTeam reviewTeam = new ReviewTeam().uid(DEFAULT_UID).code(DEFAULT_CODE).name(DEFAULT_NAME).user(DEFAULT_USER);
        return reviewTeam;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ReviewTeam createUpdatedEntity(EntityManager em) {
        ReviewTeam reviewTeam = new ReviewTeam().uid(UPDATED_UID).code(UPDATED_CODE).name(UPDATED_NAME).user(UPDATED_USER);
        return reviewTeam;
    }

    @BeforeEach
    public void initTest() {
        reviewTeam = createEntity(em);
    }

    @Test
    @Transactional
    void createReviewTeam() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ReviewTeam
        var returnedReviewTeam = om.readValue(
            restReviewTeamMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(reviewTeam)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ReviewTeam.class
        );

        // Validate the ReviewTeam in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertReviewTeamUpdatableFieldsEquals(returnedReviewTeam, getPersistedReviewTeam(returnedReviewTeam));
    }

    @Test
    @Transactional
    void createReviewTeamWithExistingId() throws Exception {
        // Create the ReviewTeam with an existing ID
        reviewTeam.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restReviewTeamMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(reviewTeam)))
            .andExpect(status().isBadRequest());

        // Validate the ReviewTeam in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkUidIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        reviewTeam.setUid(null);

        // Create the ReviewTeam, which fails.

        restReviewTeamMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(reviewTeam)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllReviewTeams() throws Exception {
        // Initialize the database
        reviewTeamRepository.saveAndFlush(reviewTeam);

        // Get all the reviewTeamList
        restReviewTeamMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(reviewTeam.getId().intValue())))
            .andExpect(jsonPath("$.[*].uid").value(hasItem(DEFAULT_UID)))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].user").value(hasItem(DEFAULT_USER)));
    }

    @Test
    @Transactional
    void getReviewTeam() throws Exception {
        // Initialize the database
        reviewTeamRepository.saveAndFlush(reviewTeam);

        // Get the reviewTeam
        restReviewTeamMockMvc
            .perform(get(ENTITY_API_URL_ID, reviewTeam.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(reviewTeam.getId().intValue()))
            .andExpect(jsonPath("$.uid").value(DEFAULT_UID))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.user").value(DEFAULT_USER));
    }

    @Test
    @Transactional
    void getNonExistingReviewTeam() throws Exception {
        // Get the reviewTeam
        restReviewTeamMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingReviewTeam() throws Exception {
        // Initialize the database
        reviewTeamRepository.saveAndFlush(reviewTeam);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the reviewTeam
        ReviewTeam updatedReviewTeam = reviewTeamRepository.findById(reviewTeam.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedReviewTeam are not directly saved in db
        em.detach(updatedReviewTeam);
        updatedReviewTeam.uid(UPDATED_UID).code(UPDATED_CODE).name(UPDATED_NAME).user(UPDATED_USER);

        restReviewTeamMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedReviewTeam.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedReviewTeam))
            )
            .andExpect(status().isOk());

        // Validate the ReviewTeam in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedReviewTeamToMatchAllProperties(updatedReviewTeam);
    }

    @Test
    @Transactional
    void putNonExistingReviewTeam() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        reviewTeam.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restReviewTeamMockMvc
            .perform(
                put(ENTITY_API_URL_ID, reviewTeam.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(reviewTeam))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReviewTeam in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchReviewTeam() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        reviewTeam.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReviewTeamMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(reviewTeam))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReviewTeam in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamReviewTeam() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        reviewTeam.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReviewTeamMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(reviewTeam)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ReviewTeam in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateReviewTeamWithPatch() throws Exception {
        // Initialize the database
        reviewTeamRepository.saveAndFlush(reviewTeam);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the reviewTeam using partial update
        ReviewTeam partialUpdatedReviewTeam = new ReviewTeam();
        partialUpdatedReviewTeam.setId(reviewTeam.getId());

        partialUpdatedReviewTeam.uid(UPDATED_UID).code(UPDATED_CODE).user(UPDATED_USER);

        restReviewTeamMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedReviewTeam.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedReviewTeam))
            )
            .andExpect(status().isOk());

        // Validate the ReviewTeam in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertReviewTeamUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedReviewTeam, reviewTeam),
            getPersistedReviewTeam(reviewTeam)
        );
    }

    @Test
    @Transactional
    void fullUpdateReviewTeamWithPatch() throws Exception {
        // Initialize the database
        reviewTeamRepository.saveAndFlush(reviewTeam);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the reviewTeam using partial update
        ReviewTeam partialUpdatedReviewTeam = new ReviewTeam();
        partialUpdatedReviewTeam.setId(reviewTeam.getId());

        partialUpdatedReviewTeam.uid(UPDATED_UID).code(UPDATED_CODE).name(UPDATED_NAME).user(UPDATED_USER);

        restReviewTeamMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedReviewTeam.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedReviewTeam))
            )
            .andExpect(status().isOk());

        // Validate the ReviewTeam in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertReviewTeamUpdatableFieldsEquals(partialUpdatedReviewTeam, getPersistedReviewTeam(partialUpdatedReviewTeam));
    }

    @Test
    @Transactional
    void patchNonExistingReviewTeam() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        reviewTeam.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restReviewTeamMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, reviewTeam.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(reviewTeam))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReviewTeam in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchReviewTeam() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        reviewTeam.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReviewTeamMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(reviewTeam))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReviewTeam in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamReviewTeam() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        reviewTeam.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReviewTeamMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(reviewTeam)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ReviewTeam in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteReviewTeam() throws Exception {
        // Initialize the database
        reviewTeamRepository.saveAndFlush(reviewTeam);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the reviewTeam
        restReviewTeamMockMvc
            .perform(delete(ENTITY_API_URL_ID, reviewTeam.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return reviewTeamRepository.count();
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

    protected ReviewTeam getPersistedReviewTeam(ReviewTeam reviewTeam) {
        return reviewTeamRepository.findById(reviewTeam.getId()).orElseThrow();
    }

    protected void assertPersistedReviewTeamToMatchAllProperties(ReviewTeam expectedReviewTeam) {
        assertReviewTeamAllPropertiesEquals(expectedReviewTeam, getPersistedReviewTeam(expectedReviewTeam));
    }

    protected void assertPersistedReviewTeamToMatchUpdatableProperties(ReviewTeam expectedReviewTeam) {
        assertReviewTeamAllUpdatablePropertiesEquals(expectedReviewTeam, getPersistedReviewTeam(expectedReviewTeam));
    }
}
