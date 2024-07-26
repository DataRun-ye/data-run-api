package org.nmcpye.datarun.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.nmcpye.datarun.domain.RefreshTokenAsserts.*;
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
import org.nmcpye.datarun.domain.RefreshToken;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.repository.RefreshTokenRepository;
import org.nmcpye.datarun.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link RefreshTokenResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class RefreshTokenResourceIT {

    private static final String DEFAULT_UID = "AAAAAAAAAA";
    private static final String UPDATED_UID = "BBBBBBBBBB";

    private static final String DEFAULT_TOKEN = "AAAAAAAAAA";
    private static final String UPDATED_TOKEN = "BBBBBBBBBB";

    private static final Instant DEFAULT_EXPIRY_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_EXPIRY_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/refresh-tokens";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepositoryMock;

    @Mock
    private RefreshTokenService refreshTokenServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restRefreshTokenMockMvc;

    private RefreshToken refreshToken;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RefreshToken createEntity(EntityManager em) {
        RefreshToken refreshToken = new RefreshToken().uid(DEFAULT_UID).token(DEFAULT_TOKEN).expiryDate(DEFAULT_EXPIRY_DATE);
        // Add required entity
        User user = UserResourceIT.createEntity(em);
        em.persist(user);
        em.flush();
        refreshToken.setUser(user);
        return refreshToken;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RefreshToken createUpdatedEntity(EntityManager em) {
        RefreshToken refreshToken = new RefreshToken().uid(UPDATED_UID).token(UPDATED_TOKEN).expiryDate(UPDATED_EXPIRY_DATE);
        // Add required entity
        User user = UserResourceIT.createEntity(em);
        em.persist(user);
        em.flush();
        refreshToken.setUser(user);
        return refreshToken;
    }

    @BeforeEach
    public void initTest() {
        refreshToken = createEntity(em);
    }

    @Test
    @Transactional
    void createRefreshToken() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the RefreshToken
        var returnedRefreshToken = om.readValue(
            restRefreshTokenMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(refreshToken)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            RefreshToken.class
        );

        // Validate the RefreshToken in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertRefreshTokenUpdatableFieldsEquals(returnedRefreshToken, getPersistedRefreshToken(returnedRefreshToken));
    }

    @Test
    @Transactional
    void createRefreshTokenWithExistingId() throws Exception {
        // Create the RefreshToken with an existing ID
        refreshToken.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restRefreshTokenMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(refreshToken)))
            .andExpect(status().isBadRequest());

        // Validate the RefreshToken in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTokenIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        refreshToken.setToken(null);

        // Create the RefreshToken, which fails.

        restRefreshTokenMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(refreshToken)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllRefreshTokens() throws Exception {
        // Initialize the database
        refreshTokenRepository.saveAndFlush(refreshToken);

        // Get all the refreshTokenList
        restRefreshTokenMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(refreshToken.getId().intValue())))
            .andExpect(jsonPath("$.[*].uid").value(hasItem(DEFAULT_UID)))
            .andExpect(jsonPath("$.[*].token").value(hasItem(DEFAULT_TOKEN)))
            .andExpect(jsonPath("$.[*].expiryDate").value(hasItem(DEFAULT_EXPIRY_DATE.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRefreshTokensWithEagerRelationshipsIsEnabled() throws Exception {
        when(refreshTokenServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRefreshTokenMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(refreshTokenServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRefreshTokensWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(refreshTokenServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRefreshTokenMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(refreshTokenRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getRefreshToken() throws Exception {
        // Initialize the database
        refreshTokenRepository.saveAndFlush(refreshToken);

        // Get the refreshToken
        restRefreshTokenMockMvc
            .perform(get(ENTITY_API_URL_ID, refreshToken.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(refreshToken.getId().intValue()))
            .andExpect(jsonPath("$.uid").value(DEFAULT_UID))
            .andExpect(jsonPath("$.token").value(DEFAULT_TOKEN))
            .andExpect(jsonPath("$.expiryDate").value(DEFAULT_EXPIRY_DATE.toString()));
    }

    @Test
    @Transactional
    void getNonExistingRefreshToken() throws Exception {
        // Get the refreshToken
        restRefreshTokenMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingRefreshToken() throws Exception {
        // Initialize the database
        refreshTokenRepository.saveAndFlush(refreshToken);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the refreshToken
        RefreshToken updatedRefreshToken = refreshTokenRepository.findById(refreshToken.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedRefreshToken are not directly saved in db
        em.detach(updatedRefreshToken);
        updatedRefreshToken.uid(UPDATED_UID).token(UPDATED_TOKEN).expiryDate(UPDATED_EXPIRY_DATE);

        restRefreshTokenMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedRefreshToken.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedRefreshToken))
            )
            .andExpect(status().isOk());

        // Validate the RefreshToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedRefreshTokenToMatchAllProperties(updatedRefreshToken);
    }

    @Test
    @Transactional
    void putNonExistingRefreshToken() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        refreshToken.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRefreshTokenMockMvc
            .perform(
                put(ENTITY_API_URL_ID, refreshToken.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(refreshToken))
            )
            .andExpect(status().isBadRequest());

        // Validate the RefreshToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchRefreshToken() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        refreshToken.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRefreshTokenMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(refreshToken))
            )
            .andExpect(status().isBadRequest());

        // Validate the RefreshToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamRefreshToken() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        refreshToken.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRefreshTokenMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(refreshToken)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the RefreshToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateRefreshTokenWithPatch() throws Exception {
        // Initialize the database
        refreshTokenRepository.saveAndFlush(refreshToken);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the refreshToken using partial update
        RefreshToken partialUpdatedRefreshToken = new RefreshToken();
        partialUpdatedRefreshToken.setId(refreshToken.getId());

        partialUpdatedRefreshToken.uid(UPDATED_UID);

        restRefreshTokenMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRefreshToken.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRefreshToken))
            )
            .andExpect(status().isOk());

        // Validate the RefreshToken in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRefreshTokenUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedRefreshToken, refreshToken),
            getPersistedRefreshToken(refreshToken)
        );
    }

    @Test
    @Transactional
    void fullUpdateRefreshTokenWithPatch() throws Exception {
        // Initialize the database
        refreshTokenRepository.saveAndFlush(refreshToken);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the refreshToken using partial update
        RefreshToken partialUpdatedRefreshToken = new RefreshToken();
        partialUpdatedRefreshToken.setId(refreshToken.getId());

        partialUpdatedRefreshToken.uid(UPDATED_UID).token(UPDATED_TOKEN).expiryDate(UPDATED_EXPIRY_DATE);

        restRefreshTokenMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRefreshToken.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRefreshToken))
            )
            .andExpect(status().isOk());

        // Validate the RefreshToken in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRefreshTokenUpdatableFieldsEquals(partialUpdatedRefreshToken, getPersistedRefreshToken(partialUpdatedRefreshToken));
    }

    @Test
    @Transactional
    void patchNonExistingRefreshToken() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        refreshToken.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRefreshTokenMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, refreshToken.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(refreshToken))
            )
            .andExpect(status().isBadRequest());

        // Validate the RefreshToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchRefreshToken() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        refreshToken.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRefreshTokenMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(refreshToken))
            )
            .andExpect(status().isBadRequest());

        // Validate the RefreshToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamRefreshToken() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        refreshToken.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRefreshTokenMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(refreshToken)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the RefreshToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteRefreshToken() throws Exception {
        // Initialize the database
        refreshTokenRepository.saveAndFlush(refreshToken);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the refreshToken
        restRefreshTokenMockMvc
            .perform(delete(ENTITY_API_URL_ID, refreshToken.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return refreshTokenRepository.count();
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

    protected RefreshToken getPersistedRefreshToken(RefreshToken refreshToken) {
        return refreshTokenRepository.findById(refreshToken.getId()).orElseThrow();
    }

    protected void assertPersistedRefreshTokenToMatchAllProperties(RefreshToken expectedRefreshToken) {
        assertRefreshTokenAllPropertiesEquals(expectedRefreshToken, getPersistedRefreshToken(expectedRefreshToken));
    }

    protected void assertPersistedRefreshTokenToMatchUpdatableProperties(RefreshToken expectedRefreshToken) {
        assertRefreshTokenAllUpdatablePropertiesEquals(expectedRefreshToken, getPersistedRefreshToken(expectedRefreshToken));
    }
}
