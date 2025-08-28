package org.nmcpye.datarun.jpa.common;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generic upsert service. Subclasses implement mergeIntoExisting to control update semantics.
 *
 * @param <E>  entity type (extends BaseEntity)
 * @param <R>  repository type extending BaseJpaRepository<E, ID>
 * @author Hamza Assada
 * @since 28/08/2025
 */
@RequiredArgsConstructor
public abstract class BaseUpsertService<E extends JpaIdentifiableObject, R extends JpaIdentifiableRepository<E>> {

    protected final R repo;
    protected final int bulkChunkSize = 500;
    protected final ApplicationEventPublisher publisher;
    /**
     * Upsert single entity. Runs inside a transaction because BaseJpaRepository.persistAndFlush/updateAndFlush
     * are not themselves transactional.
     */
    @Transactional
    public E upsert(E incoming) {
        Objects.requireNonNull(incoming, "incoming entity is required");
        String uid = incoming.getUid();
        if (uid == null || uid.isBlank()) {
            throw new IllegalArgumentException("uid is required for upsert");
        }

        Optional<E> existingOpt = repo.findByUid(uid);
        if (existingOpt.isPresent()) {
            E existing = existingOpt.get();
            mergeIntoExisting(incoming, existing);
            setAuditOnUpdate(existing);
            return repo.updateAndFlush(existing);
        } else {
            if (incoming.getId() == null) incoming.setId(CodeGenerator.nextUlid());
            setAuditOnCreate(incoming);
            return repo.persistAndFlush(incoming);
        }
    }

    /**
     * Bulk upsert. Dedupes incoming by uid (last item wins). Runs in a transaction for the whole bulk call.
     * For very large payloads you may want to call this per-chunk from the controller.
     */
    @Transactional
    public List<E> upsertAll(List<E> incomingAll) {
        if (incomingAll == null || incomingAll.isEmpty()) return Collections.emptyList();

        // 1) dedupe incoming by uid (last-wins)
        Map<String, E> incomingByUid = incomingAll.stream()
            .filter(e -> e.getUid() != null && !e.getUid().isBlank())
            .collect(Collectors.toMap(E::getUid, Function.identity(), (oldVal, newVal) -> newVal));

        List<String> uids = new ArrayList<>(incomingByUid.keySet());

        // 2) load existing in a single query
        List<E> existingList = repo.findAllByUidIn(uids);
        Map<String, E> existingByUid = existingList.stream()
            .collect(Collectors.toMap(E::getUid, Function.identity()));

        List<E> toInsert = new ArrayList<>();
        List<E> toUpdate = new ArrayList<>();

        for (E incoming : incomingByUid.values()) {
            E existing = existingByUid.get(incoming.getUid());
            if (existing == null) {
                if (incoming.getId() == null) incoming.setId(CodeGenerator.nextUlid());
                setAuditOnCreate(incoming);
                toInsert.add(incoming);
            } else {
                mergeIntoExisting(incoming, existing);
                setAuditOnUpdate(existing);
                toUpdate.add(existing);
            }
        }

        // 3) persist/flush in chunks
        List<E> result = new ArrayList<>(toInsert.size() + toUpdate.size());

        if (!toInsert.isEmpty()) {
            for (List<E> chunk : chunk(toInsert, bulkChunkSize)) {
                List<E> saved = repo.persistAllAndFlush(chunk);
                result.addAll(saved);
            }
        }

        if (!toUpdate.isEmpty()) {
            for (List<E> chunk : chunk(toUpdate, bulkChunkSize)) {
                List<E> saved = repo.updateAllAndFlush(chunk);
                result.addAll(saved);
            }
        }

        return result;
    }

    // ---------- helpers / extension points ----------

    /**
     * Implement to merge fields from incoming into existing instance. MUST not overwrite immutable fields like uid/ulid/createdAt.
     */
    protected abstract void mergeIntoExisting(E incoming, E existing);

    /**
     * Default audit set on created entities; subclasses may override.
     */
    protected void setAuditOnCreate(E e) {
        Instant now = Instant.now();
        if (e.getCreatedDate() == null) e.setCreatedDate(now);
        if (e.getLastModifiedDate() == null) e.setLastModifiedDate(now);
        // createdBy/lastModifiedBy: override to inject current user
    }

    /**
     * Default audit on update; subclasses may override to set user info.
     */
    protected void setAuditOnUpdate(E e) {
        e.setLastModifiedDate(Instant.now());
    }

    /**
     * Simple chunking helper.
     */
    @SuppressWarnings("SameParameterValue")
    protected List<List<E>> chunk(List<E> source, int chunkSize) {
        List<List<E>> chunks = new ArrayList<>();
        for (int i = 0; i < source.size(); i += chunkSize) {
            chunks.add(source.subList(i, Math.min(source.size(), i + chunkSize)));
        }
        return chunks;
    }
}
