package org.nmcpye.datarun.jpa.datatemplate.repository;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.nmcpye.datarun.jpa.datatemplate.CanonicalElement;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author Hamza Assada
 * @since 25/09/2025
 */
@Repository
public interface CanonicalElementRepository extends BaseJpaRepository<CanonicalElement, String> {
    Optional<CanonicalElement> findByCanonicalElementUid(String uid);

//    @Query(value = """
//        SELECT DISTINCT ce.*
//        FROM canonical_element ce
//        WHERE ce.canonical_element_uid IN :accessibleIds
//        """, nativeQuery = true)
//    Page<CanonicalElement> findWithAncestors(@Param("accessibleIds") List<Long> accessibleIds, Pageable pageable);

    List<CanonicalElement> findDistinctByCanonicalElementUidIn(Collection<String> uids);
}
