package org.nmcpye.datarun.jpa.datasubmissionbatching.job;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

/**
 * Simple range-based Mongo reader that pages by _id (greater-than lastSeenId).
 * It returns pages of documents of the provided type.
 *
 * @author Hamza Assada 16/08/2025 (7amza.it@gmail.com)
 */
@SuppressWarnings({"LombokGetterMayBeUsed", "UnnecessaryLocalVariable"})
public class MongoRangeItemReader<T> {

    private final MongoTemplate mongoTemplate;
    private final Class<T> targetType;
    private final String collection;
    private final int pageSize;
    private final String idField;

    private Object lastSeenId = null;
    private boolean finished = false;

    public MongoRangeItemReader(MongoTemplate mongoTemplate, Class<T> targetType, String collection, int pageSize) {
        this.mongoTemplate = mongoTemplate;
        this.targetType = targetType;
        this.collection = collection;
        this.pageSize = pageSize;
        this.idField = "serialNumber";
    }

    /**
     * Read the next page of items. Returns empty list when finished.
     */
    public List<T> readNextPage() {
        if (finished) return List.of();

        Query q = new Query();
        if (lastSeenId != null) {
            q.addCriteria(Criteria.where(idField).gt(lastSeenId));
        }
        q.limit(pageSize);
        q.with(Sort.by(Sort.Direction.ASC, idField));

        List<T> page = mongoTemplate.find(q, targetType, collection);

        if (page.isEmpty()) {
            finished = true;
            return List.of();
        }

        // update lastSeenId to last element's _id
        // assume entity has getId() or field named "id"; we use reflection to be generic
        Object last = extractId(page.get(page.size() - 1));
        this.lastSeenId = last;
        return page;
    }

    private Object extractId(T entity) {
        try {
            var clazz = entity.getClass();
            var f = clazz.getDeclaredField("serialNumber");
            f.setAccessible(true);
            return f.get(entity);
        } catch (NoSuchFieldException nsfe) {
            try {
                var m = entity.getClass().getMethod("setSerialNumber");
                return m.invoke(entity);
            } catch (Exception e) {
                throw new RuntimeException("Unable to extract id from Mongo entity. Add a public setSerialNumber() or field id", e);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Unable to extract id from Mongo entity", ex);
        }
    }

    public void reset() {
        this.lastSeenId = null;
        this.finished = false;
    }

    public boolean isFinished() {
        return finished;
    }
}
