package org.nmcpye.datarun.party.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
import org.nmcpye.datarun.party.entities.Party;
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

    /**
     * Generic upsert method.
     * We use the ID as the source of truth. If it exists, we update label/meta.
     */
    @Transactional
    public void syncParty(String id, String uid, String code, String type, String name, Map<String, String> label, String parentId,
                          List<String> tags, Map<String, Object> meta) {

        final var partyParent = partyRepo.findBySourceId(parentId)
            .orElse(null);
        final UUID partyParentId = partyParent != null ? partyParent.getId() : null;

        partyRepo.findBySourceId(id).ifPresentOrElse(
            // Case A: Update existing
            existing -> {
                boolean changed = !existing.getLabel().equals(label)
                    || !Objects.equals(existing.getParentId(), parentId);
                if (changed) {
                    existing.setName(name);
                    existing.setCode(code);
                    existing.setLabel(label);
                    existing.setType(type);
                    existing.setSourceType(SourceType.INTERNAL);
                    existing.setParentId(partyParentId);
                    existing.setProperties(meta);
                    existing.setTags(tags);
                    existing.setLastModifiedDate(Instant.now());
                    log.debug("Updated Party index: {} ({})", label, uid);
                }
            },
            // Case B: Create new
            () -> {
                Party newParty = Party.builder()
                    .id(UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8)))      // Keeps the same ULID as the source
                    .uid(uid)    // Keeps the same UID as the source
                    .code(code)
                    .type(type)
                    .name(name)
                    .parentId(partyParentId)
                    .sourceType(SourceType.INTERNAL)
                    .sourceId(id)
                    .properties(meta)
                    .tags(tags) // Default empty
                    .createdDate(Instant.now())
                    .build();
                newParty.setLabel(label);

                partyRepo.persist(newParty);
                log.info("Indexed new Party: {} ({})", name, uid);
            }
        );
    }
}
