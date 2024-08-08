package org.nmcpye.datarun.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.nmcpye.datarun.domain.WarehouseItemAsserts.*;
import static org.nmcpye.datarun.web.rest.TestUtil.createUpdateProxyForBean;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.domain.WarehouseItem;
import org.nmcpye.datarun.repository.WarehouseItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link WarehouseItemResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class WarehouseItemResourceIT {

    private static final String DEFAULT_UID = "AAAAAAAAAA";
    private static final String UPDATED_UID = "BBBBBBBBBB";

    private static final String DEFAULT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/warehouse-items";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private WarehouseItemRepository warehouseItemRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restWarehouseItemMockMvc;

    private WarehouseItem warehouseItem;

    private WarehouseItem insertedWarehouseItem;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static WarehouseItem createEntity(EntityManager em) {
        WarehouseItem warehouseItem = new WarehouseItem()
            .uid(DEFAULT_UID)
            .code(DEFAULT_CODE)
            .name(DEFAULT_NAME)
            .description(DEFAULT_DESCRIPTION);
        return warehouseItem;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static WarehouseItem createUpdatedEntity(EntityManager em) {
        WarehouseItem warehouseItem = new WarehouseItem()
            .uid(UPDATED_UID)
            .code(UPDATED_CODE)
            .name(UPDATED_NAME)
            .description(UPDATED_DESCRIPTION);
        return warehouseItem;
    }

    @BeforeEach
    public void initTest() {
        warehouseItem = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedWarehouseItem != null) {
            warehouseItemRepository.delete(insertedWarehouseItem);
            insertedWarehouseItem = null;
        }
    }

    @Test
    @Transactional
    void createWarehouseItem() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the WarehouseItem
        var returnedWarehouseItem = om.readValue(
            restWarehouseItemMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(warehouseItem)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            WarehouseItem.class
        );

        // Validate the WarehouseItem in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertWarehouseItemUpdatableFieldsEquals(returnedWarehouseItem, getPersistedWarehouseItem(returnedWarehouseItem));

        insertedWarehouseItem = returnedWarehouseItem;
    }

    @Test
    @Transactional
    void createWarehouseItemWithExistingId() throws Exception {
        // Create the WarehouseItem with an existing ID
        warehouseItem.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restWarehouseItemMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(warehouseItem)))
            .andExpect(status().isBadRequest());

        // Validate the WarehouseItem in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkUidIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        warehouseItem.setUid(null);

        // Create the WarehouseItem, which fails.

        restWarehouseItemMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(warehouseItem)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllWarehouseItems() throws Exception {
        // Initialize the database
        insertedWarehouseItem = warehouseItemRepository.saveAndFlush(warehouseItem);

        // Get all the warehouseItemList
        restWarehouseItemMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(warehouseItem.getId().intValue())))
            .andExpect(jsonPath("$.[*].uid").value(hasItem(DEFAULT_UID)))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)));
    }

    @Test
    @Transactional
    void getWarehouseItem() throws Exception {
        // Initialize the database
        insertedWarehouseItem = warehouseItemRepository.saveAndFlush(warehouseItem);

        // Get the warehouseItem
        restWarehouseItemMockMvc
            .perform(get(ENTITY_API_URL_ID, warehouseItem.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(warehouseItem.getId().intValue()))
            .andExpect(jsonPath("$.uid").value(DEFAULT_UID))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION));
    }

    @Test
    @Transactional
    void getNonExistingWarehouseItem() throws Exception {
        // Get the warehouseItem
        restWarehouseItemMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingWarehouseItem() throws Exception {
        // Initialize the database
        insertedWarehouseItem = warehouseItemRepository.saveAndFlush(warehouseItem);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the warehouseItem
        WarehouseItem updatedWarehouseItem = warehouseItemRepository.findById(warehouseItem.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedWarehouseItem are not directly saved in db
        em.detach(updatedWarehouseItem);
        updatedWarehouseItem.uid(UPDATED_UID).code(UPDATED_CODE).name(UPDATED_NAME).description(UPDATED_DESCRIPTION);

        restWarehouseItemMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedWarehouseItem.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedWarehouseItem))
            )
            .andExpect(status().isOk());

        // Validate the WarehouseItem in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedWarehouseItemToMatchAllProperties(updatedWarehouseItem);
    }

    @Test
    @Transactional
    void putNonExistingWarehouseItem() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        warehouseItem.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restWarehouseItemMockMvc
            .perform(
                put(ENTITY_API_URL_ID, warehouseItem.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(warehouseItem))
            )
            .andExpect(status().isBadRequest());

        // Validate the WarehouseItem in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchWarehouseItem() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        warehouseItem.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restWarehouseItemMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(warehouseItem))
            )
            .andExpect(status().isBadRequest());

        // Validate the WarehouseItem in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamWarehouseItem() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        warehouseItem.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restWarehouseItemMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(warehouseItem)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the WarehouseItem in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateWarehouseItemWithPatch() throws Exception {
        // Initialize the database
        insertedWarehouseItem = warehouseItemRepository.saveAndFlush(warehouseItem);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the warehouseItem using partial update
        WarehouseItem partialUpdatedWarehouseItem = new WarehouseItem();
        partialUpdatedWarehouseItem.setId(warehouseItem.getId());

        partialUpdatedWarehouseItem.name(UPDATED_NAME).description(UPDATED_DESCRIPTION);

        restWarehouseItemMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedWarehouseItem.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedWarehouseItem))
            )
            .andExpect(status().isOk());

        // Validate the WarehouseItem in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertWarehouseItemUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedWarehouseItem, warehouseItem),
            getPersistedWarehouseItem(warehouseItem)
        );
    }

    @Test
    @Transactional
    void fullUpdateWarehouseItemWithPatch() throws Exception {
        // Initialize the database
        insertedWarehouseItem = warehouseItemRepository.saveAndFlush(warehouseItem);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the warehouseItem using partial update
        WarehouseItem partialUpdatedWarehouseItem = new WarehouseItem();
        partialUpdatedWarehouseItem.setId(warehouseItem.getId());

        partialUpdatedWarehouseItem.uid(UPDATED_UID).code(UPDATED_CODE).name(UPDATED_NAME).description(UPDATED_DESCRIPTION);

        restWarehouseItemMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedWarehouseItem.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedWarehouseItem))
            )
            .andExpect(status().isOk());

        // Validate the WarehouseItem in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertWarehouseItemUpdatableFieldsEquals(partialUpdatedWarehouseItem, getPersistedWarehouseItem(partialUpdatedWarehouseItem));
    }

    @Test
    @Transactional
    void patchNonExistingWarehouseItem() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        warehouseItem.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restWarehouseItemMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, warehouseItem.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(warehouseItem))
            )
            .andExpect(status().isBadRequest());

        // Validate the WarehouseItem in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchWarehouseItem() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        warehouseItem.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restWarehouseItemMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(warehouseItem))
            )
            .andExpect(status().isBadRequest());

        // Validate the WarehouseItem in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamWarehouseItem() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        warehouseItem.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restWarehouseItemMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(warehouseItem)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the WarehouseItem in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteWarehouseItem() throws Exception {
        // Initialize the database
        insertedWarehouseItem = warehouseItemRepository.saveAndFlush(warehouseItem);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the warehouseItem
        restWarehouseItemMockMvc
            .perform(delete(ENTITY_API_URL_ID, warehouseItem.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return warehouseItemRepository.count();
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

    protected WarehouseItem getPersistedWarehouseItem(WarehouseItem warehouseItem) {
        return warehouseItemRepository.findById(warehouseItem.getId()).orElseThrow();
    }

    protected void assertPersistedWarehouseItemToMatchAllProperties(WarehouseItem expectedWarehouseItem) {
        assertWarehouseItemAllPropertiesEquals(expectedWarehouseItem, getPersistedWarehouseItem(expectedWarehouseItem));
    }

    protected void assertPersistedWarehouseItemToMatchUpdatableProperties(WarehouseItem expectedWarehouseItem) {
        assertWarehouseItemAllUpdatablePropertiesEquals(expectedWarehouseItem, getPersistedWarehouseItem(expectedWarehouseItem));
    }
}
