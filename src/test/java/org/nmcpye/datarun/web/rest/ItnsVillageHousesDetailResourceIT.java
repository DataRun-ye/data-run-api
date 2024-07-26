package org.nmcpye.datarun.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.nmcpye.datarun.domain.ItnsVillageHousesDetailAsserts.*;
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
import org.nmcpye.datarun.domain.ItnsVillage;
import org.nmcpye.datarun.domain.ItnsVillageHousesDetail;
import org.nmcpye.datarun.repository.ItnsVillageHousesDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link ItnsVillageHousesDetailResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ItnsVillageHousesDetailResourceIT {

    private static final String DEFAULT_UID = "AAAAAAAAAA";
    private static final String UPDATED_UID = "BBBBBBBBBB";

    private static final String DEFAULT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_CODE = "BBBBBBBBBB";

    private static final Long DEFAULT_COUPON_ID = 1L;
    private static final Long UPDATED_COUPON_ID = 2L;

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final Integer DEFAULT_MALE = 0;
    private static final Integer UPDATED_MALE = 1;

    private static final Integer DEFAULT_FEMALE = 0;
    private static final Integer UPDATED_FEMALE = 1;

    private static final Integer DEFAULT_PREGNANT = 0;
    private static final Integer UPDATED_PREGNANT = 1;

    private static final Integer DEFAULT_POPULATION = 0;
    private static final Integer UPDATED_POPULATION = 1;

    private static final Integer DEFAULT_MALE_CHILD = 0;
    private static final Integer UPDATED_MALE_CHILD = 1;

    private static final Integer DEFAULT_FEMALE_CHILD = 0;
    private static final Integer UPDATED_FEMALE_CHILD = 1;

    private static final Integer DEFAULT_DISPLACED = 0;
    private static final Integer UPDATED_DISPLACED = 1;

    private static final Integer DEFAULT_ITNS = 0;
    private static final Integer UPDATED_ITNS = 1;

    private static final String DEFAULT_COMMENT = "AAAAAAAAAA";
    private static final String UPDATED_COMMENT = "BBBBBBBBBB";

    private static final String DEFAULT_SUBMISSION_UUID = "AAAAAAAAAA";
    private static final String UPDATED_SUBMISSION_UUID = "BBBBBBBBBB";

    private static final String DEFAULT_HOUSE_UUID = "AAAAAAAAAA";
    private static final String UPDATED_HOUSE_UUID = "BBBBBBBBBB";

    private static final Boolean DEFAULT_DELETED = false;
    private static final Boolean UPDATED_DELETED = true;

    private static final String ENTITY_API_URL = "/api/itns-village-houses-details";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ItnsVillageHousesDetailRepository itnsVillageHousesDetailRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restItnsVillageHousesDetailMockMvc;

    private ItnsVillageHousesDetail itnsVillageHousesDetail;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ItnsVillageHousesDetail createEntity(EntityManager em) {
        ItnsVillageHousesDetail itnsVillageHousesDetail = new ItnsVillageHousesDetail()
            .uid(DEFAULT_UID)
            .code(DEFAULT_CODE)
            .couponId(DEFAULT_COUPON_ID)
            .name(DEFAULT_NAME)
            .male(DEFAULT_MALE)
            .female(DEFAULT_FEMALE)
            .pregnant(DEFAULT_PREGNANT)
            .population(DEFAULT_POPULATION)
            .maleChild(DEFAULT_MALE_CHILD)
            .femaleChild(DEFAULT_FEMALE_CHILD)
            .displaced(DEFAULT_DISPLACED)
            .itns(DEFAULT_ITNS)
            .comment(DEFAULT_COMMENT)
            .submissionUuid(DEFAULT_SUBMISSION_UUID)
            .houseUuid(DEFAULT_HOUSE_UUID)
            .deleted(DEFAULT_DELETED);
        // Add required entity
        ItnsVillage itnsVillage;
        if (TestUtil.findAll(em, ItnsVillage.class).isEmpty()) {
            itnsVillage = ItnsVillageResourceIT.createEntity(em);
            em.persist(itnsVillage);
            em.flush();
        } else {
            itnsVillage = TestUtil.findAll(em, ItnsVillage.class).get(0);
        }
        itnsVillageHousesDetail.setItnsVillage(itnsVillage);
        return itnsVillageHousesDetail;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ItnsVillageHousesDetail createUpdatedEntity(EntityManager em) {
        ItnsVillageHousesDetail itnsVillageHousesDetail = new ItnsVillageHousesDetail()
            .uid(UPDATED_UID)
            .code(UPDATED_CODE)
            .couponId(UPDATED_COUPON_ID)
            .name(UPDATED_NAME)
            .male(UPDATED_MALE)
            .female(UPDATED_FEMALE)
            .pregnant(UPDATED_PREGNANT)
            .population(UPDATED_POPULATION)
            .maleChild(UPDATED_MALE_CHILD)
            .femaleChild(UPDATED_FEMALE_CHILD)
            .displaced(UPDATED_DISPLACED)
            .itns(UPDATED_ITNS)
            .comment(UPDATED_COMMENT)
            .submissionUuid(UPDATED_SUBMISSION_UUID)
            .houseUuid(UPDATED_HOUSE_UUID)
            .deleted(UPDATED_DELETED);
        // Add required entity
        ItnsVillage itnsVillage;
        if (TestUtil.findAll(em, ItnsVillage.class).isEmpty()) {
            itnsVillage = ItnsVillageResourceIT.createUpdatedEntity(em);
            em.persist(itnsVillage);
            em.flush();
        } else {
            itnsVillage = TestUtil.findAll(em, ItnsVillage.class).get(0);
        }
        itnsVillageHousesDetail.setItnsVillage(itnsVillage);
        return itnsVillageHousesDetail;
    }

    @BeforeEach
    public void initTest() {
        itnsVillageHousesDetail = createEntity(em);
    }

    @Test
    @Transactional
    void createItnsVillageHousesDetail() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ItnsVillageHousesDetail
        var returnedItnsVillageHousesDetail = om.readValue(
            restItnsVillageHousesDetailMockMvc
                .perform(
                    post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(itnsVillageHousesDetail))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ItnsVillageHousesDetail.class
        );

        // Validate the ItnsVillageHousesDetail in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertItnsVillageHousesDetailUpdatableFieldsEquals(
            returnedItnsVillageHousesDetail,
            getPersistedItnsVillageHousesDetail(returnedItnsVillageHousesDetail)
        );
    }

    @Test
    @Transactional
    void createItnsVillageHousesDetailWithExistingId() throws Exception {
        // Create the ItnsVillageHousesDetail with an existing ID
        itnsVillageHousesDetail.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restItnsVillageHousesDetailMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(itnsVillageHousesDetail)))
            .andExpect(status().isBadRequest());

        // Validate the ItnsVillageHousesDetail in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkUidIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        itnsVillageHousesDetail.setUid(null);

        // Create the ItnsVillageHousesDetail, which fails.

        restItnsVillageHousesDetailMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(itnsVillageHousesDetail)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllItnsVillageHousesDetails() throws Exception {
        // Initialize the database
        itnsVillageHousesDetailRepository.saveAndFlush(itnsVillageHousesDetail);

        // Get all the itnsVillageHousesDetailList
        restItnsVillageHousesDetailMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(itnsVillageHousesDetail.getId().intValue())))
            .andExpect(jsonPath("$.[*].uid").value(hasItem(DEFAULT_UID)))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].couponId").value(hasItem(DEFAULT_COUPON_ID.intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].male").value(hasItem(DEFAULT_MALE)))
            .andExpect(jsonPath("$.[*].female").value(hasItem(DEFAULT_FEMALE)))
            .andExpect(jsonPath("$.[*].pregnant").value(hasItem(DEFAULT_PREGNANT)))
            .andExpect(jsonPath("$.[*].population").value(hasItem(DEFAULT_POPULATION)))
            .andExpect(jsonPath("$.[*].maleChild").value(hasItem(DEFAULT_MALE_CHILD)))
            .andExpect(jsonPath("$.[*].femaleChild").value(hasItem(DEFAULT_FEMALE_CHILD)))
            .andExpect(jsonPath("$.[*].displaced").value(hasItem(DEFAULT_DISPLACED)))
            .andExpect(jsonPath("$.[*].itns").value(hasItem(DEFAULT_ITNS)))
            .andExpect(jsonPath("$.[*].comment").value(hasItem(DEFAULT_COMMENT)))
            .andExpect(jsonPath("$.[*].submissionUuid").value(hasItem(DEFAULT_SUBMISSION_UUID)))
            .andExpect(jsonPath("$.[*].houseUuid").value(hasItem(DEFAULT_HOUSE_UUID)))
            .andExpect(jsonPath("$.[*].deleted").value(hasItem(DEFAULT_DELETED.booleanValue())));
    }

    @Test
    @Transactional
    void getItnsVillageHousesDetail() throws Exception {
        // Initialize the database
        itnsVillageHousesDetailRepository.saveAndFlush(itnsVillageHousesDetail);

        // Get the itnsVillageHousesDetail
        restItnsVillageHousesDetailMockMvc
            .perform(get(ENTITY_API_URL_ID, itnsVillageHousesDetail.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(itnsVillageHousesDetail.getId().intValue()))
            .andExpect(jsonPath("$.uid").value(DEFAULT_UID))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.couponId").value(DEFAULT_COUPON_ID.intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.male").value(DEFAULT_MALE))
            .andExpect(jsonPath("$.female").value(DEFAULT_FEMALE))
            .andExpect(jsonPath("$.pregnant").value(DEFAULT_PREGNANT))
            .andExpect(jsonPath("$.population").value(DEFAULT_POPULATION))
            .andExpect(jsonPath("$.maleChild").value(DEFAULT_MALE_CHILD))
            .andExpect(jsonPath("$.femaleChild").value(DEFAULT_FEMALE_CHILD))
            .andExpect(jsonPath("$.displaced").value(DEFAULT_DISPLACED))
            .andExpect(jsonPath("$.itns").value(DEFAULT_ITNS))
            .andExpect(jsonPath("$.comment").value(DEFAULT_COMMENT))
            .andExpect(jsonPath("$.submissionUuid").value(DEFAULT_SUBMISSION_UUID))
            .andExpect(jsonPath("$.houseUuid").value(DEFAULT_HOUSE_UUID))
            .andExpect(jsonPath("$.deleted").value(DEFAULT_DELETED.booleanValue()));
    }

    @Test
    @Transactional
    void getNonExistingItnsVillageHousesDetail() throws Exception {
        // Get the itnsVillageHousesDetail
        restItnsVillageHousesDetailMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingItnsVillageHousesDetail() throws Exception {
        // Initialize the database
        itnsVillageHousesDetailRepository.saveAndFlush(itnsVillageHousesDetail);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the itnsVillageHousesDetail
        ItnsVillageHousesDetail updatedItnsVillageHousesDetail = itnsVillageHousesDetailRepository
            .findById(itnsVillageHousesDetail.getId())
            .orElseThrow();
        // Disconnect from session so that the updates on updatedItnsVillageHousesDetail are not directly saved in db
        em.detach(updatedItnsVillageHousesDetail);
        updatedItnsVillageHousesDetail
            .uid(UPDATED_UID)
            .code(UPDATED_CODE)
            .couponId(UPDATED_COUPON_ID)
            .name(UPDATED_NAME)
            .male(UPDATED_MALE)
            .female(UPDATED_FEMALE)
            .pregnant(UPDATED_PREGNANT)
            .population(UPDATED_POPULATION)
            .maleChild(UPDATED_MALE_CHILD)
            .femaleChild(UPDATED_FEMALE_CHILD)
            .displaced(UPDATED_DISPLACED)
            .itns(UPDATED_ITNS)
            .comment(UPDATED_COMMENT)
            .submissionUuid(UPDATED_SUBMISSION_UUID)
            .houseUuid(UPDATED_HOUSE_UUID)
            .deleted(UPDATED_DELETED);

        restItnsVillageHousesDetailMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedItnsVillageHousesDetail.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedItnsVillageHousesDetail))
            )
            .andExpect(status().isOk());

        // Validate the ItnsVillageHousesDetail in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedItnsVillageHousesDetailToMatchAllProperties(updatedItnsVillageHousesDetail);
    }

    @Test
    @Transactional
    void putNonExistingItnsVillageHousesDetail() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        itnsVillageHousesDetail.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restItnsVillageHousesDetailMockMvc
            .perform(
                put(ENTITY_API_URL_ID, itnsVillageHousesDetail.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(itnsVillageHousesDetail))
            )
            .andExpect(status().isBadRequest());

        // Validate the ItnsVillageHousesDetail in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchItnsVillageHousesDetail() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        itnsVillageHousesDetail.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restItnsVillageHousesDetailMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(itnsVillageHousesDetail))
            )
            .andExpect(status().isBadRequest());

        // Validate the ItnsVillageHousesDetail in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamItnsVillageHousesDetail() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        itnsVillageHousesDetail.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restItnsVillageHousesDetailMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(itnsVillageHousesDetail)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ItnsVillageHousesDetail in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateItnsVillageHousesDetailWithPatch() throws Exception {
        // Initialize the database
        itnsVillageHousesDetailRepository.saveAndFlush(itnsVillageHousesDetail);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the itnsVillageHousesDetail using partial update
        ItnsVillageHousesDetail partialUpdatedItnsVillageHousesDetail = new ItnsVillageHousesDetail();
        partialUpdatedItnsVillageHousesDetail.setId(itnsVillageHousesDetail.getId());

        partialUpdatedItnsVillageHousesDetail
            .code(UPDATED_CODE)
            .population(UPDATED_POPULATION)
            .maleChild(UPDATED_MALE_CHILD)
            .femaleChild(UPDATED_FEMALE_CHILD)
            .itns(UPDATED_ITNS)
            .comment(UPDATED_COMMENT)
            .houseUuid(UPDATED_HOUSE_UUID);

        restItnsVillageHousesDetailMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedItnsVillageHousesDetail.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedItnsVillageHousesDetail))
            )
            .andExpect(status().isOk());

        // Validate the ItnsVillageHousesDetail in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertItnsVillageHousesDetailUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedItnsVillageHousesDetail, itnsVillageHousesDetail),
            getPersistedItnsVillageHousesDetail(itnsVillageHousesDetail)
        );
    }

    @Test
    @Transactional
    void fullUpdateItnsVillageHousesDetailWithPatch() throws Exception {
        // Initialize the database
        itnsVillageHousesDetailRepository.saveAndFlush(itnsVillageHousesDetail);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the itnsVillageHousesDetail using partial update
        ItnsVillageHousesDetail partialUpdatedItnsVillageHousesDetail = new ItnsVillageHousesDetail();
        partialUpdatedItnsVillageHousesDetail.setId(itnsVillageHousesDetail.getId());

        partialUpdatedItnsVillageHousesDetail
            .uid(UPDATED_UID)
            .code(UPDATED_CODE)
            .couponId(UPDATED_COUPON_ID)
            .name(UPDATED_NAME)
            .male(UPDATED_MALE)
            .female(UPDATED_FEMALE)
            .pregnant(UPDATED_PREGNANT)
            .population(UPDATED_POPULATION)
            .maleChild(UPDATED_MALE_CHILD)
            .femaleChild(UPDATED_FEMALE_CHILD)
            .displaced(UPDATED_DISPLACED)
            .itns(UPDATED_ITNS)
            .comment(UPDATED_COMMENT)
            .submissionUuid(UPDATED_SUBMISSION_UUID)
            .houseUuid(UPDATED_HOUSE_UUID)
            .deleted(UPDATED_DELETED);

        restItnsVillageHousesDetailMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedItnsVillageHousesDetail.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedItnsVillageHousesDetail))
            )
            .andExpect(status().isOk());

        // Validate the ItnsVillageHousesDetail in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertItnsVillageHousesDetailUpdatableFieldsEquals(
            partialUpdatedItnsVillageHousesDetail,
            getPersistedItnsVillageHousesDetail(partialUpdatedItnsVillageHousesDetail)
        );
    }

    @Test
    @Transactional
    void patchNonExistingItnsVillageHousesDetail() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        itnsVillageHousesDetail.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restItnsVillageHousesDetailMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, itnsVillageHousesDetail.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(itnsVillageHousesDetail))
            )
            .andExpect(status().isBadRequest());

        // Validate the ItnsVillageHousesDetail in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchItnsVillageHousesDetail() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        itnsVillageHousesDetail.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restItnsVillageHousesDetailMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(itnsVillageHousesDetail))
            )
            .andExpect(status().isBadRequest());

        // Validate the ItnsVillageHousesDetail in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamItnsVillageHousesDetail() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        itnsVillageHousesDetail.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restItnsVillageHousesDetailMockMvc
            .perform(
                patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(itnsVillageHousesDetail))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ItnsVillageHousesDetail in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteItnsVillageHousesDetail() throws Exception {
        // Initialize the database
        itnsVillageHousesDetailRepository.saveAndFlush(itnsVillageHousesDetail);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the itnsVillageHousesDetail
        restItnsVillageHousesDetailMockMvc
            .perform(delete(ENTITY_API_URL_ID, itnsVillageHousesDetail.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return itnsVillageHousesDetailRepository.count();
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

    protected ItnsVillageHousesDetail getPersistedItnsVillageHousesDetail(ItnsVillageHousesDetail itnsVillageHousesDetail) {
        return itnsVillageHousesDetailRepository.findById(itnsVillageHousesDetail.getId()).orElseThrow();
    }

    protected void assertPersistedItnsVillageHousesDetailToMatchAllProperties(ItnsVillageHousesDetail expectedItnsVillageHousesDetail) {
        assertItnsVillageHousesDetailAllPropertiesEquals(
            expectedItnsVillageHousesDetail,
            getPersistedItnsVillageHousesDetail(expectedItnsVillageHousesDetail)
        );
    }

    protected void assertPersistedItnsVillageHousesDetailToMatchUpdatableProperties(
        ItnsVillageHousesDetail expectedItnsVillageHousesDetail
    ) {
        assertItnsVillageHousesDetailAllUpdatablePropertiesEquals(
            expectedItnsVillageHousesDetail,
            getPersistedItnsVillageHousesDetail(expectedItnsVillageHousesDetail)
        );
    }
}
