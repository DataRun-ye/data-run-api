package org.nmcpye.datarun.mongo.common.repository;

import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.common.IdentifiableObjectRepository;
import org.nmcpye.datarun.mongo.common.MongoIdentifiableObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
@NoRepositoryBean
public interface MongoIdentifiableRepository<T extends MongoIdentifiableObject>
    extends MongoRepository<T, String>, IdentifiableObjectRepository<T, String> {

    @Query("{'uid': ?0}")
    Optional<T> findByUid(String uid);

    void deleteByUid(String uid);

    void deleteAllByUidIn(Collection<String> uids);

    Boolean existsByUid(@Size(max = 11) String uid);

    default Boolean existsByCode(String code) {
        return false;
    }

    default Optional<T> findFirstByCode(String code) {
        return Optional.empty();
    }

    default Optional<T> findFirstByName(String name) {
        return Optional.empty();
    }

    default List<T> findByNameLike(String name) {
        return Collections.emptyList();
    }

    @Query(value = "{ 'uid': { $in: ?0 }}")
    List<T> findAllByUidIn(Collection<String> uids);

    @Query(value = "{ 'uid': { $in: ?0 }}")
    Page<T> findAllByUidIn(Collection<String> uids, Pageable pageable);

    default List<T> findAllByCodeIn(Collection<String> codes) {
        return Collections.emptyList();
    }
}
