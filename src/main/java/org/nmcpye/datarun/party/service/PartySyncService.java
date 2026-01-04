package org.nmcpye.datarun.party.service;

import jakarta.transaction.Transactional;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
import org.nmcpye.datarun.jpa.team.repository.TeamRepository;
import org.nmcpye.datarun.party.entities.Party;
import org.nmcpye.datarun.party.entities.Party.PartyType;
import org.nmcpye.datarun.party.entities.Party.SourceType;
import org.nmcpye.datarun.party.repository.PartyRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartySyncService {

    private final PartyRepository partyRepo;
    private final OrgUnitRepository orgUnitRepository;
    private final TeamRepository teamRepo;

    public void updateUserPartyTags(String s) {
    }

    @Data
    @Builder
    @Accessors(fluent = true)
    public static class ToSyncParty {
        private String id;
        private String uid;
        private String code;
        private SourceType sourceType;
        private String name;
        private Map<String, String> label;
        private String parentId;
        private List<String> tags;
        private Map<String, Object> meta;
    }

    /**
     * Generic upsert method.
     * We use the ID as the source of truth. If it exists, we update label/meta.
     */
    @Transactional
    public void syncParty(ToSyncParty toSyncParty) {
        switch (toSyncParty.sourceType) {
            case ORG_UNIT -> syncOrgUnit(toSyncParty);
            case TEAM -> syncTeam(toSyncParty);
            case USER -> syncUser(toSyncParty);
        }
    }

    private void syncOrgUnit(ToSyncParty toSyncParty) {
        final var partyParent = partyRepo.findBySourceId(toSyncParty.parentId)
            .orElse(null);
        final UUID partyParentId = partyParent != null ? partyParent.getId() : null;

        partyRepo.findByUid(toSyncParty.uid).ifPresentOrElse(
            // Case A: Update existing
            existing -> {
                boolean changed = !Objects.equals(existing.getParentId(), partyParentId)
                    || !Objects.equals(existing.getLabel(), toSyncParty.label)
                    || !Objects.equals(existing.getTags(), toSyncParty.tags)
                    || !Objects.equals(existing.getProperties(), toSyncParty.meta);
                log.debug("Updating Party index: {} ({})", toSyncParty.label, toSyncParty.uid);
                if (changed && Objects.equals(existing.getSourceType(),SourceType.ORG_UNIT)) {
                    existing.setName(toSyncParty.name);
                    existing.setCode(toSyncParty.code);
                    existing.setLabel(toSyncParty.label);
                    existing.setType(PartyType.INTERNAL);
                    existing.setSourceType(SourceType.ORG_UNIT);
                    existing.setParentId(partyParentId);
                    existing.setProperties(toSyncParty.meta);
                    existing.setTags(toSyncParty.tags);
                    existing.setLastModifiedDate(Instant.now());
                    log.debug("Updated Party index: {} ({})", toSyncParty.label, toSyncParty.uid);
                }
            },
            // Case B: Create new
            () -> {
                Party newParty = Party.builder()
                    .id(UUID.nameUUIDFromBytes(toSyncParty.id.getBytes(StandardCharsets.UTF_8)))      // Keeps the same ULID as the source
                    .uid(toSyncParty.uid)    // Keeps the same UID as the source
                    .code(toSyncParty.code)
                    .type(PartyType.INTERNAL)
                    .name(toSyncParty.name)
                    .parentId(partyParentId)
                    .sourceType(SourceType.ORG_UNIT)
                    .sourceId(toSyncParty.id)
                    .properties(toSyncParty.meta)
                    .tags(toSyncParty.tags) // Default empty
                    .createdDate(Instant.now())
                    .lastModifiedDate(Instant.now())
                    .build();
                newParty.setLabel(toSyncParty.label);

                partyRepo.persistAndFlush(newParty);
//                log.info("Indexed new Party: {} ({})", toSyncParty.name, toSyncParty.uid);
            }
        );
    }

    private void syncTeam(ToSyncParty toSyncParty) {
        partyRepo.findByUid(toSyncParty.uid).ifPresentOrElse(
            // Case A: Update existing
            existing -> {
                boolean changed = !Objects.equals(existing.getLabel(), toSyncParty.label)
                    || !Objects.equals(existing.getTags(), toSyncParty.tags)
                    || !Objects.equals(existing.getProperties(), toSyncParty.meta);
                log.debug("Updating Party index: {} ({})", toSyncParty.label, toSyncParty.uid);
                if (changed && Objects.equals(existing.getSourceType(), SourceType.TEAM)) {
                    existing.setName(toSyncParty.name);
                    existing.setCode(toSyncParty.code);
                    existing.setLabel(toSyncParty.label);
                    existing.setType(PartyType.INTERNAL);
                    existing.setSourceType(SourceType.TEAM);
                    existing.setParentId(null);
                    existing.setProperties(toSyncParty.meta);
                    existing.setTags(toSyncParty.tags);
                    existing.setLastModifiedDate(Instant.now());
                    log.debug("Updated Party index: {} ({})", toSyncParty.label, toSyncParty.uid);
                }
            },
            // Case B: Create new
            () -> {
                Party newParty = Party.builder()
                    .id(UUID.nameUUIDFromBytes(toSyncParty.id.getBytes(StandardCharsets.UTF_8)))      // Keeps the same ULID as the source
                    .uid(toSyncParty.uid)    // Keeps the same UID as the source
                    .code(toSyncParty.code)
                    .type(PartyType.INTERNAL)
                    .name(toSyncParty.name)
                    .parentId(null)
                    .sourceType(SourceType.TEAM)
                    .sourceId(toSyncParty.id)
                    .properties(toSyncParty.meta)
                    .tags(toSyncParty.tags) // Default empty
                    .createdDate(Instant.now())
                    .lastModifiedDate(Instant.now())
                    .build();
                newParty.setLabel(toSyncParty.label);

                partyRepo.persistAndFlush(newParty);
//                log.info("Indexed new Party: {} ({})", toSyncParty.name, toSyncParty.uid);
            }
        );
    }

    private void syncUser(ToSyncParty toSyncParty) {
        partyRepo.findByUid(toSyncParty.uid).ifPresentOrElse(
            // Case A: Update existing
            existing -> {
                boolean changed = !Objects.equals(existing.getLabel(), toSyncParty.label)
                    || !Objects.equals(existing.getTags(), toSyncParty.tags)
                    || !Objects.equals(existing.getProperties(), toSyncParty.meta);
                log.debug("Updating Party index: {} ({})", toSyncParty.label, toSyncParty.uid);
                if (changed && Objects.equals(existing.getSourceType(),SourceType.USER)) {
                    existing.setName(toSyncParty.name);
                    existing.setCode(toSyncParty.code);
                    existing.setLabel(toSyncParty.label);
                    existing.setType(PartyType.INTERNAL);
                    existing.setSourceType(SourceType.USER);
                    existing.setParentId(null);
                    existing.setProperties(toSyncParty.meta);
                    existing.setTags(toSyncParty.tags);
                    existing.setLastModifiedDate(Instant.now());
                    log.debug("Updated Party index: {} ({})", toSyncParty.label, toSyncParty.uid);
                }
            },
            // Case B: Create new
            () -> {
                Party newParty = Party.builder()
                    .id(UUID.nameUUIDFromBytes(toSyncParty.id.getBytes(StandardCharsets.UTF_8)))      // Keeps the same ULID as the source
                    .uid(toSyncParty.uid)    // Keeps the same UID as the source
                    .code(toSyncParty.code)
                    .type(PartyType.INTERNAL)
                    .name(toSyncParty.name)
                    .parentId(null)
                    .sourceType(SourceType.USER)
                    .sourceId(toSyncParty.id)
                    .properties(toSyncParty.meta)
                    .tags(toSyncParty.tags) // Default empty
                    .createdDate(Instant.now())
                    .lastModifiedDate(Instant.now())
                    .build();
                newParty.setLabel(toSyncParty.label);

                partyRepo.persistAndFlush(newParty);
//                log.info("Indexed new Party: {} ({})", toSyncParty.name, toSyncParty.uid);
            }
        );
    }
}
